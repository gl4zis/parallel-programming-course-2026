package org.lab.pp;

import org.lab.pp.bench.Benchmark;
import org.lab.pp.gen.ZipfLoadGenerator;
import org.lab.pp.multi.buffering.DoubleBufferingMetricsCollector;
import org.lab.pp.multi.local.ThreadLocalMetricsCollector;
import org.lab.pp.multi.striped.StripedLockMetricsCollector;

public class Main {

    // SingleThread = 466.1 Mops/sec

    // SimpleSync
    // 1 = 99.4 Mops/sec
    // 2 = 41.6 Mops/sec
    // 4 = 38.2 Mops/sec
    // 8 = 29.9 Mops/sec

    // Very strange
    // SimpleLock (unfair)
    // 1 = 79.4 Mops/sec
    // 2 = 13.7 Mops/sec
    // 4 = 48.9 Mops/sec
    // 8 = 47.2 Mops/sec

    // WOW
    // SimpleLock (fair)
    // 1 = 77.1 Mops/sec
    // 2 = 1.0 Mops/sec
    // 4 = 0.5 Mops/sec
    // 8 = 0.5 Mops/sec

    // EmptySync
    // 1 = 105.9 Mops/sec
    // 2 = 45.3 Mops/sec
    // 4 = 41.3 Mops/sec
    // 8 = 29.3 Mops/sec

    // StripedSync
    // 1 = 88.8 Mops/sec
    // 2 = 17.8 Mops/sec
    // 4 = 16.2 Mops/sec
    // 8 = 13.8 Mops/sec

    // StripedLock (unfair)
    // 1 = 69.2 Mops/sec
    // 2 = 18.2 Mops/sec
    // 4 = 25.2 Mops/sec
    // 8 = 27.3 Mops/sec

    // StripedLock (fair)
    // 1 = 58.3 Mops/sec
    // 2 = 2.4 Mops/sec
    // 4 = 1.6 Mops/sec
    // 8 = 1.2 Mops/sec

    // ThreadLocal
    // 1 = 140.9 Mops/sec
    // 2 = 277.6 Mops/sec
    // 4 = 547.3 Mops/sec
    // 8 = 853.5 Mops/sec

    // DoubleBuffering
    // 1 = 101.8 Mops/sec
    // 2 = 200.3 Mops/sec
    // 4 = 395.6 Mops/sec
    // 8 = 608.0 Mops/sec

    public static void main(String[] args) {
        var res = new Benchmark().measure(
                new StripedLockMetricsCollector(false),
                new ZipfLoadGenerator().generate(),
                2
        );

        System.out.printf("%.1f Mops/sec%n", res / 1_000_000);
    }
}