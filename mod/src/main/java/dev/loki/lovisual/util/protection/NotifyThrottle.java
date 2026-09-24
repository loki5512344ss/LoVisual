package dev.loki.lovisual.util.protection;

/**
 * Minimal pure throttle for repeated warnings.
 *
 * <p>The caller supplies the current time, so the class has no clock
 * dependency and stays unit-testable.</p>
 */
public final class NotifyThrottle {

    private final long intervalMs;
    private boolean fired;
    private long lastMs;

    public NotifyThrottle(long intervalMs) {
        this.intervalMs = Math.max(0L, intervalMs);
    }

    /**
     * Returns true when the caller may emit a notification now, recording
     * the given time as the last emission. Returns false while the previous
     * emission is still inside the interval.
     */
    public boolean tryAcquire(long nowMs) {
        if (remainingMs(nowMs) > 0L) {
            return false;
        }
        fired = true;
        lastMs = nowMs;
        return true;
    }

    /**
     * Milliseconds left until the next notification is allowed.
     * Zero when the throttle has never fired or the window elapsed.
     */
    public long remainingMs(long nowMs) {
        if (!fired) {
            return 0L;
        }
        long elapsed = nowMs - lastMs;
        if (elapsed < 0L) {
            // Clock moved backwards: do not lock out the caller.
            return 0L;
        }
        long left = intervalMs - elapsed;
        return Math.max(0L, left);
    }

    public void reset() {
        fired = false;
        lastMs = 0L;
    }
}
