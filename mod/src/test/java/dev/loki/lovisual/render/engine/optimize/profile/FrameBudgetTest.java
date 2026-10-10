package dev.loki.lovisual.render.engine.optimize.profile;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link FrameBudget} — the user-facing budget envelope.
 *
 * <p>Three assertions matter most:
 * <ol>
 *   <li>OFF mode = full quality (scale 1.0, no subsystem is limited).</li>
 *   <li>SOFT/AGGRESSIVE modes scale the baseline caps correctly (0.9 / 0.6).</li>
 *   <li>The user-supplied target FPS is clamped to a sane range and the per-subsystem
 *       caps track the scale.</li>
 * </ol>
 */
public final class FrameBudgetTest {

    @AfterEach
    void reset() {
        FrameBudget.reset();
    }

    @Test
    void offIsFullQualityByDefault() {
        FrameBudget.reset();
        assertEquals(FrameBudget.Mode.OFF, FrameBudget.mode());
        assertEquals(FrameBudget.DEFAULT_TARGET_FPS, FrameBudget.targetFps());
        assertEquals(1.0, FrameBudget.scale(), 1e-9);
        assertFalse(FrameBudget.limits(FrameBudget.SubSystem.ENTITIES));
        assertFalse(FrameBudget.limits(FrameBudget.SubSystem.PARTICLES));
        assertFalse(FrameBudget.limits(FrameBudget.SubSystem.HUD));
    }

    @Test
    void softScalesBy09() {
        FrameBudget.apply(FrameBudget.Mode.SOFT, 60);
        assertEquals(0.9, FrameBudget.scale(), 1e-9);
        assertTrue(FrameBudget.limits(FrameBudget.SubSystem.ENTITIES));
        assertTrue(FrameBudget.limits(FrameBudget.SubSystem.PARTICLES));
        // Distance caps shrink with the scale.
        double d = 35 * 0.9;
        assertEquals(d * d, FrameBudget.entityLodDistanceSq(), 0.5);
    }

    @Test
    void aggressiveScalesBy06() {
        FrameBudget.apply(FrameBudget.Mode.AGGRESSIVE, 120);
        assertEquals(0.6, FrameBudget.scale(), 1e-9);
        assertTrue(FrameBudget.limits(FrameBudget.SubSystem.EFFECTS));
        assertTrue(FrameBudget.limits(FrameBudget.SubSystem.TEXT));
        // Particle near/mid thresholds shrink with the scale.
        assertEquals((16 * 0.6) * (16 * 0.6), FrameBudget.particleNearSq(), 0.5);
        assertEquals((32 * 0.6) * (32 * 0.6), FrameBudget.particleMidSq(), 0.5);
    }

    @Test
    void targetFpsIsClamped() {
        FrameBudget.apply(FrameBudget.Mode.SOFT, 5);
        assertEquals(15, FrameBudget.targetFps());
        FrameBudget.apply(FrameBudget.Mode.SOFT, 1000);
        assertEquals(360, FrameBudget.targetFps());
        FrameBudget.apply(FrameBudget.Mode.SOFT, -10);
        assertEquals(15, FrameBudget.targetFps());
    }

    @Test
    void trailMaxPointsShrinkWithBudget() {
        FrameBudget.apply(FrameBudget.Mode.OFF, 60);
        assertEquals(256, FrameBudget.trailMaxPoints());
        FrameBudget.apply(FrameBudget.Mode.SOFT, 60);
        // 256 * 0.9 = 230.4 -> rounded
        assertTrue(FrameBudget.trailMaxPoints() >= 200 && FrameBudget.trailMaxPoints() <= 240,
                "SOFT trail max should be ~230, got " + FrameBudget.trailMaxPoints());
        FrameBudget.apply(FrameBudget.Mode.AGGRESSIVE, 60);
        // 256 * 0.6 = 153.6 -> rounded
        assertTrue(FrameBudget.trailMaxPoints() >= 140 && FrameBudget.trailMaxPoints() <= 170,
                "AGGRESSIVE trail max should be ~154, got " + FrameBudget.trailMaxPoints());
    }

    @Test
    void trailStepGrowsWhenBudgetTightens() {
        // Step is divided by the scale — AGGRESSIVE (0.6) gives a longer step (less frequent sampling).
        FrameBudget.apply(FrameBudget.Mode.OFF, 60);
        long baseStep = FrameBudget.trailStepNs();
        FrameBudget.apply(FrameBudget.Mode.AGGRESSIVE, 60);
        assertTrue(FrameBudget.trailStepNs() > baseStep,
                "Step should grow with AGGRESSIVE: base=" + baseStep + ", aggressive=" + FrameBudget.trailStepNs());
    }

    @Test
    void resetReturnsToOffAndDefaultFps() {
        FrameBudget.apply(FrameBudget.Mode.AGGRESSIVE, 240);
        FrameBudget.reset();
        assertEquals(FrameBudget.Mode.OFF, FrameBudget.mode());
        assertEquals(FrameBudget.DEFAULT_TARGET_FPS, FrameBudget.targetFps());
        assertEquals(1.0, FrameBudget.scale(), 1e-9);
    }

    @Test
    void modeActiveFlag() {
        assertFalse(FrameBudget.Mode.OFF.active());
        assertTrue(FrameBudget.Mode.SOFT.active());
        assertTrue(FrameBudget.Mode.AGGRESSIVE.active());
    }

    @Test
    void spawnerDistanceNeverGoesBelow4() {
        FrameBudget.apply(FrameBudget.Mode.AGGRESSIVE, 30);
        assertTrue(FrameBudget.spawnerDistance() >= 4,
                "Spawner distance should be clamped at 4, got " + FrameBudget.spawnerDistance());
    }
}
