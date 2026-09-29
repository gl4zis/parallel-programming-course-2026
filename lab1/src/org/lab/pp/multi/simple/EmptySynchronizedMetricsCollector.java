package org.lab.pp.multi.simple;

import org.lab.pp.MetricsCollector;
import org.lab.pp.Snapshot;

public class EmptySynchronizedMetricsCollector implements MetricsCollector {

    @Override
    public synchronized void record(long value) { }

    @Override
    public synchronized Snapshot snapshot() {
        return null;
    }
}
