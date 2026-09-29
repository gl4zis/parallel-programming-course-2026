package org.lab.pp.multi.buffering;

import org.lab.pp.MetricsCollector;
import org.lab.pp.Snapshot;
import org.lab.pp.multi.local.ThreadLocalMetricsCollector;
import org.lab.pp.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DoubleBufferingMetricsCollector implements MetricsCollector {

    private static final int NOWHERE = -1;

    private volatile int active = 0;
    private final List<ThreadBuffers> allBuffers = new ArrayList<>();
    private final Object listLock = new Object();

    private final ThreadLocal<ThreadBuffers> myBuffers = ThreadLocal.withInitial(() -> {
        ThreadBuffers b = new ThreadBuffers();
        synchronized (listLock) {
            allBuffers.add(b);
        }
        return b;
    });

    private final long[] globalBuckets = new long[256];
    private long globalCount = 0;
    private long globalSum = 0;
    private long globalMin = Long.MAX_VALUE;
    private long globalMax = Long.MIN_VALUE;

    @Override
    public void record(long value) {
        ThreadBuffers buffers = myBuffers.get();
        int bucket = (int) Math.min(value / 4, 255);

        int currActive;
        while (true) {
            currActive = active;
            buffers.inside.set(currActive);

            if (active == currActive) {
                break;
            }

            buffers.inside.setRelease(NOWHERE);
        }

        buffers.buckets[currActive][bucket]++;
        buffers.count[currActive]++;
        buffers.sum[currActive] += value;
        buffers.min[currActive] = Math.min(buffers.min[currActive], value);
        buffers.max[currActive] = Math.max(buffers.max[currActive], value);

        buffers.inside.setRelease(NOWHERE);
    }

    @Override
    public Snapshot snapshot() {
        synchronized (listLock) {
            int oldActive = active;
            active = 1 - oldActive;

            for (ThreadBuffers buffers : allBuffers) {
                while (buffers.inside.get() == oldActive) {
                    Thread.onSpinWait();
                }

                for (int i = 0; i < globalBuckets.length; i++) {
                    globalBuckets[i] += buffers.buckets[oldActive][i];
                    buffers.buckets[oldActive][i] = 0;
                }

                globalCount += buffers.count[oldActive];
                buffers.count[oldActive] = 0;

                globalSum += buffers.sum[oldActive];
                buffers.sum[oldActive] = 0;

                globalMin = Math.min(globalMin, buffers.min[oldActive]);
                buffers.min[oldActive] = Long.MAX_VALUE;

                globalMax = Math.max(globalMax, buffers.max[oldActive]);
                buffers.max[oldActive] = Long.MIN_VALUE;
            }

            var percentiles = Utils.calcStats(globalCount, globalBuckets);

            return new Snapshot(
                    Arrays.copyOf(globalBuckets, globalBuckets.length),
                    globalCount,
                    globalSum,
                    globalMin,
                    globalMax,
                    percentiles.p50(),
                    percentiles.p99()
            );
        }
    }

    private static final class ThreadBuffers {
        private final long[][] buckets = new long[2][256];
        private final long[] count = new long[2];
        private final long[] sum = new long[2];
        private final long[] min = {Long.MAX_VALUE, Long.MAX_VALUE};
        private final long[] max = {0, 0};

        final AtomicInteger inside = new AtomicInteger(NOWHERE);
    }
}
