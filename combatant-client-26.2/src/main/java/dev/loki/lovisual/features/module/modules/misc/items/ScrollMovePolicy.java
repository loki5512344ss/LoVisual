/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.misc.items;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import dev.loki.lovisual.features.module.modules.misc.items.ItemScroller;

/**
 * Pure scroll-quick-move logic for ItemScroller: resolves which slot should
 * receive a QUICK_MOVE click when the user scrolls over the screen.
 */
public final class ScrollMovePolicy {

    private ScrollMovePolicy() {
    }

    public static boolean isScrollSignificant(double vertical) {
        return vertical > 0.05 || vertical < -0.05;
    }

    public static int direction(double vertical) {
        return vertical > 0 ? 1 : -1;
    }

    /**
     * QUICK_MOVE button (shift-click) as understood by
     * {@code MultiPlayerGameMode.handleContainerInput} for a left click.
     */
    public static int quickMoveButton() {
        return 0;
    }

    /**
     * Resolves the slot index that should receive the shift-click: the hovered
     * slot when it exists, otherwise the hotbar slot matching the scroll
     * direction (vanilla hotbar cycling) mapped into the menu.
     */
    public static int resolveTargetSlot(boolean hasHovered, int hoveredIndex,
                                        int hotbarSlot, int menuSlotCount) {
        if (hasHovered) return hoveredIndex;
        return hotbarIndexInMenu(hotbarSlot, menuSlotCount);
    }

    public static int hotbarIndexInMenu(int hotbarSlot, int menuSlotCount) {
        if (menuSlotCount <= 0) return -1;
        // In the standard inventory menu the hotbar occupies the last 9 slots.
        int hotbarStart = menuSlotCount - Inventory.getSelectionSize();
        if (hotbarStart < 0) return -1;
        return hotbarStart + Math.floorMod(hotbarSlot, Inventory.getSelectionSize());
    }

    /**
     * Double-check before sending a packet: an empty source stack must never
     * produce a click, and the slot must belong to the open menu.
     */
    public static boolean canClick(AbstractContainerMenu menu, int slotIndex, ItemStack hoveredStack) {
        if (menu == null || slotIndex < 0 || slotIndex >= menu.slots.size()) return false;
        if (hoveredStack == null || hoveredStack.isEmpty()) return false;
        return true;
    }
}
