/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.nondraggable.impl.tooltips;

import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.profiler.config.ProfilerPhase;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

final class ShulkerPreviewRenderer {

    private ShulkerPreviewRenderer() {
    }

    static void render(PreviewLayout preview, int baseX, int baseY, GuiGraphicsExtractor ctx) {
        if (preview == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) return;
        int slotBg = BetterTooltips.SHULKER_SLOT_BG;
        int border = BetterTooltips.SHULKER_SLOT_BORDER;
        float scale = ViewportContext.getScaleFactor();
        ViewportContext.beginUnscaled(ctx);
        Renderer2D.COLOR.begin();
        float stroke = 0.35f * scale;
        for (int row = 0; row < preview.rows(); row++) {
            for (int col = 0; col < preview.cols(); col++) {
                float sx = baseX + preview.pad() + col * (preview.slotSize() + preview.slotGap());
                float sy = baseY + preview.pad() + row * (preview.slotSize() + preview.slotGap());
                float xb = sx * scale;
                float yb = sy * scale;
                float wb = preview.slotSize() * scale;
                float hb = preview.slotSize() * scale;
                Renderer2D.COLOR.roundedRect(xb, yb, wb, hb,
                        BetterTooltips.SHULKER_SLOT_RADIUS * scale,
                        BetterTooltips.SHULKER_SLOT_SOFTNESS * scale, slotBg);
                Renderer2D.COLOR.roundedRectStroke(xb, yb, wb, hb,
                        BetterTooltips.SHULKER_SLOT_RADIUS * scale,
                        BetterTooltips.SHULKER_SLOT_SOFTNESS * scale, stroke, border);
            }
        }
        Renderer2D.COLOR.render();
        ViewportContext.end(ctx);
        if (ctx == null) return;
        ViewportContext.beginUnscaled(ctx);
        Renderer2D.COLOR.begin();
        float offset = (preview.slotSize() - 16f) * 0.5f;
        try (ProfilerPhase.Scope ignored = ProfilerPhase.scope("2d:tooltip:shulker_preview")) {
            int seed = 1;
            int idx = 0;
            for (int row = 0; row < preview.rows(); row++) {
                for (int col = 0; col < preview.cols(); col++) {
                    if (idx >= preview.stacks().size()) {
                        Renderer2D.COLOR.render();
                        ViewportContext.end(ctx);
                        return;
                    }
                    ItemStack stack = preview.stacks().get(idx);
                    if (stack == null || stack.isEmpty()) {
                        idx++;
                        continue;
                    }
                    float sx = (baseX + preview.pad() + col * (preview.slotSize() + preview.slotGap()) + offset) * scale;
                    float sy = (baseY + preview.pad() + row * (preview.slotSize() + preview.slotGap()) + offset) * scale;
                    Renderer2D.COLOR.item(stack, sx, sy, scale, seed++, Renderer2D.ITEM_OVERLAY_ALL, null);
                    idx++;
                }
            }
            Renderer2D.COLOR.render();
            ViewportContext.end(ctx);
        }
    }
}
