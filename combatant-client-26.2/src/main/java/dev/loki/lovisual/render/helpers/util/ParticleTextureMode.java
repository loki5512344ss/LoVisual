/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.helpers.util;

import dev.loki.lovisual.config.values.mode.EnumValue;

public enum ParticleTextureMode implements EnumValue.IdProvider {
    BLOOM("bloom"),
    RANDOM("random");

    private final String id;

    ParticleTextureMode(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }
}
