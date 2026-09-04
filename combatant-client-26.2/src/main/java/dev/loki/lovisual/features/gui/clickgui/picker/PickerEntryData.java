/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.picker;

import net.minecraft.world.item.ItemStack;

public record PickerEntryData(String id, String label, ItemStack stack) {
    public String getId() {
        return id;
    }
}
