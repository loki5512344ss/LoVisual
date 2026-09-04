/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.nondraggable.impl;

import dev.loki.lovisual.features.gui.hud.nondraggable.impl.CustomBarModel.BarColors;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import static dev.loki.lovisual.features.gui.hud.nondraggable.impl.CustomBar.*;

final class CustomJumpBarRenderer {

    private CustomJumpBarRenderer() {
    }

    static boolean shouldUseJumpBar(Minecraft mc) {
        if (mc == null || mc.player == null) return false;
        var mount = mc.player.jumpableVehicle();
        return mount != null && (mc.player.getJumpRidingScale() > 0.0f || mount.getJumpCooldown() > 0);
    }

    static void renderJumpBarBackground(Renderer2D r2d, Minecraft mc) {
        if (mc == null || mc.player == null) return;
        CustomBar bm = CustomBar.get();
        if (bm == null || !bm.isHudBarEnabled() || !bm.isHotbarXpBarEnabled()) return;
        float progress = mc.player.jumpableVehicle() != null && mc.player.jumpableVehicle().getJumpCooldown() <= 0
                ? Mth.clamp(mc.player.getJumpRidingScale(), 0.0f, 1.0f)
                : 1.0f;
        float scale = (float) mc.getWindow().getGuiScale();
        float x = (mc.getWindow().getGuiScaledWidth() - XP_BAR_WIDTH) * 0.5f;
        float y = mc.getWindow().getGuiScaledHeight() - 22f - XP_BAR_OFFSET - XP_BAR_HEIGHT;
        CustomXpBarRenderer.drawVanillaGlassTrack(r2d, x, y, scale, progress, bm, false);
    }

    static void renderJumpBarTexture(GuiGraphicsExtractor ctx, Minecraft mc) {
        if (ctx == null || mc == null || mc.player == null) return;
        CustomBar bm = CustomBar.get();
        if (bm == null || !bm.isHudBarEnabled() || !bm.isHotbarXpBarEnabled()) return;
        var mount = mc.player.jumpableVehicle();
        if (mount == null) return;

        int x = Mth.floor((mc.getWindow().getGuiScaledWidth() - XP_BAR_WIDTH) * 0.5f);
        int y = Mth.floor(mc.getWindow().getGuiScaledHeight() - 22f - XP_BAR_OFFSET - XP_BAR_HEIGHT);
        BarColors colors = CustomXpBarRenderer.colors(bm);
        ctx.blitSprite(LoVisualRenderPipelines.GUI_TEXTURE_LOOKUP, JUMP_BACKGROUND, x, y, 182, 5,
                CustomXpBarRenderer.applyBarAlpha(colors.trackTexture(), bm));
        if (mount.getJumpCooldown() > 0) {
            ctx.blitSprite(LoVisualRenderPipelines.GUI_TEXTURE_LOOKUP, JUMP_COOLDOWN, x, y, 182, 5,
                    CustomXpBarRenderer.applyBarAlpha(colors.fillTexture(), bm));
            return;
        }
        int progress = Mth.lerpDiscrete(mc.player.getJumpRidingScale(), 0, 182);
        if (progress > 0) {
            ctx.blitSprite(LoVisualRenderPipelines.GUI_TEXTURE_LOOKUP, JUMP_PROGRESS, 182, 5, 0, 0, x, y, progress, 5,
                    CustomXpBarRenderer.applyBarAlpha(colors.fillTexture(), bm));
        }
    }
}
