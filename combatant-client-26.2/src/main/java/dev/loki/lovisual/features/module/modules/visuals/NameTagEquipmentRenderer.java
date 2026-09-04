/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.util.pvp.CooldownRegistry;
import dev.loki.lovisual.util.pvp.ItemCooldownSnapshot;
import dev.loki.lovisual.util.pvp.opponents.OpponentCooldownManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

final class NameTagEquipmentRenderer {

    private static final int ICON_SIZE = 24;
    private static final int ICON_GAP = 5;
    private static final float EQUIP_ICON_SCALE = 1.10f;
    private static final float ENCHANT_TEXT_SHADOW_OFFSET = 0.7f;

    private NameTagEquipmentRenderer() {
    }

    static int equipIconSize() {
        return Math.round(ICON_SIZE * EQUIP_ICON_SCALE);
    }

    static int equipIconGap() {
        return Math.round(ICON_GAP * EQUIP_ICON_SCALE);
    }

    static void drawRow(NameTags self, GuiGraphicsExtractor ctx, Player player, ItemStack[] slots, int centerX, int y) {
        int iconSize = equipIconSize();
        int gap = equipIconGap();
        int rowWidth = slots.length * (iconSize + gap) - gap;
        int startX = centerX - rowWidth / 2;
        int seed = 0;

        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = slots[i];
            int x = startX + i * (iconSize + gap);
            if (stack.isEmpty()) continue;

            float scale = iconSize / 16f;
            int overlayFlags = Renderer2D.ITEM_OVERLAY_COUNT;
            if (self.toggles.get("Item durability bar")) {
                overlayFlags |= Renderer2D.ITEM_OVERLAY_DURABILITY;
            }
            Renderer2D.COLOR.item(stack, x, y, scale, seed++, overlayFlags, null,
                    self.durabilityStartPercent.get(), 70);
            drawOpponentItemCooldown(self, player, stack, x, y, scale);
        }
    }

    static void drawOpponentItemCooldown(NameTags self, Player player, ItemStack stack, float x, float y, float scale) {
        if (!NameTagOpponentRenderer.shouldRenderOpponentStuff(self, player)) return;
        if (stack == null || stack.isEmpty()) return;

        Item item = stack.getItem();
        if (!CooldownRegistry.isTracked(item)) return;

        ItemCooldownSnapshot snapshot = OpponentCooldownManager.snapshot(player.getUUID(), item);
        if (!snapshot.visible()) return;
        float progress = snapshot.cooling() ? snapshot.cooldownProgress() : snapshot.usesProgress();
        if (progress <= 0f) return;

        int top = Mth.floor(16.0F * (1.0F - progress));
        int bottom = top + Mth.ceil(16.0F * progress);
        Renderer2D.COLOR.quad(
                x,
                y + top * scale,
                16.0f * scale,
                (bottom - top) * scale,
                snapshot.cooling() ? 0x80FFA500 : 0x706D9DFF
        );
    }

    static void drawRowText(TextRenderer textRenderer, ItemStack[] slots,
                            List<List<NameTagModel.EnchantLine>> enchantLines, int centerX, int y) {
        int iconSize = equipIconSize();
        int gap = equipIconGap();
        int rowWidth = slots.length * (iconSize + gap) - gap;
        int startX = centerX - rowWidth / 2;

        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = slots[i];
            int x = startX + i * (iconSize + gap);
            if (stack.isEmpty()) continue;

            List<NameTagModel.EnchantLine> ench = enchantLines.get(i);
            if (!ench.isEmpty()) {
                int lineHeight = (int) textRenderer.getHeight(true) + 1;
                int startY = y - 14;
                for (int idx = 0; idx < ench.size(); idx++) {
                    NameTagModel.EnchantLine line = ench.get(idx);
                    int lineY = startY - idx * lineHeight;
                    int textX = x + iconSize / 2 - (int) textRenderer.getWidth(line.text(), true) / 2;
                    renderEquipmentText(textRenderer, line.text(), textX, lineY, line.color());
                }
            }
        }
    }

    private static void renderEquipmentText(TextRenderer textRenderer, String text, double x, double y, int color) {
        RenderColor shadow = new RenderColor(0xB0000000);
        double o = ENCHANT_TEXT_SHADOW_OFFSET;
        textRenderer.render(text, x + o, y, shadow, false);
        textRenderer.render(text, x - o, y, shadow, false);
        textRenderer.render(text, x, y + o, shadow, false);
        textRenderer.render(text, x, y - o, shadow, false);
        textRenderer.render(text, x, y, new RenderColor(color), false);
    }
}
