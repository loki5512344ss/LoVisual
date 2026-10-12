package dev.loki.lovisual.render.engine.optimize.hud;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link MonitorRate} pure logic — the refresh-rate clamp, the
 * once-per-second probe caching and the reset — through the test probe hook;
 * the GLFW path needs a live window.
 */
public final class MonitorRateTest {

    @AfterEach
    void reset() {
        MonitorRate.reset();
    }

    @Test
    void clampsLowRefreshRatesToThirty() {
        MonitorRate.setProbeForTesting(() -> 10);
        assertEquals(1_000_000_000L / 30, MonitorRate.intervalNs(0L));
    }

    @Test
    void usesProbedRate() {
        MonitorRate.setProbeForTesting(() -> 60);
        assertEquals(1_000_000_000L / 60, MonitorRate.intervalNs(0L));
    }

    @Test
    void probesAtMostOncePerSecond() {
        AtomicInteger probes = new AtomicInteger();
        MonitorRate.setProbeForTesting(() -> {
            probes.incrementAndGet();
            return 60;
        });

        MonitorRate.intervalNs(0L);
        MonitorRate.intervalNs(500_000_000L);       // still inside the 1 s window
        assertEquals(1, probes.get(), "second call inside the window must not re-probe");

        MonitorRate.intervalNs(1_000_000_000L);     // window elapsed
        assertEquals(2, probes.get(), "a new second must re-probe");
    }

    @Test
    void resetRestoresDefaultInterval() {
        MonitorRate.setProbeForTesting(() -> 30);
        MonitorRate.intervalNs(0L);
        MonitorRate.reset();
        assertTrue(MonitorRate.intervalNs(0L) > 0);

        // after reset the production probe is back — in headless tests it
        // falls back to the 144 Hz default instead of touching GLFW
        assertEquals(1_000_000_000L / 144, MonitorRate.intervalNs(0L));
    }
}
