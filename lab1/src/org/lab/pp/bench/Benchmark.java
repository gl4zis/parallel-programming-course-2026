package org.lab.pp.bench;

import org.lab.pp.MetricsCollector;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class Benchmark {

    public double measure(
            MetricsCollector collector,
            long[] values,
            int threadCount
    ) {
        run(collector, values, threadCount, 5);

        var results = new double[5];
        for (int i = 0; i < results.length; i++) {
            results[i] = run(collector, values, threadCount, 5);
            System.out.printf("Result %d: %f%n", i + 1, results[i]);
        }
        System.out.println(collector.snapshot());
        return mean(results);
    }

    private double run(
            MetricsCollector collector,
            long[] values,
            int threadCount,
            int seconds
    ) {
        var start = new CountDownLatch(1);
        var stop = new AtomicBoolean(false);
        var ops = new long[threadCount];
        var threads = new Thread[threadCount];

        for (int k = 0; k < threads.length; k++) {
            var thread = k;
            threads[thread] = new Thread(() -> {
                try {
                    var localCount = 0L;
                    var i = thread * 1000;
                    start.await();
                    while (!stop.get()) {
                        collector.record(values[i++]);
                        localCount++;
                        if (i == values.length) {
                            i = 0;
                        }
                    }
                    ops[thread] = localCount;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            });

            threads[thread].start();
        }

        try {
            var t0 = System.currentTimeMillis();
            start.countDown();
            Thread.sleep(Duration.ofSeconds(seconds));
            stop.set(true);
            var t1 = System.currentTimeMillis();

            for (Thread thread : threads) {
                thread.join();
            }

            return Arrays.stream(ops).sum() / ((t1 - t0) / 1000d);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private double mean(double[] values) {
        Arrays.sort(values);

        var mid = values.length / 2;
        if (values.length % 2 == 1) {
            return values[mid];
        } else {
            return (values[mid - 1] + values[mid]) / 2d;
        }
    }

}
