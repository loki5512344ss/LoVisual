/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.aiming.features;

import dev.loki.lovisual.config.values.EnumValue;

public enum MovementCorrection implements EnumValue.IdProvider {
    OFF("Off"),
    STRICT("Strict"),
    SILENT("Silent");

    private final String id;

    MovementCorrection(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }
}
