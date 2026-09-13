package fr.rakambda.fallingtree.common.tree.builder;

/** One bounded limiter per builder, so distinct positions cannot flood the console. */
final class ScanLogLimiter {
    private final long intervalNanos;
    private boolean logged;
    private long lastLog;

    ScanLogLimiter(long intervalNanos) { this.intervalNanos = intervalNanos; }

    synchronized boolean shouldLog(long now) {
        if (logged && now - lastLog < intervalNanos) return false;
        logged = true;
        lastLog = now;
        return true;
    }
}
