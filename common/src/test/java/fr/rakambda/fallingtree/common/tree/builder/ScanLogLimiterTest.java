package fr.rakambda.fallingtree.common.tree.builder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ScanLogLimiterTest {
    @Test void allowsFirstThenSuppressesBurstThenAllowsAfterInterval() {
        var limiter = new ScanLogLimiter(30);
        assertTrue(limiter.shouldLog(100));
        for (int i = 0; i < 1000; i++) assertFalse(limiter.shouldLog(101));
        assertFalse(limiter.shouldLog(129));
        assertTrue(limiter.shouldLog(130));
        assertFalse(limiter.shouldLog(130));
    }
    @Test void handlesNegativeNanoTimeAndWraparound() {
        var limiter = new ScanLogLimiter(30);
        assertTrue(limiter.shouldLog(Long.MAX_VALUE - 10));
        assertFalse(limiter.shouldLog(Long.MIN_VALUE + 10));
        assertTrue(limiter.shouldLog(Long.MIN_VALUE + 20));
    }
}
