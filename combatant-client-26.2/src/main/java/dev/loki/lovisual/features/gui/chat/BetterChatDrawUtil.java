/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.chat;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.hud.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.draggable.impl.BetterChat;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.helpers.SystemCursor;
import net.minecraft.util.Mth;

import static dev.loki.lovisual.features.theme.Theme.theme;

/**
 * Low-level draw primitives + color helpers shared by the chat renderer helpers.
 */
final class BetterChatDrawUtil {
    private BetterChatDrawUtil() {
    }

    static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    static int mulAlpha(int argb, float a) {
        int alpha = (int) (((argb >>> 24) & 0xFF) * a);
        return (argb & 0x00FFFFFF) | (Mth.clamp(alpha, 0, 255) << 24);
    }

    static void applyColor(int argb) {
        BetterChatRenderer.TMP_COLOR.a = (argb >>> 24) & 0xFF;
        BetterChatRenderer.TMP_COLOR.r = (argb >>> 16) & 0xFF;
        BetterChatRenderer.TMP_COLOR.g = (argb >>> 8) & 0xFF;
        BetterChatRenderer.TMP_COLOR.b = argb & 0xFF;
    }

    static void drawRoundedRect(float x, float y, float w, float h, float radius, int argb) {
        if (BetterChatRenderer.renderer == null) return;
        if (((argb >>> 24) & 0xFF) <= 0) return;
        BetterChat cfg = BetterChat.get();
        boolean useGradient = cfg != null && cfg.gradientEnabled();
        HudRenderUtil.drawHudBackground(BetterChatRenderer.renderer, x, y, w, h, radius, 1.1f, argb, useGradient);
    }

    static void drawRoundedRectStrokeGradient(float x, float y, float w, float h, float radius, float thickness,
                                              int startArgb, int endArgb) {
        if (BetterChatRenderer.renderer == null) return;
        if (((startArgb >>> 24) & 0xFF) <= 0 && ((endArgb >>> 24) & 0xFF) <= 0) return;
        BetterChatRenderer.renderer.roundedRectStrokeGradient(x, y, w, h, radius, 1.1f, thickness, startArgb, endArgb, (float) 0.0);
    }

    static void drawRect(float x, float y, float w, float h, int argb) {
        if (BetterChatRenderer.renderer == null) return;
        if (((argb >>> 24) & 0xFF) <= 0) return;
        BetterChatRenderer.renderer.quad(x, y, w, h, argb);
    }

    static void flushRenderer() {
        if (BetterChatRenderer.renderer == null) return;
        if (Renderer2D.isBatching()) {
            Renderer2D.flushBatch();
        } else {
            BetterChatRenderer.renderer.begin();
        }
    }

    static void drawGlassPill(float x, float y, float w, float h, float radius, float alpha, boolean softPanel) {
        if (BetterChatRenderer.renderer == null || w <= 0f || h <= 0f) return;
        BetterChat settings = BetterChat.get();
        float drawAlpha = Mth.clamp(alpha, 0.0f, 1.0f);
        if (drawAlpha <= 0.001f) return;
        float configuredAlpha = settings != null ? settings.liquidGlassAlphaFactor() : (230f / 255f);
        float blurStrength = configuredAlpha * drawAlpha;
        float materialAlpha = Mth.clamp(0.72f + configuredAlpha * 0.28f, 0.0f, 1.0f) * drawAlpha;
        float glassRadius = Math.max(2f, radius);
        float thickness = softPanel ? 15.5f : 12.5f;
        float fresnelPower = softPanel ? -26.0f : -20.0f;
        float fresnelAlpha = Mth.clamp(0.72f + configuredAlpha * 0.28f, 0.0f, 1.0f) * drawAlpha;
        float baseAlpha = Mth.clamp((softPanel ? 0.66f : 0.68f) + configuredAlpha * (softPanel ? 0.34f : 0.32f), 0.0f, 1.0f);
        float fresnelMix = softPanel ? 0.46f : 0.44f;
        float distortPx = (softPanel ? 0.172f : 0.145f) * drawAlpha;

        flushRenderer();
        BetterChatRenderer.renderer.liquidGlassRect(
                x, y, w, h,
                glassRadius,
                thickness,
                theme().accent(),
                materialAlpha,
                blurStrength,
                fresnelPower,
                fresnelAlpha,
                baseAlpha,
                fresnelMix,
                distortPx,
                softPanel ? 2.65f : 2.25f
        );

        int veilAlpha = Math.round((softPanel ? 0x16 : 0x0C) * drawAlpha);
        if (veilAlpha > 0) {
            drawRoundedRect(x, y, w, h, glassRadius, withAlpha(theme().windowBg(), veilAlpha));
        }
    }

    static void drawLiquidHoverPill(float x, float y, float w, float h, float radius, float alpha) {
        if (BetterChatRenderer.renderer == null || w <= 0f || h <= 0f || alpha <= 0.001f) return;
        flushRenderer();
        BetterChatRenderer.renderer.liquidGlassRect(
                x, y, w, h,
                Math.max(1.0f, radius),
                10.5f,
                theme().accentSoft(),
                Mth.clamp(alpha, 0.0f, 1.0f),
                0.08f,
                -18.0f,
                0.82f,
                0.72f,
                0.34f,
                0.095f,
                1.2f
        );
        drawRoundedRect(x, y, w, h, radius, withAlpha(theme().surfaceHover(), 0x32));
        drawRoundedRectStrokeGradient(x, y, w, h, radius, 0.55f,
                withAlpha(theme().accentSoft(), 0x50),
                withAlpha(theme().accent(), 0x34));
    }

