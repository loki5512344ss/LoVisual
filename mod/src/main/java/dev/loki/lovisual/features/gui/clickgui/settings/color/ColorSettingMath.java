/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings.color;

import java.awt.Color;

public final class ColorSettingMath {
    private ColorSettingMath() {
    }

    public static int hsbToArgb(float h, float s, float b, int a) {
        return (Math.max(0, Math.min(255, a)) << 24) | (Color.HSBtoRGB(h, s, b) & 0x00FFFFFF);
    }

    public static int withAlpha(int argb, int a) {
        return (argb & 0x00FFFFFF) | ((Math.max(0, Math.min(255, a)) & 0xFF) << 24);
    }
}
