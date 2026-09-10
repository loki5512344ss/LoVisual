/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.config.values.color;

public interface ColorValue {
    String get();

    void set(String value);

    int getArgb();

    boolean supportsAlpha();

    boolean isRainbow();

    String toRainbowValue();

    String rainbowFallbackHex();
}
