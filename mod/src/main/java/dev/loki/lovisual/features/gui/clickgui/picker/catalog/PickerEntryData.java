package dev.loki.lovisual.features.gui.clickgui.picker.catalog;

import net.minecraft.world.item.ItemStack;

public record PickerEntryData(String id, String label, ItemStack stack) {
    public String getId() {
        return id;
    }
}
