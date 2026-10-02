package dev.loki.lovisual.features.module.modules.visuals.weather.blizzard.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Спецификация белой пелены: чем сильнее шторм, тем ближе туман; пелена только сжимает обзор и красит туман. */
class BlizzardFogTest {

    @Test
    void faintStormLeavesTheFogAlone() {
        assertFalse(BlizzardFog.active(0.0f));
        assertFalse(BlizzardFog.active(0.01f));
        assertTrue(BlizzardFog.active(0.2f));
    }

    @Test
    void strongerStormPullsTheFogCloser() {
        assertTrue(BlizzardFog.endDistance(28f, 1.0f) < BlizzardFog.endDistance(28f, 0.5f));
        assertEquals(28f, BlizzardFog.endDistance(28f, 1.0f), 1e-6f);
        assertTrue(BlizzardFog.startDistance(28f, 0.8f) < BlizzardFog.endDistance(28f, 0.8f));
    }

    @Test
    void fogNeverMovesFurtherThanTheVanillaOne() {
        assertEquals(40f, BlizzardFog.limit(100f, 40f), 0f);
        assertEquals(60f, BlizzardFog.limit(60f, 400f), 0f);
    }

    @Test
    void brightnessIsAlwaysAValidFactorAndBrighterSkyGivesBrighterSnowFog() {
        for (float c : new float[]{0f, 0.1f, 0.5f, 1f, 3f}) {
            float k = BlizzardFog.brightness(c, c, c);
            assertTrue(k >= 0f && k <= 1f);
        }
        assertTrue(BlizzardFog.brightness(0.6f, 0.3f, 0.3f) > BlizzardFog.brightness(0.1f, 0.1f, 0.1f));
        assertTrue(BlizzardFog.brightness(0f, 0f, 0f) > 0f, "the night fog still glows a little");
    }

    @Test
    void fogColourMovesFromVanillaTowardsTheSnowTint() {
        assertEquals(0.2f, BlizzardFog.blend(0.2f, 1f, 1f, 0f), 1e-6f);
        float half = BlizzardFog.blend(0.2f, 1f, 1f, 0.5f);
        float full = BlizzardFog.blend(0.2f, 1f, 1f, 1f);
        assertTrue(half > 0.2f && full > half && full <= 1f);
    }
}