    static void drawScrollbarGlass(float trackX, float trackY, float trackW, float trackH,
                                   float thumbY, float thumbH, boolean hot, int trackAlpha, int thumbAlpha) {
        if (BetterChatRenderer.renderer == null || trackW <= 0f || trackH <= 0f || thumbH <= 0f) return;
        SettingsGuiPalette palette = SettingsGuiPalette.current();
        int trackA = withAlpha(palette.moduleScrollTrackA(), trackAlpha);
        int trackB = withAlpha(palette.moduleScrollTrackB(), Math.min(255, trackAlpha + 0x18));
        int handleA = withAlpha(palette.moduleScrollHandleA(), thumbAlpha);
        int handleB = withAlpha(palette.moduleScrollHandleB(), Math.min(255, thumbAlpha + 0x0C));
        float radius = trackW * 0.5f;
        BetterChatRenderer.renderer.roundedRectGradientQuad(trackX, trackY, trackW, trackH, radius, 1.0f, trackA, trackB, trackB, trackA);
        BetterChatRenderer.renderer.roundedRectGradientQuad(trackX, thumbY, trackW, thumbH, radius, 1.0f, handleA, handleB, handleB, handleA);
    }

    static void renderScrollbar(float boxX, float boxY, float boxW, float boxH,
                                int startLine, int visibleLines, int totalLines, double mouseX, double mouseY) {
        if (visibleLines <= 0 || totalLines <= visibleLines) {
            BetterChatRenderer.sbEnabled = false;
            BetterChatRenderer.draggingScrollbar = false;
            return;
        }
        int scrollable = Math.max(1, totalLines - visibleLines);
        boolean hot = BetterChatRenderer.draggingScrollbar || isScrollbarHovered(mouseX, mouseY);
        boolean recently = System.currentTimeMillis() - BetterChatRenderer.lastScrollInteractionMs < 850L;
        float trackW = 5.0f;
        float trackX = boxX - trackW - 7f;
        float trackY = boxY + 10f;
        float trackH = Math.max(16f, boxH - 20f);
        float thumbH = Math.max(18f, trackH * ((float) visibleLines / (float) totalLines));
        float ratio = (float) startLine / (float) scrollable;
        float thumbY = trackY + (trackH - thumbH) * Mth.clamp(ratio, 0f, 1f);

        BetterChatRenderer.sbTrackX = trackX;
        BetterChatRenderer.sbTrackY = trackY;
        BetterChatRenderer.sbTrackW = trackW;
        BetterChatRenderer.sbTrackH = trackH;
        BetterChatRenderer.sbThumbY = thumbY;
        BetterChatRenderer.sbThumbH = thumbH;
        BetterChatRenderer.sbEnabled = true;

        if (hot || BetterChatRenderer.draggingScrollbar) {
            SystemCursor.set(SystemCursor.CursorType.SCROLL);
        }
        int trackAlpha = hot ? 0xB8 : (recently ? 0x9C : 0x84);
        int thumbAlpha = hot ? 0xFF : (recently ? 0xEC : 0xD4);
        drawScrollbarGlass(trackX, trackY, trackW, trackH, thumbY, thumbH, hot, trackAlpha, thumbAlpha);
    }

    static boolean isScrollbarHovered(double mx, double my) {
        float hitPad = 5f;
        return BetterChatRenderer.sbEnabled &&
                mx >= BetterChatRenderer.sbTrackX - hitPad &&
                mx <= BetterChatRenderer.sbTrackX + BetterChatRenderer.sbTrackW + hitPad &&
                my >= BetterChatRenderer.sbTrackY - hitPad &&
                my <= BetterChatRenderer.sbTrackY + BetterChatRenderer.sbTrackH + hitPad;
    }

    static void scrollToPosition(double my) {
        if (!BetterChatRenderer.sbEnabled || BetterChatRenderer.sbVisibleLines <= 0 || BetterChatRenderer.sbTotalLines <= BetterChatRenderer.sbVisibleLines)
            return;
        float trackSpan = BetterChatRenderer.sbTrackH - BetterChatRenderer.sbThumbH;
        if (trackSpan <= 0f) return;
        float rel = (float) (my - BetterChatRenderer.sbTrackY - BetterChatRenderer.sbDragOffset);
        float t = Mth.clamp(rel / trackSpan, 0f, 1f);
        int scrollable = Math.max(0, BetterChatRenderer.sbTotalLines - BetterChatRenderer.sbVisibleLines);
        int targetStart = Math.round(t * scrollable);
        BetterChatRenderer.scrollOffsetLines = Mth.clamp(BetterChatRenderer.sbTotalLines - BetterChatRenderer.sbVisibleLines - targetStart, 0, scrollable);
        BetterChatRenderer.lastScrollInteractionMs = System.currentTimeMillis();
    }
}