package org.lab.pp;

import org.lab.pp.multi.buffering.DoubleBufferingMetricsCollector;
import org.lab.pp.multi.local.ThreadLocalMetricsCollector;
import org.lab.pp.multi.striped.StripedLockMetricsCollector;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;


// StripedLock (unfair)
// broken snapshots: 9842/10000 (98.42%) [sum<count: 9810, sum>count: 32]
// recorded calls (threads) = 1781434, final count() = 1781434 -> MATCH

// ThreadLocal
// broken snapshots: 9421/10000 (94.21%) [sum<count: 9421, sum>count: 0]
// recorded calls (threads) = 13434584, final count() = 13434584 -> MATCH

// DoubleBuffering
// broken snapshots: 0/10000 (0.00%) [sum<count: 0, sum>count: 0]
// recorded calls (threads) = 1759893, final count() = 1759893 -> MATCH

// broken DoubleBuffering
// broken snapshots: 8861/10000 (88.61%) [sum<count: 8861, sum>count: 0]
// recorded calls (threads) = 343371, final count() = 346006 -> MISMATCH

public class SnapshotConsistencyTest {

    private static final int WRITER_THREADS = 4;
    private static final int SNAPSHOTS = 10_000;

    public static void main(String[] args) throws InterruptedException {
        check(new DoubleBufferingMetricsCollector());
    }

    private static void check(MetricsCollector collector) throws InterruptedException {
        System.out.println("=== " + collector.getClass().getSimpleName() + " ===");

        AtomicBoolean stop = new AtomicBoolean(false);
        long[] recordedByThread = new long[WRITER_THREADS];
        Thread[] writers = new Thread[WRITER_THREADS];

        for (int i = 0; i < WRITER_THREADS; i++) {
            var thread = i;
            writers[i] = new Thread(() -> {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                long local = 0;
                while (!stop.get()) {
                    collector.record(rnd.nextLong(1024));
                    local++;
                }
                recordedByThread[thread] = local;
            });
            writers[i].start();
        }

        int broken = 0;
        int less = 0;
        int greater = 0;
        for (int i = 0; i < SNAPSHOTS; i++) {
            Snapshot snapshot = collector.snapshot();
            long bucketSum = 0;
            for (long b : snapshot.buckets()) {
                bucketSum += b;
            }
            if (bucketSum != snapshot.count()) {
                broken++;
                if (bucketSum < snapshot.count()) {
                    less++;
                }
                else {
                    greater++;
                }
            }
        }

        stop.set(true);
        for (Thread w : writers) {
            w.join();
        }

        long totalRecorded = Arrays.stream(recordedByThread).sum();
        long finalCount = collector.snapshot().count();

        System.out.printf("broken snapshots: %d/%d (%.2f%%) [sum<count: %d, sum>count: %d]%n",
                broken, SNAPSHOTS, 100.0 * broken / SNAPSHOTS, less, greater);
        System.out.printf("recorded calls (threads) = %d, final count() = %d -> %s%n",
                totalRecorded, finalCount, totalRecorded == finalCount ? "MATCH" : "MISMATCH");
    }
}
