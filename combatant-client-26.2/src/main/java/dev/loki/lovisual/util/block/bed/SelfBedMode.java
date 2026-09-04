/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.block.bed;

import dev.loki.lovisual.config.values.EnumValue;

public enum SelfBedMode implements EnumValue.IdProvider {
    NONE("none"),
    COLOR("color"),
    SPAWN_LOCATION("spawn_location"),
    MANUAL("manual");

    private final String id;

    SelfBedMode(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }
}