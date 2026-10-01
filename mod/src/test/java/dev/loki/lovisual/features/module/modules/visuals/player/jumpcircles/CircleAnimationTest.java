package dev.loki.lovisual.features.module.modules.visuals.player.jumpcircles;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircleAnimationTest {
    @Test
    void radiusStartsAtZeroAndNeverExceedsMax() {
        assertEquals(0f, CircleAnimation.radius(0f, 3f), 1e-6f);
        for (int i = 0; i <= 200; i++) {
            float r = CircleAnimation.radius(i / 200f, 3f);
            assertTrue(r >= 0f && r <= 3f, "radius " + r);
        }
    }

    @Test
    void easeOutCubicIsFastFirstAndClamped() {
        assertEquals(0f, CircleAnimation.easeOutCubic(0f), 1e-6f);
        assertEquals(1f, CircleAnimation.easeOutCubic(1f), 1e-6f);
        assertTrue(CircleAnimation.easeOutCubic(0.5f) > 0.5f);
        assertEquals(1f, CircleAnimation.easeOutCubic(7f), 1e-6f);
    }

    @Test
    void alphaStaysInUnitRangeAndFadesToZero() {
        for (float fade : new float[]{0.5f, 1f, 3f}) {
            for (int i = 0; i <= 200; i++) {
                float a = CircleAnimation.alpha(i / 200f, fade);
                assertTrue(a >= 0f && a <= 1f, "alpha " + a);
            }
            assertEquals(0f, CircleAnimation.alpha(0f, fade), 1e-6f);
            assertEquals(0f, CircleAnimation.alpha(1f, fade), 1e-6f);
        }
    }

    @Test
    void higherFadeSpeedKeepsCircleBrighterMidLife() {
        assertTrue(CircleAnimation.alpha(0.5f, 3f) > CircleAnimation.alpha(0.5f, 0.5f));
    }

    @Test
    void lifetimeShrinksWithSpeedAndProgressIsClamped() {
        assertTrue(CircleAnimation.lifetimeMs(2f) < CircleAnimation.lifetimeMs(1f));
        assertEquals(0f, CircleAnimation.progress(-5f, 1000f));
        assertEquals(1f, CircleAnimation.progress(5000f, 1000f));
    }

    @Test
    void angleGrowsWithTimeAndSwaysWithProgress() {
        assertTrue(CircleAnimation.angle(2f, 1f, 0f) > CircleAnimation.angle(1f, 1f, 0f));
        assertEquals(0f, CircleAnimation.angle(0f, 3f, 0f), 1e-6f);
        assertTrue(Math.abs(CircleAnimation.angle(0f, 0f, 0.125f)) > 0f);
    }
}
