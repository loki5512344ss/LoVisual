/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.config.values;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ColorValueTest {

    @Test
    void rgbNormalizesSupportedFormats() {
        assertEquals("#FF0000", new RGBColorValue("c", "#ff0000").get());
        assertEquals("#00FF00", new RGBColorValue("c", "0x00ff00").get());
        assertEquals("#FF0000", new RGBColorValue("c", "#CCFF0000").get());
        assertEquals("#FFFFFF", new RGBColorValue("c", "not-a-color").get());
        assertEquals("#FFFFFF", new RGBColorValue("c", null).get());
        assertEquals("#FFFFFF", new RGBColorValue("c", "").get());
    }

    @Test
    void rgbNormalizesRainbowValues() {
        assertEquals("RNB#123456", new RGBColorValue("c", "rainbow:#123456").get());
        assertEquals("RNB", new RGBColorValue("c", "RNB").get());
    }

    @Test
    void rgbGetArgbAddsFullAlpha() {
        assertEquals(0xFFFF0000, new RGBColorValue("c", "#FF0000").getArgb());
    }

    @Test
    void rgbRainbowKeepsFullAlpha() {
        int argb = new RGBColorValue("c", "RNB#0000FF").getArgb();
        assertEquals(0xFF000000, argb & 0xFF000000);
    }

    @Test
    void rgbRainbowHelpers() {
        RGBColorValue value = new RGBColorValue("c", "RNB#00FF00");

        assertTrue(value.isRainbow());
        assertFalse(value.supportsAlpha());
        assertEquals("#00FF00", value.rainbowFallbackHex());
    }

    @Test
    void rgbToRainbowKeepsBaseColor() {
        assertEquals("RNB#FF0000", RGBColorValue.toRainbow("#FF0000"));
    }

    @Test
    void rgbFromJsonNormalizes() {
        RGBColorValue value = new RGBColorValue("c", "#FFFFFF");

        value.fromJson("0x00ff00");
        assertEquals("#00FF00", value.get());

        value.fromJson(42);
        assertEquals("#00FF00", value.get());
    }

    @Test
    void rgbaNormalizesSupportedFormats() {
        assertEquals("#FF0000", new RGBAColorValue("c", "#ff0000").get());
        assertEquals("#80FF0000", new RGBAColorValue("c", "#80FF0000").get());
        assertEquals("RGBA#11223344", new RGBAColorValue("c", "rgba:11223344").get());
        // normalize только приводит к '#'+hex; невалидный hex остаётся как есть, getArgb даёт белый
        assertEquals("#NOPE", new RGBAColorValue("c", "nope").get());
        assertEquals(0xFFFFFFFF, new RGBAColorValue("c", "nope").getArgb());
        assertEquals("#FFFFFFFF", new RGBAColorValue("c", null).get());
    }

    @Test
    void rgbaNormalizesRainbowValues() {
        assertEquals("RNB#FFFF0000", new RGBAColorValue("c", "RNB#FF0000").get());
        assertEquals("RNB#80FF0000", new RGBAColorValue("c", "rainbow:80FF0000").get());
    }

    @Test
    void rgbaGetArgbParsesAarrggbb() {
        assertEquals(0x80FF0000, new RGBAColorValue("c", "#80FF0000").getArgb());
    }

    @Test
    void rgbaGetArgbInterpretsRgbaPrefixAsRrggbbAA() {
        assertEquals(0x44112233, new RGBAColorValue("c", "RGBA#11223344").getArgb());
    }

    @Test
    void rgbaGetArgbAddsFullAlphaToSixDigits() {
        assertEquals(0xFFFF0000, new RGBAColorValue("c", "#FF0000").getArgb());
    }

    @Test
    void rgbaRainbowKeepsConfiguredAlpha() {
        int argb = new RGBAColorValue("c", "RNB#80FF0000").getArgb();
        assertEquals(0x80000000, argb & 0xFF000000);
    }

    @Test
    void rgbaSupportsAlphaAndRainbowHelpers() {
        RGBAColorValue value = new RGBAColorValue("c", "RNB#80FF0000");

        assertTrue(value.isRainbow());
        assertTrue(value.supportsAlpha());
        assertEquals("#80FF0000", value.rainbowFallbackHex());
        assertEquals("RNB#80FF0000", RGBAColorValue.toRainbow("#80FF0000"));
    }

    @Test
    void rgbaFromJsonNormalizes() {
        RGBAColorValue value = new RGBAColorValue("c", "#FFFFFFFF");

        value.fromJson("RGBA:11223344");
        assertEquals("RGBA#11223344", value.get());

        value.fromJson(42);
        assertEquals("RGBA#11223344", value.get());
    }
}
