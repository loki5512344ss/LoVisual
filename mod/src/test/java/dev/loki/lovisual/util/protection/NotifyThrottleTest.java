package dev.loki.lovisual.util.protection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class NotifyThrottleTest {

    @Test
    void firstCallIsAllowed() {
        NotifyThrottle throttle = new NotifyThrottle(1000L);
        assertTrue(throttle.tryAcquire(5_000L));
    }

    @Test
    void secondCallInsideIntervalIsSuppressed() {
        NotifyThrottle throttle = new NotifyThrottle(1_000L);
        assertTrue(throttle.tryAcquire(5_000L));
        assertFalse(throttle.tryAcquire(5_999L));
    }

    @Test
    void callExactlyAtIntervalBoundaryIsAllowed() {
        NotifyThrottle throttle = new NotifyThrottle(1_000L);
        assertTrue(throttle.tryAcquire(5_000L));
        assertTrue(throttle.tryAcquire(6_000L));
    }

    @Test
    void callAfterIntervalIsAllowed() {
        NotifyThrottle throttle = new NotifyThrottle(1_500L);
        assertTrue(throttle.tryAcquire(10_000L));
        assertFalse(throttle.tryAcquire(11_499L));
        assertTrue(throttle.tryAcquire(11_500L));
        assertTrue(throttle.tryAcquire(13_001L));
    }

    @Test
    void remainingMsIsMonotonicAndClampedToZero() {
        NotifyThrottle throttle = new NotifyThrottle(1_000L);
        assertEquals(0L, throttle.remainingMs(1_000L), "never fired");
        throttle.tryAcquire(2_000L);
        assertEquals(1_000L, throttle.remainingMs(2_000L));
        assertEquals(500L, throttle.remainingMs(2_500L));
        assertEquals(0L, throttle.remainingMs(3_000L));
        assertEquals(0L, throttle.remainingMs(10_000L));
    }

    @Test
    void backwardsClockDoesNotLockOutCaller() {
        NotifyThrottle throttle = new NotifyThrottle(1_000L);
        throttle.tryAcquire(5_000L);
        assertTrue(throttle.tryAcquire(1_000L), "moving time back should allow an emission");
    }

    @Test
    void nonPositiveIntervalAlwaysAllows() {
        NotifyThrottle zero = new NotifyThrottle(0L);
        assertTrue(zero.tryAcquire(100L));
        assertTrue(zero.tryAcquire(100L));
        assertTrue(zero.tryAcquire(100L));

        NotifyThrottle negative = new NotifyThrottle(-5L);
        assertTrue(negative.tryAcquire(1L));
        assertTrue(negative.tryAcquire(2L));
    }

    @Test
    void resetRestoresInitialState() {
        NotifyThrottle throttle = new NotifyThrottle(60_000L);
        assertTrue(throttle.tryAcquire(1_000L));
        assertFalse(throttle.tryAcquire(2_000L));
        throttle.reset();
        assertEquals(0L, throttle.remainingMs(2_000L));
        assertTrue(throttle.tryAcquire(2_000L));
    }
}
