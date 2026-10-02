package dev.loki.lovisual.features.module.modules.visuals.weather.blizzard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Спецификация экранных эффектов метели: белая дымка и обмерзание краёв. */
class BlizzardOverlayTest {

    @Test
    void alphaIsClampedAndTheTintColourIsKept() {
        assertEquals(0xFF, BlizzardOverlay.withAlpha(0x00ABCDEF, 5.0f) >>> 24);
        assertEquals(0x00, BlizzardOverlay.withAlpha(0xFFABCDEF, -1.0f) >>> 24);
        assertEquals(0xABCDEF, BlizzardOverlay.withAlpha(0x12ABCDEF, 0.5f) & 0xFFFFFF);
    }

    @Test
    void noHazeOrNoStormMeansNoVeil() {
        assertEquals(0f, BlizzardOverlay.veilAlpha(0f, 1f), 0f);
        assertEquals(0f, BlizzardOverlay.veilAlpha(1f, 0f), 0f);
    }

    @Test
    void veilGrowsWithHazeAndStormAndIsCapped() {
        assertTrue(BlizzardOverlay.veilAlpha(0.8f, 0.5f) > BlizzardOverlay.veilAlpha(0.4f, 0.5f));
        assertTrue(BlizzardOverlay.veilAlpha(0.5f, 0.9f) > BlizzardOverlay.veilAlpha(0.5f, 0.3f));
        assertEquals(BlizzardOverlay.MAX_VEIL, BlizzardOverlay.veilAlpha(100f, 100f), 0f);
        assertTrue(BlizzardOverlay.veilAlpha(1f, 1f) < 1f, "the world must stay visible");
    }

    @Test
    void veilIsProportionalBelowTheCap() {
        assertEquals(2.0, BlizzardOverlay.veilAlpha(0.2f, 0.6f) / BlizzardOverlay.veilAlpha(0.1f, 0.6f), 1e-5);
    }

    @Test
    void frostIsStrongestAtTheEdgeAndFadesInward() {
        float depth = 100f;
        int bands = BlizzardOverlay.FROST_BANDS;
        assertEquals(1.0f, BlizzardOverlay.edgeCoverage(0f, depth, bands), 0f);
        float previous = 1.0f;
        for (float d = 0f; d <= depth; d += 5f) {
            float c = BlizzardOverlay.edgeCoverage(d, depth, bands);
            assertTrue(c <= previous + 1e-6f);
            assertTrue(c >= 0f && c <= 1f);
            previous = c;
        }
        assertEquals(0f, BlizzardOverlay.edgeCoverage(depth + 1f, depth, bands), 0f);
    }

    @Test
    void frostCoverageNeedsBandsAndDepth() {
        assertEquals(0f, BlizzardOverlay.edgeCoverage(1f, 100f, 0), 0f);
        assertEquals(0f, BlizzardOverlay.edgeCoverage(1f, 0f, 8), 0f);
    }

    @Test
    void frostBandsGetThinnerTowardTheEdgeSoAlphaAccumulatesThere() {
        int depth = 80;
        assertEquals(depth, BlizzardOverlay.bandThickness(0, BlizzardOverlay.FROST_BANDS, depth));
        int previous = Integer.MAX_VALUE;
        for (int i = 0; i < BlizzardOverlay.FROST_BANDS; i++) {
            int t = BlizzardOverlay.bandThickness(i, BlizzardOverlay.FROST_BANDS, depth);
            assertTrue(t <= previous);
            assertTrue(t >= 1);
            previous = t;
        }
    }

    @Test
    void frostBandsTogetherAddUpToTheConfiguredFrostAlpha() {
        float strength = 0.8f, frost = 0.5f;
        float total = BlizzardOverlay.frostTotal(strength, frost);
        assertEquals(BlizzardOverlay.FROST_TOTAL * strength * frost, total, 1e-6f);
        assertEquals(total, BlizzardOverlay.frostBandAlpha(total) * BlizzardOverlay.FROST_BANDS, 1e-6f);
        assertEquals(0f, BlizzardOverlay.frostTotal(0f, 1f), 0f);
        assertEquals(0f, BlizzardOverlay.frostTotal(1f, 0f), 0f);
    }
}
