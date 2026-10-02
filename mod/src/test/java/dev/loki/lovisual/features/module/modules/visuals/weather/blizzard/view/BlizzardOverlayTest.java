package dev.loki.lovisual.features.module.modules.visuals.weather.blizzard.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Спецификация экранных эффектов метели: лёгкая дымка и обмерзание, которое копится к краям. */
class BlizzardOverlayTest {

    @Test
    void hazeNeverVeilsTheWholeWorldAndGrowsWithTheStorm() {
        assertEquals(0f, BlizzardOverlay.hazeAlpha(0f), 0f);
        assertTrue(BlizzardOverlay.hazeAlpha(1f) > BlizzardOverlay.hazeAlpha(0.3f));
        assertTrue(BlizzardOverlay.hazeAlpha(1f) <= 0.15f, "the haze stays a light veil");
    }

    @Test
    void frostAtTheEdgeAddsUpToTheConfiguredShareOfTheStorm() {
        float current = 0.7f;
        float total = BlizzardOverlay.frostBandAlpha(current) * BlizzardOverlay.FROST_BANDS;
        assertEquals(0.55f * current, total, 1e-6f);
    }

    @Test
    void frostIsStrongestAtTheEdgeAndFadesTowardsTheCentre() {
        float depth = 100f;
        float edge = BlizzardOverlay.edgeCoverage(0f, depth, BlizzardOverlay.FROST_BANDS);
        float mid = BlizzardOverlay.edgeCoverage(depth * 0.2f, depth, BlizzardOverlay.FROST_BANDS);
        float inner = BlizzardOverlay.edgeCoverage(depth * 0.9f, depth, BlizzardOverlay.FROST_BANDS);
        float centre = BlizzardOverlay.edgeCoverage(depth * 1.5f, depth, BlizzardOverlay.FROST_BANDS);
        assertEquals(1f, edge, 0f);
        assertTrue(edge > mid && mid > inner && inner > centre);
        assertEquals(0f, centre, 0f);
    }

    @Test
    void bandsGetThinnerOneByOne() {
        float depth = 100f;
        float prev = Float.MAX_VALUE;
        for (int i = 0; i < BlizzardOverlay.FROST_BANDS; i++) {
            float t = BlizzardOverlay.bandThickness(i, BlizzardOverlay.FROST_BANDS, depth);
            assertTrue(t <= prev && t >= 0f);
            prev = t;
        }
        assertEquals(depth, BlizzardOverlay.bandThickness(0, BlizzardOverlay.FROST_BANDS, depth), 1e-6f);
    }

    @Test
    void frostTakesAFifthOfTheScreenAtMost() {
        assertEquals(0.2f, BlizzardOverlay.FROST_DEPTH_SHARE, 0f);
    }

    @Test
    void alphaIsClampedIntoAByte() {
        assertEquals(0x00FFFFFF & 0x123456, BlizzardOverlay.withAlpha(0xFF123456, -3f) & 0x00FFFFFF);
        assertEquals(0xFF, BlizzardOverlay.withAlpha(0x00123456, 7f) >>> 24);
        assertEquals(0x80, BlizzardOverlay.withAlpha(0x00123456, 0.5f) >>> 24);
    }
}
