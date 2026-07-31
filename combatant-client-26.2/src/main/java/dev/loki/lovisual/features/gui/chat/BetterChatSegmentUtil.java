/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

import dev.loki.lovisual.util.item.IllegalItemUtil;
import dev.loki.lovisual.util.item.TopEnchantUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dev.loki.lovisual.features.theme.Theme.theme;

/**
 * Segment flattening and per-segment color resolution for the chat line layout.
 */
final class BetterChatSegmentUtil {
    private BetterChatSegmentUtil() {
    }

    static List<BetterChatModel.Segment> flatten(Component text) {
        List<BetterChatModel.Segment> segments = new ArrayList<>();
        text.visit((style, string) -> {
            if (string != null && !string.isEmpty()) {
                segments.add(new BetterChatModel.Segment(string, style == null ? Style.EMPTY : style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return segments;
    }

    static int resolveSegmentColor(BetterChatModel.Segment seg, Style style, HoverEvent hover) {
        int color = style.getColor() != null ? (0xFF << 24) | style.getColor().getValue() : theme().textPrimary();
        if (hover instanceof HoverEvent.ShowItem(net.minecraft.world.item.ItemStackTemplate itemTemplate)) {
            ItemStack item = itemTemplate.create();
            ItemStack stack = item.copy();

            BetterChatHoverCache cache = BetterChatStoreManager.getActiveCache();
            if (cache != null) {
                String keyId = BetterChatStoreManager.hoverItemKey(stack);
                String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                ItemStack cachedStack = cache.getItemByKeyOrId(keyId, itemId);
                if (!cachedStack.isEmpty()) stack = cachedStack;
            }
            if (IllegalItemUtil.isIllegal(stack)) {
                color = IllegalItemUtil.illegalColor();
            } else if (TopEnchantUtil.hasTopEnchant(stack)) {
                color = TopEnchantUtil.topColor();
            }
        }
        if (color == theme().textPrimary() && !(hover instanceof HoverEvent.ShowItem)) {
            BetterChatHoverCache cache = BetterChatStoreManager.getActiveCache();
            if (cache != null) {
                String cleaned = seg.text().replaceAll("[\\[\\]<>‚]", "").trim();
                if (!cleaned.isEmpty()) {
                    cache.findItemByDisplayNameWithMeta(cleaned);
                }
            }
        }
        return color;
    }
}