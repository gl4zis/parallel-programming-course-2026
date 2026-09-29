package org.lab.pp.multi.striped;

import org.lab.pp.MetricsCollector;
import org.lab.pp.Snapshot;
import org.lab.pp.utils.Utils;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class StripedSynchronizedMetricsCollector implements MetricsCollector {

    private static final int BUCKET_COUNT = 256;

    private final BucketLock[] bucketLocks = new BucketLock[16];

    private final AtomicLong count = new AtomicLong(0);
    private final AtomicLong sum = new AtomicLong(0);
    private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);

    public StripedSynchronizedMetricsCollector() {
        for (int i = 0; i < bucketLocks.length; i++) {
            bucketLocks[i] = new BucketLock(BUCKET_COUNT / bucketLocks.length);
        }
    }

    @Override
    public void record(long value) {
        var bucket = (int) Math.min(value / 4, 255);
        var idx = calcIndexes(bucket);
        bucketLocks[idx.lock].inc(idx.bucket);

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
        var currBuckets = new long[BUCKET_COUNT];
        for (int lock = 0; lock < bucketLocks.length; lock++) {
            var copiedBuckets = bucketLocks[lock].copyAll();

            for (int lockBucket = 0; lockBucket < copiedBuckets.length; lockBucket++) {
                var bucket = lock + (lockBucket * bucketLocks.length);
                currBuckets[bucket] = copiedBuckets[lockBucket];
            }
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

    private BucketIndexes calcIndexes(int bucket) {
        var lock = bucket % bucketLocks.length;
        var lockBucket = bucket / bucketLocks.length;
        return new BucketIndexes(lock, lockBucket);
    }

    private record BucketLock(long[] buckets) {

        private BucketLock(int bucketsCount) {
            this(new long[bucketsCount]);
        }

        private synchronized void inc(int bucket) {
            buckets[bucket]++;
        }

        private synchronized long[] copyAll() {
            return Arrays.copyOf(buckets, buckets.length);
        }
    }

    private record BucketIndexes(
            int lock,
            int bucket
    ) {}
}
