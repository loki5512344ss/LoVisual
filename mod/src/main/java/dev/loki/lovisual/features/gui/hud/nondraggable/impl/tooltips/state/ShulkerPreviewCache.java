/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.nondraggable.impl.tooltips;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

final class ShulkerPreviewCache {

    static final int SHULKER_HINT_COLOR = 0xFFFFAA00;
    private static final float PADDING = 4.0f;
    private static final float SLOT_GAP = 2.0f;

    private static PreviewLayout cache;
    private static int cacheSignature;
    private static float cachePad;
    private static float cacheSlotSize;
    private static float cacheSlotGap;
    private static boolean cacheHasItems;

    private ShulkerPreviewCache() {
    }

    static PreviewLayout buildLayout() {
        BetterTooltips self = BetterTooltips.get();
        if (self == null || !self.isShulkerPreviewEnabled() || !self.isShulkerPreviewHeld()) return null;
        ItemStack toolStack = TooltipState.getStack();
        GuiGraphicsExtractor ctx = TooltipState.getCtx();
        if (toolStack == null || toolStack.isEmpty() || !isShulkerBox(toolStack) || ctx == null) return null;
        ItemContainerContents container = toolStack.get(DataComponents.CONTAINER);
        if (container == null) return null;
        int cols = 9;
        int rows = 3;
        NonNullList<ItemStack> stacks = NonNullList.withSize(cols * rows, ItemStack.EMPTY);
        container.copyInto(stacks);
        float pad = PADDING;
        float slotSize = self.shulkerPreviewSlotSize();
        float gap = SLOT_GAP;
        int sig = computeSignature(toolStack, stacks);
        if (cache != null && cacheSignature == sig && Float.compare(cachePad, pad) == 0
                && Float.compare(cacheSlotSize, slotSize) == 0 && Float.compare(cacheSlotGap, gap) == 0) {
            return cacheHasItems ? cache : null;
        }
        boolean hasItems = hasAnyStacks(stacks);
        cacheSignature = sig;
        cachePad = pad;
        cacheSlotSize = slotSize;
        cacheSlotGap = gap;
        cacheHasItems = hasItems;
        if (!hasItems) {
            cache = null;
            return null;
        }
        int bgW = Math.round(pad * 2f + cols * slotSize + (cols - 1) * gap);
        int bgH = Math.round(pad * 2f + rows * slotSize + (rows - 1) * gap);
        cache = new PreviewLayout(bgW, bgH, cols, rows, pad, slotSize, gap, copyStacks(stacks), sig);
        return cache;
    }

    static boolean isShulkerBox(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof BlockItem bi)) return false;
        return bi.getBlock() instanceof ShulkerBoxBlock;
    }

    static boolean hasShulkerContents(ItemStack stack) {
        if (!isShulkerBox(stack)) return false;
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container == null) return false;
        for (net.minecraft.world.item.ItemStackTemplate t : container.nonEmptyItems()) {
            ItemStack s = t.create();
            if (s != null && !s.isEmpty()) return true;
        }
        return false;
    }

    static boolean hasAnyStacks(NonNullList<ItemStack> stacks) {
        if (stacks == null) return false;
        for (ItemStack s : stacks) {
            if (s != null && !s.isEmpty()) return true;
        }
        return false;
    }

    static NonNullList<ItemStack> copyStacks(NonNullList<ItemStack> stacks) {
        NonNullList<ItemStack> copy = NonNullList.withSize(stacks.size(), ItemStack.EMPTY);
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack s = stacks.get(i);
            copy.set(i, s == null ? ItemStack.EMPTY : s.copy());
        }
        return copy;
    }

    static int computeSignature(ItemStack toolStack, NonNullList<ItemStack> stacks) {
        int hash = 1;
        hash = 31 * hash + (toolStack == null ? 0 : toolStack.getItem().hashCode());
        hash = 31 * hash + stacks.size();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack s = stacks.get(i);
            if (s == null || s.isEmpty()) {
                hash = 31 * hash;
                continue;
            }
            hash = 31 * hash + s.getItem().hashCode();
            hash = 31 * hash + s.getCount();
            hash = 31 * hash + s.getDamageValue();
            hash = 31 * hash + s.getComponents().hashCode();
        }
        return hash;
    }
}

record PreviewLayout(int width, int height, int cols, int rows, float pad,
                     float slotSize, float slotGap, NonNullList<ItemStack> stacks,
                     int containerSignature) {
}
