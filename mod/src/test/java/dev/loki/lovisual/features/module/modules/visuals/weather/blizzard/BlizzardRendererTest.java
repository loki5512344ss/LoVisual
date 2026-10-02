package dev.loki.lovisual.features.module.modules.visuals.weather.blizzard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Спецификация видимости снежинки: не лезет в глаза у камеры, плавно появляется и тает на границе зоны. */
class BlizzardRendererTest {

    private static final float R = 14f;

    private static float alpha(float dist, float age, float base, float opacity, float strength) {
        return BlizzardRenderer.flakeAlpha(dist, age, base, R, opacity, strength);
    }

    @Test
    void flakesRightInFrontOfTheLensAreInvisible() {
        assertEquals(0f, alpha(0.1f, 5f, 0.8f, 1f, 1f), 0f);
        assertEquals(0f, alpha(0.35f, 5f, 0.8f, 1f, 1f), 0f);
    }

    @Test
    void flakesFadeInWithDistanceFromTheCamera() {
        assertTrue(alpha(0.6f, 5f, 0.8f, 1f, 1f) > alpha(0.45f, 5f, 0.8f, 1f, 1f));
        assertTrue(alpha(2f, 5f, 0.8f, 1f, 1f) >= alpha(0.6f, 5f, 0.8f, 1f, 1f));
    }

    @Test
    void flakesMeltAwayAtTheEdgeOfTheStormZone() {
        assertTrue(alpha(R * 0.4f, 5f, 0.8f, 1f, 1f) > alpha(R * 0.9f, 5f, 0.8f, 1f, 1f));
        assertEquals(0f, alpha(R * 1.2f, 5f, 0.8f, 1f, 1f), 0f);
    }

    @Test
    void newlySpawnedFlakesAppearGraduallyInsteadOfPopping() {
        assertEquals(0f, alpha(5f, 0f, 0.8f, 1f, 1f), 0f);
        assertTrue(alpha(5f, 0.2f, 0.8f, 1f, 1f) < alpha(5f, 1f, 0.8f, 1f, 1f));
    }

    @Test
    void opacityAndStormStrengthScaleVisibilityLinearly() {
        float full = alpha(5f, 5f, 0.5f, 0.6f, 0.6f);
        assertEquals(0.5, alpha(5f, 5f, 0.5f, 0.3f, 0.6f) / full, 1e-5);
        assertEquals(0.5, alpha(5f, 5f, 0.5f, 0.6f, 0.3f) / full, 1e-5);
        assertEquals(0f, alpha(5f, 5f, 0.5f, 0f, 1f), 0f);
        assertEquals(0f, alpha(5f, 5f, 0.5f, 1f, 0f), 0f);
    }

    @Test
    void aFlakeIsNeverFullyOpaque() {
        assertTrue(alpha(5f, 5f, 1f, 1f, 1f) <= 0.95f);
    }

    @Test
    void faintFlakesAreSkippedInsteadOfDrawnInvisibly() {
        assertEquals(0f, alpha(5f, 5f, 0.35f, 0.01f, 0.2f), 0f);
    }

    @Test
    void distantFlakesStayAtLeastAPixelWide() {
        assertEquals(0.045f, BlizzardRenderer.halfWidth(1f, 1f, 5f), 1e-6f);
        assertTrue(BlizzardRenderer.halfWidth(1f, 1f, 200f) >= 200f * 0.0016f - 1e-6f);
        assertTrue(BlizzardRenderer.halfWidth(2f, 1f, 5f) > BlizzardRenderer.halfWidth(1f, 1f, 5f));
    }

    @Test
    void fasterFlakesLeaveLongerStreaksUpToALimit() {
        float slow = BlizzardRenderer.halfLength(0.05f, 2f, 1f);
        float fast = BlizzardRenderer.halfLength(0.05f, 10f, 1f);
        assertTrue(fast > slow);
        assertEquals(0.05f + 0.35f, BlizzardRenderer.halfLength(0.05f, 1000f, 1f), 1e-6f);
        assertEquals(0.05f, BlizzardRenderer.halfLength(0.05f, 0f, 1f), 1e-6f);
    }
}
