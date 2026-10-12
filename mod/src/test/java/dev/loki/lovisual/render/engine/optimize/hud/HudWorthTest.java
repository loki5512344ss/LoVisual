package dev.loki.lovisual.render.engine.optimize.hud;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link HudWorth} hysteresis — off below 1.5 frames per refresh,
 * on above 2.0, stable inside the band.
 */
public final class HudWorthTest {

    private static final long REFRESH_60HZ = 1_000_000_000L / 60; // ~16.67 ms

    @AfterEach
    void reset() {
        HudWorth.reset();
    }

    @Test
    void startsWorth() {
        assertTrue(HudWorth.worth());
    }

    @Test
    void fastFramesTurnItOff() {
        // ~1 ms frames at 60 Hz → framesPerRefresh ≈ 16.7, far above the band: stays on
        long t = 0;
        for (int i = 0; i < 100; i++) {
            t += 1_000_000L;
            assertTrue(HudWorth.update(t, REFRESH_60HZ));
        }

        // now ~16 ms frames → framesPerRefresh ≈ 1.04, below the off threshold
        for (int i = 0; i < 200; i++) {
            t += 16_000_000L;
            HudWorth.update(t, REFRESH_60HZ);
        }
        assertFalse(HudWorth.worth(), "near one frame per refresh the cache must turn off");
    }

    @Test
    void hysteresisBandKeepsState() {
        // drive it off with ~16 ms frames
        long t = 0;
        for (int i = 0; i < 200; i++) {
            t += 16_000_000L;
            HudWorth.update(t, REFRESH_60HZ);
        }
        assertFalse(HudWorth.worth());

        // ~10 ms frames → framesPerRefresh ≈ 1.67, inside the band: stays off
        for (int i = 0; i < 200; i++) {
            t += 10_000_000L;
            HudWorth.update(t, REFRESH_60HZ);
        }
        assertFalse(HudWorth.worth(), "inside the hysteresis band the state must not flip");

        // ~5 ms frames → framesPerRefresh ≈ 3.3, above the on threshold: back on
        for (int i = 0; i < 200; i++) {
            t += 5_000_000L;
            HudWorth.update(t, REFRESH_60HZ);
        }
        assertTrue(HudWorth.worth(), "well above the band the cache must turn back on");
    }

    @Test
    void resetRestoresWorth() {
        long t = 0;
        for (int i = 0; i < 200; i++) {
            t += 16_000_000L;
            HudWorth.update(t, REFRESH_60HZ);
        }
        assertFalse(HudWorth.worth());
        HudWorth.reset();
        assertTrue(HudWorth.worth());
    }
}
