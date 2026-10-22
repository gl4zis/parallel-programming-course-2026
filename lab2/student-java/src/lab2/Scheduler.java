package lab2;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.function.BooleanSupplier;

/* Этап 1: планировщик, который ведёт настоящие потоки строго по одному (передача хода через семафоры).
 * Этап 2: обнаружение deadlock в главном цикле и корректное завершение застрявших потоков.
 * Внутреннее устройство (свои классы, семафоры, вспомогательные методы для Shared и SchedLock) придумываете сами. */
public final class Scheduler {
    /** Если true, каждая операция с общими данными добавляет строку в log. Выставляет runner. */
    boolean logging;
    /** Лог прогона: строки "<поток>  <операция>", например "A  lock mu", "A  balance -> 100". */
    final List<String> log = new ArrayList<>();
    /** null, если deadlock не было; иначе "held={a:A, b:B}" (этап 2). */
    String deadlock;

    static final class Worker {
        final Scheduler owner;
        final int id;
        final Runnable body;
        final Semaphore sem = new Semaphore(0);
        boolean done;
        boolean dirty;
        BooleanSupplier waitCond;

        Worker(Scheduler owner, int id, Runnable body) {
            this.owner = owner;
            this.id = id;
            this.body = body;
        }

        char letter() {
            return (char) ('A' + id);
        }

        void pause() {
            owner.turn.release();
            sem.acquireUninterruptibly();
            if (owner.aborted) {
                throw new Abort();
            }
            dirty = false;
        }
    }

    static final class Abort extends RuntimeException {
        Abort() {
            super(null, null, false, false);
        }
    }

    private static final ThreadLocal<Worker> CURRENT = new ThreadLocal<>();

    private final List<Worker> workers = new ArrayList<>();
    final List<SchedLock> locks = new ArrayList<>();
    private boolean aborted;
    private final StringBuilder trace = new StringBuilder();
    private final Semaphore turn = new Semaphore(0);

    public Scheduler() { }

    /** Зарегистрировать поток. Потоки получают буквы A, B, C… в порядке вызова spawn. */
    public void spawn(Runnable body) {
        if (workers.size() >= 26) {
            throw new UnsupportedOperationException("Max 26 threads supported");
        }
        workers.add(new Worker(this, workers.size(), body));
    }

    /** Запустить потоки и вести их по одному: на каждой паузе спросить strategy.pick(enabled, last). */
    public void run(Strategy strategy) {
        for (Worker w : workers) {
            Thread t = new Thread(() -> {
                CURRENT.set(w);
                try {
                    w.pause();
                    w.body.run();
                } catch (Abort ignored) {
                    // deadlock
                } finally {
                    w.done = true;
                    turn.release();
                }
            }, "sched-" + w.letter());
            t.setDaemon(true);
            t.start();
            turn.acquireUninterruptibly();
        }

        int last = -1;
        while (true) {
            List<Integer> enabled = new ArrayList<>();
            for (Worker w : workers) {
                if (!w.done && (w.waitCond == null || w.waitCond.getAsBoolean())) {
                    enabled.add(w.id);
                }
            }
            if (enabled.isEmpty()) {
                if (workers.stream().anyMatch(w -> !w.done)) {
                    deadlock = describeHeldLocks();
                    abortStuckThreads();
                }
                break;
            }

            int pick = strategy.pick(enabled, last);
            trace.append(workers.get(pick).letter());
            last = pick;
            workers.get(pick).sem.release();
            turn.acquireUninterruptibly();
        }
    }

    private String describeHeldLocks() {
        StringJoiner held = new StringJoiner(", ", "held={", "}");
        for (SchedLock l : locks) {
            if (l.owner != null) {
                held.add(l.name + ":" + l.owner.letter());
            }
        }
        return held.toString();
    }

    /** Будит застрявшие потоки по одному; каждый бросает Abort и заканчивается, так что в log они не пишут. */
    private void abortStuckThreads() {
        aborted = true;
        for (Worker w : workers) {
            if (!w.done) {
                w.sem.release();
                turn.acquireUninterruptibly();
            }
        }
    }

    /** Трасса прогона: буквы потоков, выбранных во всех решениях подряд, например "AAAABB". */
    public String traceString() {
        return trace.toString();
    }

    /** window() из тестовых программ: пауза перед следующей операцией с общими данными обязательна. */
    public static void point() {
        Worker w = CURRENT.get();
        w.dirty = true;
        log("window");
    }

    /** Ждать, пока cond не станет true. До этого поток не попадает в enabled. */
    public static void await(BooleanSupplier cond) {
        sync(cond);
        log("await done");
    }

    static void sync() {
        sync(null);
    }

    /** Пауза перед операцией с общим состоянием; ждёт, пока cond (если есть) не станет true. */
    static void sync(BooleanSupplier cond) {
        Worker w = CURRENT.get();
        if (w.dirty || (cond != null && !cond.getAsBoolean())) {
            w.waitCond = cond;
            w.pause();
            w.waitCond = null;
        }
        w.dirty = true;
    }

    static Worker current() {
        return CURRENT.get();
    }

    static void log(String op) {
        Worker w = CURRENT.get();
        if (w.owner.logging) {
            w.owner.log.add(w.letter() + "  " + op);
        }
    }
}
