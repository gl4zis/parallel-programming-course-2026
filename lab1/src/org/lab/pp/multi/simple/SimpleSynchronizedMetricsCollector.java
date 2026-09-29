package org.lab.pp.multi.simple;

import org.lab.pp.MetricsCollector;
import org.lab.pp.Snapshot;
import org.lab.pp.single.SingleThreadMetricsCollector;

public class SimpleSynchronizedMetricsCollector implements MetricsCollector {

    private final MetricsCollector unsafeCollector = new SingleThreadMetricsCollector();

    @Override
    public synchronized void record(long value) {
        unsafeCollector.record(value);
    }

    @Override
    public synchronized Snapshot snapshot() {
        return unsafeCollector.snapshot();
    }
}
