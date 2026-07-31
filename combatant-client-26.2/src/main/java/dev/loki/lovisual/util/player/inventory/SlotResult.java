/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.player.inventory;

import net.minecraft.world.item.ItemStack;

public record SlotResult(int slot, boolean found, ItemStack stack) {
    public static SlotResult empty() {
        return new SlotResult(-1, false, ItemStack.EMPTY);
    }

    public boolean isHotbar() {
        return found && slot >= 0 && slot <= 8;
    }

    public boolean isInventory() {
        return found && slot > 8;
    }
}
