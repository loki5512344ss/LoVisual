/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl;

import dev.loki.lovisual.features.gui.hud.HudRenderUtil;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

import static dev.loki.lovisual.features.gui.hud.draggable.impl.TargetHud.*;

final class TargetHudEquipmentRenderer {

    private TargetHudEquipmentRenderer() {
    }

    static EquipmentLayout buildEquipmentLayout(TargetHud element, LivingEntity target, boolean preview, TextRenderer textRenderer, float scale) {
        if (!element.isEquipmentEnabled() || target == null || textRenderer == null || scale <= 0.0f) {
            return new EquipmentLayout(List.of(), 0.0f, 0.0f);
        }

        List<EquipmentEntry> entries = collectEquipmentEntries(target, preview, textRenderer, scale);
        if (entries.isEmpty()) {
            return new EquipmentLayout(List.of(), 0.0f, 0.0f);
        }

        float totalWidth = 0.0f;
        for (int i = 0; i < entries.size(); i++) {
            totalWidth += entries.get(i).width();
            if (i < entries.size() - 1) {
                totalWidth += EQUIPMENT_GAP * scale;
            }
        }
        float totalHeight = EQUIPMENT_TOP_GAP * scale + EQUIPMENT_SLOT * scale + EQUIPMENT_BOTTOM_PAD * scale;
        return new EquipmentLayout(entries, totalWidth, totalHeight);
    }

    private static List<EquipmentEntry> collectEquipmentEntries(LivingEntity target, boolean preview, TextRenderer textRenderer, float scale) {
        List<EquipmentEntry> entries = new ArrayList<>();

        if (preview) {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            ItemStack apple = new ItemStack(Items.GOLDEN_APPLE);
            ItemStack head = new ItemStack(Items.DIAMOND_HELMET);
            ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
            ItemStack legs = new ItemStack(Items.DIAMOND_LEGGINGS);
            ItemStack feet = new ItemStack(Items.DIAMOND_BOOTS);

            sword.setDamageValue(Math.max(1, sword.getMaxDamage() / 6));
            head.setDamageValue(Math.max(1, head.getMaxDamage() / 3));
            chest.setDamageValue(Math.max(1, chest.getMaxDamage() / 2));
            legs.setDamageValue(Math.max(1, legs.getMaxDamage() / 4));
            feet.setDamageValue(Math.max(1, feet.getMaxDamage() / 5));

            addEquipmentEntry(entries, sword, scale);
            addEquipmentEntry(entries, apple, scale);
            addEquipmentEntry(entries, head, scale);
            addEquipmentEntry(entries, chest, scale);
            addEquipmentEntry(entries, legs, scale);
            addEquipmentEntry(entries, feet, scale);
            return entries;
        }

        addEquipmentEntry(entries, target.getMainHandItem(), scale);
        addEquipmentEntry(entries, target.getOffhandItem(), scale);
        addEquipmentEntry(entries, target.getItemBySlot(EquipmentSlot.HEAD), scale);
        addEquipmentEntry(entries, target.getItemBySlot(EquipmentSlot.CHEST), scale);
        addEquipmentEntry(entries, target.getItemBySlot(EquipmentSlot.LEGS), scale);
        addEquipmentEntry(entries, target.getItemBySlot(EquipmentSlot.FEET), scale);
        return entries;
    }

    private static void addEquipmentEntry(List<EquipmentEntry> entries, ItemStack stack, float scale) {
        float baseWidth = EQUIPMENT_SLOT * scale;
        entries.add(new EquipmentEntry(stack == null ? ItemStack.EMPTY : stack, baseWidth));
    }

