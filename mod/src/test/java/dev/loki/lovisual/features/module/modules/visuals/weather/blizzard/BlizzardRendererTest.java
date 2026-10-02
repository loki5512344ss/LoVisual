package dev.loki.lovisual.features.module.modules.visuals.weather.blizzard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Спецификация вида снежинки (как в evo): не лезет в глаза у линзы, проявляется со временем, тянется вдоль скорости. */
class BlizzardRendererTest {

    private static float alpha(float dist, float age, float base, float opacity, float strength, float intensity) {
        return BlizzardRenderer.flakeAlpha(dist, age, base, opacity, strength, intensity);
    }

    @Test
    void flakesRightInFrontOfTheLensAreInvisible() {
        assertEquals(0f, alpha(0.1f, 5f, 0.8f, 1f, 1f, 1f), 0f);
        assertEquals(0f, alpha(0.25f, 5f, 0.8f, 1f, 1f, 1f), 0f);
    }

    @Test
    void flakesFadeInWithDistanceUntilAboutAMeter() {
        assertTrue(alpha(0.6f, 5f, 0.8f, 1f, 1f, 1f) > alpha(0.4f, 5f, 0.8f, 1f, 1f, 1f));
        assertEquals(alpha(1.5f, 5f, 0.8f, 1f, 1f, 1f), alpha(30f, 5f, 0.8f, 1f, 1f, 1f), 1e-6f);
    }

    @Test
    void newlySpawnedFlakesAppearGraduallyInsteadOfPopping() {
        assertEquals(0f, alpha(5f, 0f, 0.8f, 1f, 1f, 1f), 0f);
        assertTrue(alpha(5f, 0.2f, 0.8f, 1f, 1f, 1f) < alpha(5f, 1f, 0.8f, 1f, 1f, 1f));
        assertEquals(alpha(5f, 0.45f, 0.8f, 1f, 1f, 1f), alpha(5f, 9f, 0.8f, 1f, 1f, 1f), 1e-6f);
    }

    @Test
    void opacityAndShareOfStormScaleVisibilityLinearly() {
        float full = alpha(5f, 5f, 0.5f, 0.6f, 0.6f, 0.6f);
        assertEquals(0.5, alpha(5f, 5f, 0.5f, 0.3f, 0.6f, 0.6f) / full, 1e-5);
        assertEquals(0.5, alpha(5f, 5f, 0.5f, 0.6f, 0.3f, 0.6f) / full, 1e-5);
        assertEquals(0f, alpha(5f, 5f, 0.5f, 0f, 1f, 1f), 0f);
        assertEquals(0f, alpha(5f, 5f, 0.5f, 1f, 0f, 1f), 0f);
    }

    @Test
    void aFlakeNeverBecomesMoreOpaqueThanItsOwnAlphaTimesOpacity() {
        assertTrue(alpha(5f, 5f, 0.7f, 0.8f, 1f, 0.5f) <= 0.7f * 0.8f + 1e-6f);
    }

    @Test
    void flakesBehindTheCameraAreSkippedExceptCloseOnes() {
        assertTrue(BlizzardRenderer.isBehind(-0.9f, 10f));
        assertFalse(BlizzardRenderer.isBehind(-0.2f, 10f));
        assertFalse(BlizzardRenderer.isBehind(0.9f, 10f));
        assertFalse(BlizzardRenderer.isBehind(-0.9f, 2f));
    }

    @Test
    void distantFlakesStayAtLeastAPixelWideAndSizeSettingScalesNearOnes() {
        assertEquals(0.028f, BlizzardRenderer.radius(1f, 1f, 5f), 1e-6f);
        assertEquals(200f * 0.0024f, BlizzardRenderer.radius(1f, 1f, 200f), 1e-5f);
        assertTrue(BlizzardRenderer.radius(2f, 1f, 5f) > BlizzardRenderer.radius(1f, 1f, 5f));
        assertTrue(BlizzardRenderer.radius(1f, 1.4f, 5f) > BlizzardRenderer.radius(1f, 0.6f, 5f));
    }

    @Test
    void fasterFlakesLeaveLongerTrailsWhichAreLongerBehindThanInFront() {
        float r = 0.03f;
        float slow = BlizzardRenderer.streakLength(r, 2f, 1f);
        float fast = BlizzardRenderer.streakLength(r, 20f, 1f);
        assertTrue(fast > slow);
        assertEquals(r, BlizzardRenderer.streakLength(r, 0f, 1f), 1e-7f);
        float front = BlizzardRenderer.frontLength(r, fast);
        assertTrue(front < fast && front > r);
        assertEquals(r, BlizzardRenderer.frontLength(r, r), 1e-7f);
    }
}
