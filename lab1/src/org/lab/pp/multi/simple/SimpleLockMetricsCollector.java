package org.lab.pp.multi.simple;

import org.lab.pp.MetricsCollector;
import org.lab.pp.Snapshot;
import org.lab.pp.single.SingleThreadMetricsCollector;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleLockMetricsCollector implements MetricsCollector {

    private final MetricsCollector unsafeCollector = new SingleThreadMetricsCollector();
    private final Lock lock;

    public SimpleLockMetricsCollector(boolean fair) {
        lock = new ReentrantLock(fair);
    }

    @Override
    public void record(long value) {
        lock.lock();
        unsafeCollector.record(value);
        lock.unlock();
    }

    @Override
    public Snapshot snapshot() {
        lock.lock();
        var sn = unsafeCollector.snapshot();
        lock.unlock();
        return sn;
    }
}
