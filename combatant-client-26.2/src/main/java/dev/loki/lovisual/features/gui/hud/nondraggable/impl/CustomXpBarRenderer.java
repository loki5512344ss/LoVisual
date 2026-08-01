/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.nondraggable.impl;

import dev.loki.lovisual.features.gui.hud.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.CustomBarModel.BarColors;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.CustomBarModel.XpTextInfo;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import static dev.loki.lovisual.features.gui.hud.nondraggable.impl.CustomBar.*;
import static dev.loki.lovisual.features.theme.Theme.theme;

final class CustomXpBarRenderer {

    private CustomXpBarRenderer() {
    }

    static void renderXpBarBackground(Renderer2D r2d, Minecraft mc) {
        if (mc == null || mc.player == null) return;
        CustomBar bm = CustomBar.get();
        if (bm == null || !bm.isHudBarEnabled() || !bm.isHotbarXpBarEnabled()) return;

        float progress = Mth.clamp(mc.player.experienceProgress, 0.0f, 1.0f);
        float scale = (float) mc.getWindow().getGuiScale();
        float screenW = mc.getWindow().getGuiScaledWidth();
        float screenH = mc.getWindow().getGuiScaledHeight();

        float baseY = screenH - 22f;

        float x = (screenW - XP_BAR_WIDTH) * 0.5f;
        float y = baseY - XP_BAR_OFFSET - XP_BAR_HEIGHT;
        drawVanillaGlassTrack(r2d, x, y, scale, progress, bm, false);
    }

    static void drawXpBar(Renderer2D r2d, float barX, float barY, float scale, float progress, CustomBar bm) {
        if (r2d == null || bm == null) return;
        drawVanillaGlassTrack(r2d, barX, barY, scale, progress, bm, false);
    }

    static void renderXpBarTexture(GuiGraphicsExtractor ctx, Minecraft mc) {
        if (ctx == null || mc == null || mc.player == null) return;
        CustomBar bm = CustomBar.get();
        if (bm == null || !bm.isHudBarEnabled() || !bm.isHotbarXpBarEnabled()) return;
        int k = mc.player.getXpNeededForNextLevel();
        if (k <= 0) return;

        int x = Mth.floor((mc.getWindow().getGuiScaledWidth() - XP_BAR_WIDTH) * 0.5f);
        int y = Mth.floor(mc.getWindow().getGuiScaledHeight() - 22f - XP_BAR_OFFSET - XP_BAR_HEIGHT);
        BarColors colors = colors(bm);
        ctx.blitSprite(LoVisualRenderPipelines.GUI_TEXTURE_LOOKUP, XP_BACKGROUND, x, y, 182, 5,
                applyBarAlpha(colors.trackTexture(), bm));
        int progress = (int) (Mth.clamp(mc.player.experienceProgress, 0.0f, 1.0f) * 183.0f);
        if (progress > 0) {
            ctx.blitSprite(LoVisualRenderPipelines.GUI_TEXTURE_LOOKUP, XP_PROGRESS, 182, 5, 0, 0, x, y, progress, 5,
                    applyBarAlpha(colors.fillTexture(), bm));
        }
    }

    static void drawVanillaGlassTrack(Renderer2D r2d, float barX, float barY, float scale,
                                      float progress, CustomBar bm, boolean locator) {
        if (r2d == null || bm == null) return;

        float x = barX * scale;
        float y = barY * scale;
        float w = XP_BAR_WIDTH * scale;
        float h = XP_BAR_HEIGHT * scale;
        float radius = XP_BAR_RADIUS * scale;
        BarColors colors = colors(bm);

        HudRenderUtil.drawLiquidGlass(x, y, w, h, radius, scale, false, BAR_BLUR_ALPHA, BAR_GLASS_ALPHA);

        int trackA = applyBarAlpha(colors.track(), bm);
        int stroke = applyBarAlpha(colors.stroke(), bm);
        r2d.roundedRectGradient(x, y, w, h, radius, BAR_SOFTNESS * scale, trackA,
                HudRenderUtil.scaleAlpha(trackA, 0.76f), 0.0f);

        float fillW = XP_BAR_WIDTH * Mth.clamp(progress, 0.0f, 1.0f) * scale;
        if (fillW > 0.0f && !locator) {
            float fillRadius = Math.min(radius, Math.min(fillW, h) * 0.5f);
            r2d.roundedRectMaskedQuad(
                    x, y, fillW, h,
                    x, y, w, h,
                    radius, BAR_SOFTNESS * scale,
                    colors.fillStart(), colors.fillEnd(), colors.fillEnd(), colors.fillStart()
            );
            if (fillRadius > 0.0f) {
                r2d.roundedRectGradient(x, y, fillW, Math.max(scale, h * 0.42f), fillRadius,
                        BAR_SOFTNESS * scale, HudRenderUtil.scaleAlpha(0xFFFFFFFF, 0.10f),
                        HudRenderUtil.scaleAlpha(0xFFFFFFFF, 0.02f), 90.0f);
            }
        }

        if (LOCATOR_TICK_COUNT > 0 && locator) {
            float lineW = Math.max(1.0f, 0.65f * scale);
            float step = w / (LOCATOR_TICK_COUNT + 1f);
            int tickColor = HudRenderUtil.scaleAlpha(colors.fillTexture(), 0.36f);
            for (int i = 1; i <= LOCATOR_TICK_COUNT; i++) {
                float lx = x + step * i - lineW * 0.5f;
                r2d.quad(lx, y + scale, lineW, Math.max(scale, h - 2.0f * scale), tickColor);
            }
        }

        r2d.roundedRectStroke(x, y, w, h, radius, BAR_SOFTNESS * scale, Math.max(0.65f, 0.72f * scale), stroke);
    }

