/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.panels;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

final class ModuleListRenderer {

    private ModuleListRenderer() {}

    static void render(ModuleList host, Renderer2D renderer, TextRenderer textRenderer,
                       GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        if (!host.isEnabled() || mc == null || mc.player == null) {
            host.rows.clear();
            host.setSize(0f, 0f);
            return;
        }

        TextRenderer listRenderer = Fonts.renderer("Comfortaa", FontInfo.Type.Regular, TextRenderer.get());
        if (listRenderer == null) listRenderer = textRenderer;

        float baseScale = HudScale.scale(screenW, screenH) * host.scaleValue.get().floatValue();
        float lineH = ModuleList.BASE_LINE_H * baseScale;
        float textOffsetY = ModuleList.BASE_TEXT_Y * baseScale;
        float textOffsetX = ModuleList.BASE_TEXT_X * baseScale;
        float textOffsetXRev = ModuleList.BASE_TEXT_X_REV * baseScale;
        float stringPad = ModuleList.BASE_STRING_PAD * baseScale;
        float rectExtra = ModuleList.BASE_RECT_EXTRA * baseScale;
        float radius = ModuleList.BASE_RADIUS * baseScale;
        float softness = ModuleList.BASE_SOFTNESS * baseScale;

        float fontHeight = Math.max(1.0f, (float) listRenderer.getHeight(false));
        float targetTextHeight = Math.max(4.4f * baseScale, lineH - 4.2f * baseScale);
        float textScale = Math.max(0.2f, targetTextHeight / fontHeight);

        int modulesHash = ModuleListData.collectEnabled(host);
        if (host.enabledModules.isEmpty()) {
            host.rows.clear();
            host.setSize(0f, 0f);
            return;
        }

        ModuleListData.rebuildRowsIfNeeded(host, listRenderer, textScale, modulesHash);

        float maxWidth = ModuleListData.computeMaxWidth(host, stringPad, rectExtra);
        float maxX = Math.max(0f, screenW - maxWidth);
        host.clampX(maxX);

        boolean reverse = host.getX() > (screenW * 0.5f);
        float drawBaseX = reverse ? (host.getX() + maxWidth) : host.getX();
        float listHeight = host.rows.size() * lineH;

        host.renderX = reverse ? (drawBaseX - maxWidth) : drawBaseX;
        host.renderY = host.getY();
        host.setSize(maxWidth, listHeight);

        int bgTop = ModuleListPalette.resolveBgTop(host);
        int bgBottom = ModuleListPalette.resolveBgBottom(host);
        int textTop = ModuleListPalette.resolveTextTop(host);
        int textBottom = ModuleListPalette.resolveTextBottom(host);
        boolean blur = ModuleListPalette.isBlurEffect(host);
        boolean glass = ModuleListPalette.isGlassEffect(host);
        boolean shimmer = host.textEffect.get() == ModuleList.TextEffect.SHIMMER;
        float time = (float) (Util.getMillis() / 1000.0);
        float blurQuality = host.hud != null ? host.hud.getBlurRadius() : 12.0f;
        boolean autoBatch = !Renderer2D.isBatching();
        if (autoBatch) renderer.begin();
        try {
            for (int i = 0; i < host.rows.size(); i++) {
                ModuleList.Row row = host.rows.get(i);
                float offsetY = host.getY() + i * lineH;
                float stringWidth = row.textWidth + stringPad;
                float rowX = reverse ? (drawBaseX - stringWidth) : drawBaseX;
                float rowW = stringWidth + rectExtra;
                row.x = rowX;
                row.y = offsetY;
                row.w = rowW;
                row.fillTop = ModuleListPalette.rowFill(host, bgTop, i, host.rows.size());
                row.fillBottom = ModuleListPalette.rowFill(host, bgBottom, i, host.rows.size());
            }
            if (blur) {
                renderer.blurComposite(composite -> {
                    for (int i = 0; i < host.rows.size(); i++) {
                        ModuleList.Row row = host.rows.get(i);
                        int tint = HudRenderUtil.mixColor(row.fillTop, row.fillBottom, 0.5f) & 0x00FFFFFF;
                        composite.roundedRect(row.x, row.y, row.w, lineH, radius, blurQuality, 1.0f, host.blurAlpha.get() / 255f, tint);
                    }
                });
            } else if (glass) {
                for (int i = 0; i < host.rows.size(); i++) {
                    ModuleList.Row row = host.rows.get(i);
                    ModuleListPalette.drawGlass(host, row.x, row.y, row.w, lineH, radius);
                }
            }
            for (int i = 0; i < host.rows.size(); i++) {
                ModuleList.Row row = host.rows.get(i);
                if (glass) {
                    int glassTop = HudRenderUtil.scaleAlpha(row.fillTop, 0.22f);
                    int glassBottom = HudRenderUtil.scaleAlpha(row.fillBottom, 0.18f);
                    renderer.roundedRectGradientQuad(row.x, row.y, row.w, lineH, radius, softness,
                            glassTop, glassTop, glassBottom, glassBottom);
                } else {
                    int drawTop = blur ? HudRenderUtil.scaleAlpha(row.fillTop, 0.42f) : row.fillTop;
                    int drawBottom = blur ? HudRenderUtil.scaleAlpha(row.fillBottom, 0.42f) : row.fillBottom;
                    renderer.roundedRectGradientQuad(row.x, row.y, row.w, lineH, radius, softness,
                            drawTop, drawTop, drawBottom, drawBottom);
                }
            }
        } finally {
            if (autoBatch) renderer.render();
        }

        int gradientTop = ModuleListPalette.resolveTextGradientTop(host);
        int gradientBottom = ModuleListPalette.resolveTextGradientBottom(host);
        boolean topRainbow = ModuleListPalette.isCustomMode(host) && host.textTopColor.isRainbow();
        boolean bottomRainbow = ModuleListPalette.isCustomMode(host) && host.textBottomColor.isRainbow();
        AnimatedRenderColors.Mode gradientMode = host.textGradientMode.get();
        int gradientSpeed = ModuleListPalette.hasAnimatedTextGradient(host) ? host.textGradientSpeed.get() : Math.max(1, host.textEffectSpeed.get());

        listRenderer.begin(textScale, false, false);
        float textHeight = (float) listRenderer.getHeight(false);
        float blockTop = host.renderY;
        float blockBottom = host.renderY + host.getHeight();
        for (ModuleList.Row row : host.rows) {
            float textX = reverse ? (drawBaseX - (row.textWidth + stringPad) + textOffsetXRev) : (drawBaseX + textOffsetX);
            float textY = row.y + textOffsetY + (lineH - textHeight - textOffsetY) * 0.08f;
            listRenderer.renderQuadGradient(row.label, textX, textY, (idx, cp, x0, y0, x1, y1, out) ->
                    AnimatedRenderColors.moduleListBlockGlyphGradient(
                            gradientTop, gradientBottom, gradientMode, gradientSpeed,
                            y0, y1, blockTop, blockBottom, idx, time, topRainbow, bottomRainbow, shimmer, out
                    ), false);
        }
        listRenderer.end();
    }
}
