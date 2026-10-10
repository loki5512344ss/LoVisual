package dev.loki.lovisual.features.module.modules.misc.optimize.hud;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class HudWorthTest {

    @AfterEach
    void reset() {
        HudWorth.reset();
    }

    @Test
    void worthStartsTrue() {
        assertTrue(HudWorth.worth());
    }

    @Test
    void worthStaysTrueWhenFramesPerRefreshIsHigh() {
        // Frame time 5 ms, refresh 16.6 ms (60 Hz) -> ~3.3 frames per refresh -> well above ON_ABOVE (2.0).
        // Should stay "worth".
        long refreshNs = 16_600_000L;
        long now = 0;
        for (int i = 0; i < 50; i++) {
            now += 5_000_000L;
            assertTrue(HudWorth.update(now, refreshNs));
        }
        assertTrue(HudWorth.worth());
    }

    @Test
    void worthFlipsToFalseWhenFramesPerRefreshDropsBelowHysteresis() {
        // Frame time ~= refresh time -> ~1 frame per refresh -> below OFF_BELOW (1.5).
        long refreshNs = 16_600_000L;
        long now = 0;
        // Warm up the EMA at the high frame rate (worth stays true).
        for (int i = 0; i < 50; i++) {
            now += 5_000_000L;
            HudWorth.update(now, refreshNs);
        }
        assertTrue(HudWorth.worth());
        // Now drop the frame rate to one frame per refresh.
        for (int i = 0; i < 50; i++) {
            now += 16_600_000L;
            HudWorth.update(now, refreshNs);
        }
        assertFalse(HudWorth.worth());
    }

    @Test
    void hysteresisAvoidsFlickerNearTheThreshold() {
        // Sawtooth between 1.4 and 1.6 frames-per-refresh should NOT cause worth to flip back and forth.
        // (Default initial state is worth=true, so 1.4 will drop it; the test asserts it stays dropped
        // rather than flickering on every 1.6 frame.)
        long refreshNs = 16_600_000L;
        long now = 0;
        // Drop to ~1.4 frames per refresh first (16.6 / ~12ms = 1.38).
        for (int i = 0; i < 60; i++) {
            now += 12_000_000L;
            HudWorth.update(now, refreshNs);
        }
        assertFalse(HudWorth.worth());
        // A few short frames (~10ms => 1.66 fps/refresh) should NOT flip worth back on (below ON_ABOVE=2.0).
        for (int i = 0; i < 5; i++) {
            now += 10_000_000L;
            HudWorth.update(now, refreshNs);
        }
        assertFalse(HudWorth.worth());
        // A longer sustained high frame rate (5ms => 3.32 fps/refresh) flips it back on.
        for (int i = 0; i < 60; i++) {
            now += 5_000_000L;
            HudWorth.update(now, refreshNs);
        }
        assertTrue(HudWorth.worth());
    }

    @Test
    void updateIgnoresExtremeGaps() {
        // A 2-second gap (paused or main-thread stall) should NOT poison the EMA.
        long refreshNs = 16_600_000L;
        long now = 0;
        for (int i = 0; i < 30; i++) {
            now += 5_000_000L;
            HudWorth.update(now, refreshNs);
        }
        double beforeGap = HudWorth.frameNs();
        now += 2_000_000_000L; // 2 s gap
        HudWorth.update(now, refreshNs);
        // EMA should not have jumped to the gap value (the guard rejects dt >= 1e9 ns).
        // It may have moved slightly due to the legitimate 5 ms frames, but not to ~2 s.
        assertTrue(HudWorth.frameNs() < 100_000_000L,
                "EMA should not absorb a 2-second pause, frameNs=" + HudWorth.frameNs());
        assertTrue(HudWorth.frameNs() > 0, "EMA should be positive after warmup");
        // Sanity: the gap reading didn't blow up frameNs to near 2 seconds.
        assertTrue(HudWorth.frameNs() < beforeGap * 2 + 1_000_000L);
    }
}
