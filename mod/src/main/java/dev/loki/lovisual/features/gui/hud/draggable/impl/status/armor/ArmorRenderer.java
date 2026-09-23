/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.status;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

final class ArmorRenderer {

    private ArmorRenderer() {}

    static void render(Armor host, Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx,
                       float tickDelta, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        host.layoutReady = false;
        boolean preview = DraggableHudElementRegistry.isForceVisible();
        float scale = HudScale.scale(screenW, screenH) * host.scaleValue.get().floatValue();
        boolean horizontal = host.isHorizontal();

        host.slotSize = Armor.DEFAULT_SLOT_SIZE * scale;
        host.slotGap = Armor.DEFAULT_SLOT_GAP * scale;
        host.slotRadius = Armor.PANEL_RADIUS * scale;
        host.itemScale = scale;

        float w = horizontal ? Armor.SLOTS * host.slotSize + (Armor.SLOTS - 1) * host.slotGap : host.slotSize;
        float h = horizontal ? host.slotSize : Armor.SLOTS * host.slotSize + (Armor.SLOTS - 1) * host.slotGap;
        host.setArmorSize(w, h);

        if (mc == null || (mc.player == null && !preview)) {
            host.setEmptySize();
            return;
        }
        if (!preview && !host.isEnabled()) {
            host.setEmptySize();
            return;
        }

        ItemStack[] stacks = (mc != null && mc.player != null) ? ArmorData.getArmorStacks() : new ItemStack[0];
        if (preview && !ArmorData.hasAnyStacks(stacks)) {
            stacks = ArmorData.previewStacks();
        }
        host.cachedStacks = stacks;
        if (!ArmorData.hasAnyStacks(host.cachedStacks) && !ArmorData.shouldShowWhenEmpty(host) && !preview) {
            host.setEmptySize();
            return;
        }

        boolean reverse = host.getX() > screenW * 0.5f;
        host.renderX = reverse ? (host.getX() - host.getWidth()) : host.getX();
        host.renderY = host.getY();
        host.baseX = host.renderX;
        host.baseY = host.renderY;

        float glassScale = Armor.PANEL_RADIUS <= 0.0f ? 1.0f : host.slotRadius / Armor.PANEL_RADIUS;
        HudRenderUtil.drawLiquidGlass(
                host.baseX,
                host.baseY,
                host.getWidth(),
                host.getHeight(),
                host.slotRadius,
                glassScale,
                true,
                Armor.GLASS_BLUR_STRENGTH,
                Armor.GLASS_ALPHA
        );
        renderer.roundedRectCorners(
                host.baseX,
                host.baseY,
                host.getWidth(),
                host.getHeight(),
                host.slotRadius,
                host.slotRadius,
                host.slotRadius,
                host.slotRadius,
                1.0f,
                HudRenderUtil.glassSmallBackground(Armor.GLASS_ALPHA)
        );
        drawSlotDividers(host, renderer, horizontal, scale);

        host.layoutReady = true;
    }

    static void renderForeground(Armor host, Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx,
                                 float tickDelta, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        if (!host.layoutReady || mc == null || mc.player == null || ctx == null) return;

        ItemStack[] stacks = host.cachedStacks != null ? host.cachedStacks : ArmorData.getArmorStacks();
        float iconSize = 16f * host.itemScale;
        float offset = (host.slotSize - iconSize) * 0.5f;
        boolean horizontal = host.isHorizontal();
        boolean reverseOrder = host.isReverseOrder();
        Armor.StateMode mode = host.stateMode.get();
        int overlayFlags = mode == Armor.StateMode.TEXT
                ? Renderer2D.ITEM_OVERLAY_DURABILITY_TEXT
                : Renderer2D.ITEM_OVERLAY_DURABILITY;
        int durabilityThreshold = mode == Armor.StateMode.TEXT ? host.stateThreshold.get() : host.stateBarThreshold.get();
        int durabilityColorThreshold = mode == Armor.StateMode.TEXT ? host.stateColorThreshold.get() : host.stateBarThreshold.get();

        int seed = 0;
        for (int i = 0; i < stacks.length; i++) {
            int idx = reverseOrder ? (Armor.SLOTS - 1 - i) : i;
            ItemStack stack = stacks[idx];
            if (stack == null || stack.isEmpty()) continue;
            float sx = host.baseX + (horizontal ? i * (host.slotSize + host.slotGap) : 0f) + offset;
            float sy = host.baseY + (horizontal ? 0f : i * (host.slotSize + host.slotGap)) + offset;

            // Armor widget is laid out in UNSCALED_LOGICAL coordinates together with its slots.
            renderer.item(stack, sx, sy, host.itemScale, seed++, overlayFlags, null,
                    durabilityThreshold, durabilityColorThreshold);
        }
    }

    static void drawSlotDividers(Armor host, Renderer2D renderer, boolean horizontal, float scale) {
        if (renderer == null) return;
        float dividerThickness = Math.max(0.5f, Armor.DIVIDER_THICKNESS * scale);
        float dividerLength = Math.max(6.0f * scale, host.slotSize * 0.52f);
        float dividerInset = Math.max(0.0f, (host.slotSize - dividerLength) * 0.5f);
        int divider = HudRenderUtil.scaleAlpha(0xFFFFFFFF, 0.12f);
        int dividerShadow = HudRenderUtil.scaleAlpha(0xFF000000, 0.10f);
        for (int i = 0; i < Armor.SLOTS - 1; i++) {
            if (horizontal) {
                float lineX = host.baseX + host.slotSize + i * (host.slotSize + host.slotGap)
                        + host.slotGap * 0.5f - dividerThickness * 0.5f;
                float lineY = host.baseY + dividerInset;
                renderer.quad(lineX + dividerThickness, lineY, dividerThickness, dividerLength, dividerShadow);
                renderer.quad(lineX, lineY, dividerThickness, dividerLength, divider);
            } else {
                float lineX = host.baseX + dividerInset;
                float lineY = host.baseY + host.slotSize + i * (host.slotSize + host.slotGap)
                        + host.slotGap * 0.5f - dividerThickness * 0.5f;
                renderer.quad(lineX, lineY + dividerThickness, dividerLength, dividerThickness, dividerShadow);
                renderer.quad(lineX, lineY, dividerLength, dividerThickness, divider);
            }
        }
    }
}
