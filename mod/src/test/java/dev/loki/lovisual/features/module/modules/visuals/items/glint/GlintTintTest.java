package dev.loki.lovisual.features.module.modules.visuals.items.glint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class GlintTintTest {
    private static final int RED = 0xFF0000;
    private static final int BLUE = 0x0000FF;

    @Test
    void solidKeepsBrightnessPatternAndAlpha() {
        assertEquals(0xFF00FF00, GlintTint.pixel(0xFF8040FF, 0f, 0f, GlintTint.SOLID, 0x00FF00, 0, 1.0f));
        assertEquals(0x80000000, GlintTint.pixel(0x80000000, 0f, 0f, GlintTint.SOLID, 0xFFFFFF, 0, 1.0f));
        assertEquals(0xFF800000, GlintTint.pixel(0xFF808080, 0f, 0f, GlintTint.SOLID, RED, 0, 1.0f));
    }

    @Test
    void strengthBrightensButNeverOverflows() {
        assertEquals(0xFF800000, GlintTint.pixel(0xFF404040, 0f, 0f, GlintTint.SOLID, RED, 0, 2.0f));
        assertEquals(0xFFFF0000, GlintTint.pixel(0xFFC0C0C0, 0f, 0f, GlintTint.SOLID, RED, 0, 3.0f));
    }

    @Test
    void gradientBlendsAcrossTheTexture() {
        int left = GlintTint.pixel(0xFFFFFFFF, 0.0f, 0.0f, GlintTint.GRADIENT, RED, BLUE, 1.0f);
        int right = GlintTint.pixel(0xFFFFFFFF, 0.99f, 0.99f, GlintTint.GRADIENT, RED, BLUE, 1.0f);
        assertEquals(0xFFFF0000, left);
        assertEquals(true, (right & 0xFF) > 0xF0 && (right >> 16 & 0xFF) < 0x10);
    }

    @Test
    void rainbowChangesHueAlongTheTexture() {
        int a = GlintTint.pixel(0xFFFFFFFF, 0.0f, 0.0f, GlintTint.RAINBOW, 0, 0, 1.0f);
        int b = GlintTint.pixel(0xFFFFFFFF, 0.5f, 0.0f, GlintTint.RAINBOW, 0, 0, 1.0f);
        assertEquals(0xFFFF0000, a);
        assertEquals(true, a != b);
    }

    @Test
    void hiddenMakesTheTextureTransparent() {
        assertEquals(0x00FFFFFF, GlintTint.pixel(0xFFFFFFFF, 0f, 0f, GlintTint.HIDDEN, RED, BLUE, 1.0f));
    }
}
