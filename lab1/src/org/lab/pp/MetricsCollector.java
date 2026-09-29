package org.lab.pp;

public interface MetricsCollector {
    void record(long value);
    Snapshot snapshot();
}
