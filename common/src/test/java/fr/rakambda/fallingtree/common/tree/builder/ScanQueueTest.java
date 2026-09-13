package fr.rakambda.fallingtree.common.tree.builder;

import org.junit.jupiter.api.Test;
import java.util.PriorityQueue;
import static org.junit.jupiter.api.Assertions.*;

class ScanQueueTest {
    static final class Key implements Comparable<Key> {
        static int comparisons;
        final int id;
        Key(int id) { this.id = id; }
        public int compareTo(Key other) { return 0; }
        public int hashCode() { return id; }
        public boolean equals(Object other) {
            comparisons++;
            return other instanceof Key k && id == k.id;
        }
    }
    @Test void preservesUpstreamTraversalOrder() {
        var expected = new PriorityQueue<Key>();
        var actual = new ScanQueue<Key>();
        for (int i = 0; i < 500; i++) {
            var key = new Key(i);
            expected.add(key); actual.add(key);
        }
        while (!expected.isEmpty()) assertSame(expected.remove(), actual.remove());
        assertTrue(actual.isEmpty());
    }
    @Test void duplicateChecksAreLinearAcrossLargeFrontier() {
        var queue = new ScanQueue<Key>();
        for (int i = 0; i < 10000; i++) queue.add(new Key(i));
        Key.comparisons = 0;
        for (int i = 0; i < 10000; i++) assertTrue(queue.contains(new Key(i)));
        assertTrue(Key.comparisons <= 20000, "Membership must not search the whole frontier");
    }
    @Test void removesMembershipAndRejectsDuplicatePositions() {
        var queue = new ScanQueue<Key>();
        var key = new Key(1);
        queue.add(key); queue.add(new Key(1));
        assertSame(key, queue.remove());
        assertFalse(queue.contains(key));
        assertTrue(queue.isEmpty());
        queue.add(key);
        assertSame(key, queue.remove());
    }
}
