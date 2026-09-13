package fr.rakambda.fallingtree.common.tree.builder;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

/** Keeps upstream traversal order while making pending-position lookup constant-time on average. */
final class ScanQueue<E extends Comparable<E>> {
    private final PriorityQueue<E> queue = new PriorityQueue<>();
    private final Set<E> pending = new HashSet<>();

    boolean contains(E value) { return pending.contains(value); }
    boolean isEmpty() { return queue.isEmpty(); }
    void add(E value) {
        if (pending.add(value)) queue.add(value);
    }
    E remove() {
        E value = queue.remove();
        pending.remove(value);
        return value;
    }
}
