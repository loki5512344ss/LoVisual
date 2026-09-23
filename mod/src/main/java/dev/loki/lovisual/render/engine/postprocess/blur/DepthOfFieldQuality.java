/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.postprocess.blur;

import dev.loki.lovisual.config.values.mode.EnumValue;

public enum DepthOfFieldQuality implements EnumValue.IdProvider {
    LOW("low", 8),
    MEDIUM("medium", 14),
    HIGH("high", 22);

    private final String id;
    private final int taps;

    DepthOfFieldQuality(String id, int taps) {
        this.id = id;
        this.taps = taps;
    }

    @Override
    public String id() {
        return id;
    }

    public int taps() {
        return taps;
    }
}