    static BarColors colors(CustomBar bm) {

        if (bm != null && bm.isCustomMode()) {
            int track = HudRenderUtil.scaleAlpha(bm.customBgColor.getArgb(), BAR_TRACK_ALPHA);
            int fillStart = HudRenderUtil.scaleAlpha(bm.customFillColor.getArgb(), BAR_FILL_ALPHA);
            int fillEnd = HudRenderUtil.scaleAlpha(bm.customFillGradientColor.getArgb(), BAR_FILL_ALPHA);
            int stroke = HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(fillEnd, track, 0.38f), BAR_STROKE_ALPHA);
            int textureTrack = forceOpaque(HudRenderUtil.mixColor(track, 0xFFFFFFFF, 0.18f));
            int textureFill = forceOpaque(HudRenderUtil.mixColor(fillStart, fillEnd, 0.48f));
            int locator = forceOpaque(HudRenderUtil.mixColor(fillEnd, track, 0.16f));
            return new BarColors(track, fillStart, fillEnd, stroke, textureTrack, textureFill, locator);
        }

        int window = theme().windowBg();
        int surface = theme().surface();
        int accent = theme().accent();
        int soft = theme().accentSoft();
        int track = HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(window, surface, 0.44f), BAR_TRACK_ALPHA);
        int fillStart = HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(soft, accent, 0.24f), BAR_FILL_ALPHA);
        int fillEnd = HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(accent, theme().textPrimary(), 0.18f), BAR_FILL_ALPHA);
        int stroke = HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(theme().windowStroke(), accent, 0.18f), BAR_STROKE_ALPHA);
        int textureTrack = forceOpaque(HudRenderUtil.mixColor(track, theme().textMuted(), 0.22f));
        int textureFill = forceOpaque(HudRenderUtil.mixColor(fillStart, fillEnd, 0.50f));
        int locator = forceOpaque(HudRenderUtil.mixColor(accent, 0xFF1E7A4C, 0.42f));
        return new BarColors(track, fillStart, fillEnd, stroke, textureTrack, textureFill, locator);
    }

    static int forceOpaque(int argb) {
        return 0xFF000000 | (argb & 0x00FFFFFF);
    }
    static void renderXpBarText(GuiGraphicsExtractor ctx, Minecraft mc) {
        if (mc == null || mc.player == null || ctx == null) return;
        CustomBar bm = CustomBar.get();
        if (bm == null || !bm.isHudBarEnabled() || !bm.isHotbarXpBarEnabled() || !bm.isHotbarXpBarShowLevel()) return;
        if (mc.player.experienceLevel <= 0) return;

        Component text = Component.translatable("gui.experience.level", mc.player.experienceLevel);
        int x = (ctx.guiWidth() - mc.font.width(text)) / 2 + XP_TEXT_OFFSET;
        int y = ctx.guiHeight() - 24 - 9 - 2 + XP_TEXT_OFFSET;
        int color = applyBarAlpha(bm.getXpTextArgb(), bm);
        ctx.text(mc.font, text, x + 1, y, 0xFF000000, false);
        ctx.text(mc.font, text, x - 1, y, 0xFF000000, false);
        ctx.text(mc.font, text, x, y + 1, 0xFF000000, false);
        ctx.text(mc.font, text, x, y - 1, 0xFF000000, false);
        ctx.text(mc.font, text, x, y, color, false);
    }

    static void renderPreview(Renderer2D r2d, float x, float y, float scale, float progress) {
        CustomBar bm = CustomBar.get();
        if (bm == null) return;
        drawXpBar(r2d, x, y, scale, progress, bm);
    }

    static XpTextInfo buildPreviewText(float barX, float barY, float scale, int level) {
        CustomBar bm = CustomBar.get();
        if (bm == null) return new XpTextInfo("", 0, 0, 0, (float) XP_TEXT_SCALE * scale);
        if (level <= 0) return new XpTextInfo("", 0, 0, 0, bm.getXpTextScale() * scale);

        String text = String.valueOf(level);
        TextRenderer tr = TextRenderer.get();
        float textScale = bm.getXpTextScale() * scale;
        tr.begin(textScale, false, false);
        int textW = (int) Math.ceil(tr.getWidth(text, true));
        int textH = (int) Math.ceil(tr.getHeight(true));
        tr.end();

        float barW = XP_BAR_WIDTH * scale;
        int tx = Math.round(barX * scale + (barW - textW) * 0.5f) + Math.round(XP_TEXT_OFFSET * scale);
        int ty = Math.round(barY * scale - textH - XP_TEXT_OFFSET * scale) + Math.round(XP_TEXT_OFFSET * scale);

        int color = applyBarAlpha(bm.getXpTextArgb(), bm);
        return new XpTextInfo(text, tx, ty, color, textScale);
    }

    static int applyBarAlpha(int argb, CustomBar bm) {
        if (bm == null) return argb;
        int a = (argb >>> 24) & 0xFF;
        int na = (int) (a * bm.getHudOverlayAlphaFactor());
        return (argb & 0x00FFFFFF) | ((na & 0xFF) << 24);
    }
}