    static void drawEquipment(TargetHud element, Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx,
                              EquipmentLayout layout, float x, float y, float panelWidth, float equipmentHeight,
                              float scaleFactor, float alphaFactor, boolean glassEnabled) {
        if (ctx == null || layout.entries().isEmpty() || alphaFactor <= 0.01f) {
            return;
        }

        float rowX = x + Math.max(0.0f, (panelWidth - layout.width()) * 0.5f);
        float rowY = y + Math.max(0.0f, equipmentHeight - layout.height()) + (EQUIPMENT_ROW_Y_OFFSET * scaleFactor);

        float slotHeight = EQUIPMENT_SLOT * scaleFactor;
        float radius = 2.2f * scaleFactor;
        List<EquipmentRenderTask> itemTasks = new ArrayList<>(layout.entries().size());
        int slotFillLeft = HudRenderUtil.scaleAlpha(
                HudRenderUtil.mixColor(element.uiBarBackground, element.uiBgPrimary, 0.34f),
                alphaFactor
        );
        int slotFillRight = HudRenderUtil.scaleAlpha(
                HudRenderUtil.mixColor(element.uiBgSecondary, element.uiBarBackground, 0.24f),
                alphaFactor * 0.96f
        );
        int slotStroke = HudRenderUtil.scaleAlpha(
                HudRenderUtil.mixColor(element.uiStroke, element.uiBgSecondary, 0.10f),
                alphaFactor * 0.46f
        );
        int slotHighlight = HudRenderUtil.scaleAlpha(
                HudRenderUtil.mixColor(element.uiTextPrimary, element.uiBgPrimary, 0.88f),
                alphaFactor * 0.08f
        );
        float itemScale = Math.max(0.35f, (slotHeight - 3.0f * scaleFactor) / 16.0f);
        float cursorX = rowX;
        for (EquipmentEntry entry : layout.entries()) {
            if (glassEnabled) {
                TargetHudPanelRenderer.drawGlassCard(element, cursorX, rowY, entry.width(), slotHeight, radius, scaleFactor, alphaFactor);
                renderer.roundedRect(cursorX, rowY, entry.width(), slotHeight, radius, 1.0f,
                        HudRenderUtil.glassSmallBackground(alphaFactor));
            } else {
                renderer.roundedRectGradientQuad(
                        cursorX, rowY, entry.width(), slotHeight, radius, 1.0f,
                        slotFillLeft, slotFillRight, slotFillRight, slotFillLeft
                );
                renderer.roundedRectStroke(cursorX, rowY, entry.width(), slotHeight, radius, 1.0f, 0.5f * scaleFactor, slotStroke);
                renderer.roundedRect(
                        cursorX + 0.75f * scaleFactor,
                        rowY + 0.75f * scaleFactor,
                        Math.max(0.0f, entry.width() - 1.5f * scaleFactor),
                        Math.max(0.6f, 0.75f * scaleFactor),
                        Math.max(0.5f, radius * 0.45f),
                        1.0f,
                        slotHighlight
                );
            }

            if (entry.stack() != null && !entry.stack().isEmpty()) {
                float itemX = cursorX + 1.5f * scaleFactor;
                float itemY = rowY + (slotHeight - 16.0f * itemScale) * 0.5f;
                itemTasks.add(new EquipmentRenderTask(entry.stack(), itemX, itemY, itemScale));
            } else {
                drawEmptyEquipmentCross(element, renderer, cursorX, rowY, entry.width(), slotHeight, scaleFactor, alphaFactor);
            }

            cursorX += entry.width() + EQUIPMENT_GAP * scaleFactor;
        }
        if (!itemTasks.isEmpty()) {
            int seed = 0;
            for (EquipmentRenderTask task : itemTasks) {
                drawItem(renderer, task.stack(), task.x(), task.y(), task.scale(), seed++, alphaFactor);
            }
        }
    }

    static void drawItem(Renderer2D renderer, ItemStack stack, float x, float y, float itemScale, int seed, float alphaFactor) {
        if (renderer == null || stack == null || stack.isEmpty() || alphaFactor <= 0.001f) {
            return;
        }

        double previousAlpha = renderer.getAlpha();
        renderer.setAlpha(previousAlpha * AnimationUtility.clamp01(alphaFactor));
        try {
            renderer.item(stack, x, y, itemScale, seed, Renderer2D.ITEM_OVERLAY_NONE, null);
        } finally {
            renderer.setAlpha(previousAlpha);
        }
    }

    private static void drawEmptyEquipmentCross(TargetHud element,
                                                Renderer2D renderer,
                                                float x,
                                                float y,
                                                float width,
                                                float height,
                                                float scaleFactor,
                                                float alphaFactor) {
        float inset = Math.max(2.0f * scaleFactor, width * 0.28f);
        float stroke = Math.max(0.6f, 0.85f * scaleFactor);
        int color = HudRenderUtil.scaleAlpha(element.uiTextSecondary, alphaFactor);
        renderer.line(x + inset, y + inset, x + width - inset, y + height - inset, color);
        renderer.line(x + width - inset, y + inset, x + inset, y + height - inset, color);
        if (stroke > 1.0f) {
            float nudge = stroke * 0.35f;
            renderer.line(x + inset + nudge, y + inset, x + width - inset + nudge, y + height - inset, color);
            renderer.line(x + width - inset - nudge, y + inset, x + inset - nudge, y + height - inset, color);
        }
    }
}
