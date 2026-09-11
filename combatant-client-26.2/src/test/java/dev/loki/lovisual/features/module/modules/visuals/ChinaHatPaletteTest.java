/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import dev.loki.lovisual.features.module.modules.visuals.hats.ChinaHatPalette;

public final class ChinaHatPaletteTest {
    @Test
    void channelsAreIsolatedAndPreserved() {
        int argb = 0xA1B2C3D4;

        assertEquals(0xA1, ChinaHatPalette.alpha(argb));
        assertEquals(0xB2, ChinaHatPalette.red(argb));
        assertEquals(0xC3, ChinaHatPalette.green(argb));
        assertEquals(0xD4, ChinaHatPalette.blue(argb));
    }

    @Test
    void multiplyRgbKeepsAlphaAndClamps() {
        int argb = 0xFF804020;

        assertEquals(0xFFC06030, ChinaHatPalette.multiplyRgb(argb, 1.5f));
        assertEquals(0xFF000000, ChinaHatPalette.multiplyRgb(argb, 0.0f));
        assertEquals(0xFFFFFF80, ChinaHatPalette.multiplyRgb(argb, 4.0f));
    }

    @Test
    void brightenUsesSameScalingAsMultiply() {
        int argb = 0x80FF8000;

        assertEquals(
                ChinaHatPalette.multiplyRgb(argb, 1.25f),
                ChinaHatPalette.brighten(argb, 1.25f)
        );
    }

    @Test
    void withAlphaReplacesOnlyTheAlphaChannel() {
        int argb = 0xFF123456;

        assertEquals(0x50123456, ChinaHatPalette.withAlpha(argb, 80));
        assertEquals(0xFF123456, ChinaHatPalette.withAlpha(argb, 999));
        assertEquals(0x00123456, ChinaHatPalette.withAlpha(argb, -5));
    }

    @Test
    void alphaOrFallsBackOnlyWhenChannelIsZero() {
        assertEquals(238, ChinaHatPalette.alphaOr(0x00123456, 238));
        assertEquals(255, ChinaHatPalette.alphaOr(0xFF123456, 238));
    }

    @Test
    void mixClampsAndBlendsEveryChannel() {
        int a = 0xFF000000;
        int b = 0x00FFFFFF;

        assertEquals(a, ChinaHatPalette.mix(a, b, -1.0f));
        assertEquals(b, ChinaHatPalette.mix(a, b, 2.0f));
        assertEquals(0x80808080, ChinaHatPalette.mix(a, b, 0.5f));
    }
}
