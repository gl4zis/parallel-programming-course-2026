package lab2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/* Стратегии выбора следующего потока.
 * FirstAlive — этап 1, Replay — этап 2, RandomPick — этап 3, Dfs — этап 4 (ограничение bound — этап 5).
 * enabled — номера потоков, которые могут сделать шаг, по возрастанию; last — прошлый выбранный (-1 в начале). */
public final class Strategies {
    private Strategies() { }

    /** Всегда первый поток из enabled. */
    public static final class FirstAlive implements Strategy {
        public int pick(List<Integer> enabled, int last) {
            return enabled.getFirst();
        }
    }

    /** Проигрывает трассу: на i-м решении выбирает поток trace[i] ('A' -> 0, 'B' -> 1). */
    public static final class Replay implements Strategy {
        private final String trace;
        private int pos;

        public Replay(String trace) {
            this.trace = trace;
        }

        /** Сбросить позицию в трассе перед прогоном. */
        public void beginRun() {
            pos = 0;
        }

        public int pick(List<Integer> enabled, int last) {
            if (pos >= trace.length()) {
                throw new IllegalStateException("replay trace ended");
            }
            int thread = trace.charAt(pos) - 'A';
            if (!enabled.contains(thread)) {
                throw new IllegalStateException("replay trace thread is not enabled");
            }
            pos++;
            return thread;
        }
    }

    /** Случайный поток: enabled.get(rnd.nextInt(enabled.size())), rnd = new java.util.Random(seed). */
    public static final class RandomPick implements Strategy {
        private final Random random;

        public RandomPick(long seed) {
            this.random = new Random(seed);
        }

        public int pick(List<Integer> enabled, int last) {
            return enabled.get(random.nextInt(enabled.size()));
        }
    }

    /** Stateless DFS по дереву выборов: каждый прогон с нуля, повторяет префикс прошлого и берёт следующий вариант.
     *  bound — максимум вытеснений (этап 5); для dfs runner передаёт Integer.MAX_VALUE. */
    public static final class Dfs implements Strategy {
        /** Развилка: enabled для проверки детерминизма, options — варианты после фильтра по bound (фиксируются при первом заходе). */
        private static final class Node {
            final List<Integer> enabled;
            final List<Integer> options;
            int chosen;

            Node(List<Integer> enabled, List<Integer> options) {
                this.enabled = enabled;
                this.options = options;
            }
        }

        private final List<Node> path = new ArrayList<>();
        private final int bound;
        private int pos;
        private int preemptions;

        public Dfs(int bound) {
            this.bound = bound;
        }

        /** Подготовиться к новому прогону (позиция в пути, счётчик вытеснений). */
        public void beginRun() {
            pos = 0;
            preemptions = 0;
        }

        private static boolean isPreemption(List<Integer> enabled, int last, int choice) {
            return last != -1 && choice != last && enabled.contains(last);
        }

        public int pick(List<Integer> enabled, int last) {
            Node node;
            if (pos < path.size()) {
                node = path.get(pos);
                if (!node.enabled.equals(enabled)) {
                    throw new IllegalStateException("dfs run is not deterministic");
                }
            } else {
                List<Integer> allowed = new ArrayList<>();
                for (int t : enabled) {
                    if (preemptions < bound || !isPreemption(enabled, last, t)) {
                        allowed.add(t);
                    }
                }
                node = new Node(List.copyOf(enabled), allowed);
                path.add(node);
            }
            pos++;
            int choice = node.options.get(node.chosen);
            if (isPreemption(enabled, last, choice)) {
                preemptions++;
            }
            return choice;
        }

        /** Выбрать следующий путь; false — дерево обойдено. */
        public boolean endRun() {
            path.subList(pos, path.size()).clear();
            while (!path.isEmpty()) {
                Node top = path.getLast();
                if (top.chosen + 1 < top.options.size()) {
                    top.chosen++;
                    return true;
                }
                path.removeLast();
            }
            return false;
        }
    }
}
