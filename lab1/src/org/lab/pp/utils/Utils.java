package org.lab.pp.utils;

public class Utils {

    private Utils() { }

    public static Stats calcStats(long count, long[] buckets) {
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

        return new Stats(p50, p99);
    }
}
