package org.lab.pp.multi.striped;

import org.lab.pp.MetricsCollector;
import org.lab.pp.Snapshot;
import org.lab.pp.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StripedLockMetricsCollector implements MetricsCollector {

    private final Lock[] bucketLocks = new Lock[16];

    private final long[] buckets = new long[256];
    private final AtomicLong count = new AtomicLong(0);
    private final AtomicLong sum = new AtomicLong(0);
    private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);

    public StripedLockMetricsCollector(boolean fair) {
        for (int i = 0; i < bucketLocks.length; i++) {
            bucketLocks[i] = new ReentrantLock(fair);
        }
    }

    @Override
    public void record(long value) {
        var bucket = (int) Math.min(value / 4, 255);
        int lock = lockByBucket(bucket);

        bucketLocks[lock].lock();
        buckets[bucket]++;
        bucketLocks[lock].unlock();

        count.incrementAndGet();
        sum.addAndGet(value);

        var currMin = min.get();
        while (value < currMin) {
            min.compareAndSet(currMin, value);
            currMin = min.get();
        }

        var currMax = max.get();
        while (value > currMax) {
            max.compareAndSet(currMax, value);
            currMax = max.get();
        }
    }

    @Override
    public Snapshot snapshot() {
        var currBuckets = new long[buckets.length];
        for (int lock = 0; lock < bucketLocks.length; lock++) {
            var bucketsToCopy = bucketsByLock(lock);

            bucketLocks[lock].lock();
            for (int bucket : bucketsToCopy) {
                currBuckets[bucket] = buckets[bucket];
            }
            bucketLocks[lock].unlock();
        }

        var currCount = count.get();
        var currSum = sum.get();
        var currMin = min.get();
        var currMax = max.get();
        var percentiles = Utils.calcStats(currCount, currBuckets);

        return new Snapshot(
                currBuckets,
                currCount,
                currSum,
                currMin,
                currMax,
                percentiles.p50(),
                percentiles.p99()
        );
    }

    private int lockByBucket(int bucket) {
        return bucket % bucketLocks.length;
    }

    private List<Integer> bucketsByLock(int lock) {
        var res = new ArrayList<Integer>(buckets.length / bucketLocks.length);
        for (int i = 0; i < buckets.length; i++) {
            if (i % bucketLocks.length == lock) {
                res.add(i);
            }
        }

        return res;
    }
}
