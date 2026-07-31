/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.player.inventory;

import dev.loki.lovisual.config.values.EnumValue;

public enum InventorySwapPolicy implements EnumValue.IdProvider {
    NONE("none"),
    GRIM_STRICT("grim_strict"),
    LEGIT("legit");

    private final String id;

    InventorySwapPolicy(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }
}
