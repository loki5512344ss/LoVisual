/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.layout.screen.modules;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.modules.model.ModulesMenuStyle;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.helpers.util.SystemCursor;

final class ModulesMenuGlassRenderer {
    private ModulesMenuGlassRenderer() {}

    static void renderPanelGlass(ModulesMenuPanel panel, float alpha, float scale) {
        if (alpha <= 0.001f) return;
        LayoutRender2D.roundedSoftShadow(
                panel.x, panel.y, panel.w, panel.h,
                ModulesMenuScreen.PANEL_RADIUS * scale,
                18.0f * scale, 0.018f,
                ModulesMenuLayout.withAlpha(ModulesMenuStyle.shadow(), alpha));
        float materialAlpha = alpha >= 0.999f ? 1.0f : alpha;
        drawLiquidGlass(panel.x, panel.y, panel.w, panel.h, ModulesMenuScreen.PANEL_RADIUS * scale, true, materialAlpha, scale);
    }

    static void drawLiquidGlass(float x, float y, float w, float h, float radius, boolean panel, float alpha, float scale) {
        if (alpha <= 0.001f) return;
        float materialAlpha = AnimationUtility.clamp(alpha, 0.0f, 1.0f);
        float blurAlpha = AnimationUtility.clamp(materialAlpha * (panel ? 1.20f : 1.04f), 0.0f, 1.0f);
        float thickness = (panel ? 15.5f : 12.5f) * scale;
        float fresnelPower = panel ? -18.0f : -16.0f;
        float fresnelAlpha = 1.0f;
        float baseAlpha = panel ? 0.78f : 0.84f;
        float fresnelMix = panel ? 0.52f : 0.48f;
        float distortion = (panel ? 0.190f : 0.155f) * materialAlpha;
        Renderer2D.COLOR.blurRect(x, y, w, h, radius, ClickGuiRenderer.clickGuiBlurQuality(), 1.0f, blurAlpha, 0xFFFFFF);
        Renderer2D.COLOR.liquidGlassRect(x, y, w, h, radius, thickness, 0xFFFFFFFF,
                materialAlpha, blurAlpha, fresnelPower, fresnelAlpha, baseAlpha, fresnelMix, distortion, 0.0f);
    }

    static void drawCategoryIcon(ModulesMenuPanel panel, int color, float scale) {
        float icon = 8.0f * scale;
        float ix = panel.x + panel.w - 10.0f * scale - icon;
        float iy = panel.y + ModulesMenuLayout.middle(icon, ModulesMenuScreen.HEADER_H * scale) + 0.5f * scale;
        Renderer2D.COLOR.svg(panel.category.icon(), ix, iy, icon, icon, SvgRenderOptions.overrideColor(color));
    }

    static void renderScrollbar(ModulesMenuPanel panel, float scroll, float maxScroll,
                                float clipY, float clipH, float alpha, float mouseX, float mouseY, float scale) {
        boolean settingsPage = panel.selected != null;
        if (maxScroll <= 0.5f) {
            if (settingsPage) {
                panel.settingsScrollbarX = panel.settingsScrollbarY = panel.settingsScrollbarW = panel.settingsScrollbarH = 0f;
            } else {
                panel.modulesScrollbarX = panel.modulesScrollbarY = panel.modulesScrollbarW = panel.modulesScrollbarH = 0f;
            }
            return;
        }
        float trackW = 2.5f * scale;
        float trackX = panel.x + panel.w - 5.0f * scale;
        float trackY = clipY + 5.0f * scale;
        float trackH = Math.max(1.0f, clipH - 10.0f * scale);
        int trackA = ModulesMenuStyle.scrollTrackA(alpha);
        int trackB = ModulesMenuStyle.scrollTrackB(alpha);
        int handleA = ModulesMenuStyle.scrollHandleA(alpha);
        int handleB = ModulesMenuStyle.scrollHandleB(alpha);
        boolean scrollbarHovered = ModulesMenuLayout.inside(mouseX, mouseY, trackX - 3.0f * scale, trackY, trackW + 6.0f * scale, trackH);
        boolean scrollbarDragging = settingsPage ? panel.settingsScrollbarDragging : panel.modulesScrollbarDragging;
        if (scrollbarHovered || scrollbarDragging) {
            SystemCursor.set(SystemCursor.CursorType.SCROLL);
        }
        LayoutRender2D.roundedQuad(trackX, trackY, trackW, trackH, trackW * 0.5f, trackA, trackB, trackB, trackA);
        float handleH = Math.max(18.0f * scale, trackH * (trackH / (trackH + maxScroll)));
        float handleY = trackY + (trackH - handleH) * (scroll / Math.max(1.0f, maxScroll));
        if (settingsPage) {
            panel.settingsScrollbarX = trackX;
            panel.settingsScrollbarY = trackY;
            panel.settingsScrollbarW = trackW;
            panel.settingsScrollbarH = trackH;
            panel.settingsScrollbarHandleY = handleY;
            panel.settingsScrollbarHandleH = handleH;
        } else {
            panel.modulesScrollbarX = trackX;
            panel.modulesScrollbarY = trackY;
            panel.modulesScrollbarW = trackW;
            panel.modulesScrollbarH = trackH;
            panel.modulesScrollbarHandleY = handleY;
            panel.modulesScrollbarHandleH = handleH;
        }
        LayoutRender2D.roundedQuad(trackX, handleY, trackW, handleH, trackW * 0.5f, handleA, handleB, handleB, handleA);
    }
}
