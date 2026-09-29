package org.lab.pp.single;

import org.lab.pp.MetricsCollector;
import org.lab.pp.Snapshot;

import java.util.Arrays;

public class SingleThreadMetricsCollector implements MetricsCollector {

    private final long[] buckets = new long[256];
    private long count = 0;
    private long sum = 0;
    private long min = Long.MAX_VALUE;
    private long max = Long.MIN_VALUE;

    @Override
    public void record(long value) {
        var bucket = (int) Math.min(value / 4, 255);
        buckets[bucket]++;

        count++;
        sum += value;
        if (value < min) {
            min = value;
        }
        if (value > max) {
            max = value;
        }
    }

    @Override
    public Snapshot snapshot() {
        var p50 = -1L;
        var p99 = -1L;

        var p50Barrier = count * 0.5;
        var p99Barrier = count * 0.99;
        var collected = 0L;

        for (int i = 0; i < buckets.length; i++) {
            collected += buckets[i];
            if (collected >= p50Barrier && p50 < 0) {
                p50 = i * 4L;
            }
            if (collected >= p99Barrier) {
                p99 = i * 4L;
                break;
            }
        }

        return new Snapshot(
                Arrays.copyOf(buckets, buckets.length),
                count,
                sum,
                min,
                max,
                p50,
                p99
        );
    }
}
