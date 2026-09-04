/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.postprocess;

import dev.loki.lovisual.config.values.EnumValue;

public enum MotionBlurMode implements EnumValue.IdProvider {
    WORLD_AND_HAND("world_and_hand"),
    WORLD_ONLY("world_only");

    private final String id;

    MotionBlurMode(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }
}
