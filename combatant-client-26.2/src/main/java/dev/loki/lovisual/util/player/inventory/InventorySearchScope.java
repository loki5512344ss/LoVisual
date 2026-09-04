/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.player.inventory;

import dev.loki.lovisual.config.values.EnumValue;

public enum InventorySearchScope implements EnumValue.IdProvider {
    HOTBAR("hotbar"),
    INVENTORY("inventory"),
    FULL("full");

    private final String id;

    InventorySearchScope(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }
}
