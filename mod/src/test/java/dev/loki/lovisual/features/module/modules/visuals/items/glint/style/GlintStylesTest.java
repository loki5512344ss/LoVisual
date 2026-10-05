package dev.loki.lovisual.features.module.modules.visuals.items.glint.style;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class GlintStylesTest {
    private static final String[] STYLES = {GlintStyles.PULSE, GlintStyles.FIRE, GlintStyles.GALAXY,
            GlintStyles.HOLOGRAM, GlintStyles.GLITCH, GlintStyles.SPARKLE, GlintStyles.AURORA};

    @Test
    void hashIsDeterministicAndInRange() {
        for (int i = 0; i < 200; i++) {
            float a = GlintNoise.hash(i, i * 3, 16, 5);
            assertEquals(a, GlintNoise.hash(i, i * 3, 16, 5));
            assertTrue(a >= 0.0f && a < 1.0f);
        }
    }

    @Test
    void noiseWrapsSeamlessly() {
        assertEquals(GlintNoise.fbm(0.0f, 0.3f, 4, 3, 7), GlintNoise.fbm(1.0f, 0.3f, 4, 3, 7), 1e-4f);
        assertEquals(GlintNoise.sparkles(0.0f, 0.4f, 12, 0.22f, 0.35f, 3), GlintNoise.sparkles(1.0f, 0.4f, 12, 0.22f, 0.35f, 3), 1e-4f);
    }

    @Test
    void fbmStaysNormalised() {
        for (int i = 0; i < 100; i++) {
            float n = GlintNoise.fbm(i * 0.0137f, i * 0.0291f, 5, 3, 7);
            assertTrue(n >= 0.0f && n <= 1.0f);
        }
    }

    @Test
    void everyStyleProducesValidColoursAndSomeLight() {
        for (String style : STYLES) {
            assertTrue(GlintStyles.handles(style));
            boolean anyLit = false;
            for (int i = 0; i < 400; i++) {
                float u = (i % 20) / 20.0f;
                float v = (i / 20) / 20.0f;
                int rgb = GlintStyles.shade(style, u, v, 0.8f, 0xFF40A0, 0x40A0FF, 0.35f);
                assertEquals(0, rgb & 0xFF000000);
                anyLit |= rgb != 0;
            }
            assertTrue(anyLit, style);
        }
    }

    @Test
    void coverageLiftsTheGlowBetweenStreaks() {
        int bare = GlintStyles.shade(GlintStyles.AURORA, 0.1f, 0.2f, 0.0f, 0xFFFFFF, 0xFFFFFF, 0.0f);
        int covered = GlintStyles.shade(GlintStyles.AURORA, 0.1f, 0.2f, 0.0f, 0xFFFFFF, 0xFFFFFF, 0.6f);
        assertTrue((covered & 0xFF) > (bare & 0xFF));
    }

    @Test
    void vanillaStylesAreNotHandled() {
        assertFalse(GlintStyles.handles("Rainbow"));
        assertFalse(GlintStyles.handles("Hidden"));
        assertTrue(GlintStyles.usesSecondary(GlintStyles.FIRE));
        assertFalse(GlintStyles.usesSecondary(GlintStyles.PULSE));
    }

    @Test
    void motionOnlyForCustomStyles() {
        assertNull(GlintMotion.matrix("Rainbow", 1.0f, 8.0f));
        for (String style : STYLES) assertNotNull(GlintMotion.matrix(style, 1.0f, 8.0f));
    }

    @Test
    void pulseBreathesAndFireRises() {
        Matrix4f early = GlintMotion.matrix(GlintStyles.PULSE, 0.0f, 8.0f);
        Matrix4f later = GlintMotion.matrix(GlintStyles.PULSE, 1.0f, 8.0f);
        assertNotEquals(early.m00(), later.m00());
        assertNotEquals(GlintMotion.matrix(GlintStyles.FIRE, 0.5f, 8.0f).m31(), GlintMotion.matrix(GlintStyles.FIRE, 1.0f, 8.0f).m31());
    }

    @Test
    void glitchJumpsOnlyBetweenSteps() {
        Matrix4f a = GlintMotion.matrix(GlintStyles.GLITCH, 0.01f, 8.0f);
        Matrix4f b = GlintMotion.matrix(GlintStyles.GLITCH, 0.10f, 8.0f);
        Matrix4f c = GlintMotion.matrix(GlintStyles.GLITCH, 0.30f, 8.0f);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }
}
