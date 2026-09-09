/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier;

import dev.loki.lovisual.features.gui.hud.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.ToastGeometry.ToastMetrics;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model.ToastModel.Toast;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model.ToastModel.ToastVisual;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model.ToastStyle.ToastPalette;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.HudNotifier;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model.ToastModel;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model.ToastStyle;

/**
 * Card drawing routines for HudNotifier toasts: glass pill background, progress bar
 * and the icon/pipe/text layout of a single toast card.
 */
public final class ToastCardRenderer {

    public static final float BASE_PROGRESS_H = 1.35f;
    public static final float GLASS_PASS_ALPHA = 255f;
    public static final float TEXT_ALPHA_FACTOR = 0.88f;
    public static final float PIPE_ALPHA_FACTOR = 0.72f;
    public static final float TEXT_DARKEN_MIN = 0.78f;
    public static final float TEXT_DARKEN_MAX = 1.00f;
    public static final float TEXT_ANIM_SPEED = 2.2f;

    private ToastCardRenderer() {
    }

    public static void drawToastCard(Renderer2D renderer,
                               RenderColor colorTmp,
                               Toast toast,
                               TextRenderer lineRenderer,
                               TextRenderer iconRenderer,
                               float x,
                               float y,
                               ToastMetrics metrics,
                               float remaining,
                               float anim,
                               float baseScale) {
        ToastPalette palette = ToastStyle.paletteFor();

        int glassTint = ToastStyle.applyAlpha(palette.glassTint, anim);
        int bgLeft = ToastStyle.applyAlpha(palette.bgLeft, anim);
        int bgRight = ToastStyle.applyAlpha(palette.bgRight, anim);
        long now = System.currentTimeMillis();
        float wave = 0.5f + 0.5f * (float) Math.sin(now / 1000.0f * TEXT_ANIM_SPEED);
        float darkenFactor = AnimationUtility.lerp(TEXT_DARKEN_MIN, TEXT_DARKEN_MAX, wave);

        int text = ToastStyle.scaleAlpha(ToastStyle.applyAlpha(palette.text, anim), TEXT_ALPHA_FACTOR);
        text = ToastStyle.darken(text, darkenFactor);

        int progressTrack = ToastStyle.applyAlpha(palette.progressTrack, anim);
        int progress = ToastStyle.applyAlpha(palette.progressFill, anim);
        int accent = ToastStyle.applyAlpha(ToastStyle.colorFor(toast.type), anim);

        int pipe = ToastStyle.scaleAlpha(ToastStyle.applyAlpha(HudRenderUtil.mixColor(palette.text, palette.progressTrack, 0.55f), anim), PIPE_ALPHA_FACTOR);
        pipe = ToastStyle.darken(pipe, darkenFactor * 0.96f);
        float sideBottomR = Math.max(1f, metrics.boxH * 0.5f);
        float sideTopR = 0f;

        drawGlassPill(
                renderer,
                x,
                y,
                metrics.boxW,
                metrics.boxH,
                sideTopR,
                sideTopR,
                sideBottomR,
                sideBottomR,
                glassTint,
                anim
        );

        int bgMixed = HudRenderUtil.mixColor(bgLeft, bgRight, 0.5f);
        int bgA = (bgMixed >>> 24) & 0xFF;
        int bgSolid = HudRenderUtil.glassBackground(anim);
        renderer.roundedRectCornersQuad(
                x,
                y,
                metrics.boxW,
                metrics.boxH,
                sideTopR,
                sideTopR,
                sideBottomR,
                sideBottomR,
                1.0f,
                bgSolid,
                bgSolid,
                bgSolid,
                bgSolid
        );

        float progressInsetX = 0.45f * baseScale;
        float progressInsetY = 0.08f * baseScale;
        float progressH = Math.max(1f, BASE_PROGRESS_H * baseScale);
        float progressY = y + progressInsetY;
        float progressX = x + progressInsetX;
        float progressAreaW = Math.max(0f, metrics.boxW - progressInsetX * 2f);
        float progressW = Math.max(0f, progressAreaW * ToastModel.clamp01(remaining));
        float progressTopR = 0f;
        float progressBottomR = 0.02f * baseScale;
        renderer.roundedRectCornersQuad(
                progressX,
                progressY,
                progressAreaW,
                progressH,
                progressTopR,
                progressTopR,
                progressBottomR,
                progressBottomR,
                1.0f,
                progressTrack,
                progressTrack,
                progressTrack,
                progressTrack
        );
        if (progressW > 0f) {
            renderer.roundedRectCornersQuad(
                    progressX,
                    progressY,
                    progressW,
                    progressH,
                    progressTopR,
                    progressTopR,
                    progressBottomR,
                    progressBottomR,
                    1.0f,
                    progress,
                    progress,
                    progress,
                    progress
            );
        }

        if (toast.visual == ToastVisual.MODULE_TOGGLE) {
            ToastStyle.drawModuleToggle(renderer, toast, x + metrics.iconX, y + metrics.iconY, metrics.iconW, metrics.iconH, accent, anim, now);
        } else {
            iconRenderer.begin(metrics.iconScale, false, false);
            ToastStyle.applyColor(colorTmp, accent);
            iconRenderer.render(ToastStyle.iconFor(toast.type), x + metrics.iconX, y + metrics.iconY, colorTmp, false);
            iconRenderer.end();
        }

        lineRenderer.begin(metrics.textScale, false, false);
        ToastStyle.applyColor(colorTmp, pipe);
        lineRenderer.render("|", x + metrics.pipeX, y + metrics.textY, colorTmp, false);
        ToastStyle.applyColor(colorTmp, text);
        lineRenderer.render(toast.line, x + metrics.textX, y + metrics.textY, colorTmp, false);
        lineRenderer.end();
    }

    public static void drawGlassPill(Renderer2D renderer,
                               float x,
                               float y,
                               float w,
                               float h,
                               float radiusTL,
                               float radiusTR,
                               float radiusBR,
                               float radiusBL,
                               int tintArgb,
                               float anim) {
        if (renderer == null) return;
        float a = ToastModel.clamp01(anim);
        if (a <= 0.001f) return;

        float glassAlpha = (GLASS_PASS_ALPHA / 255.0f) * a;
        float glassScale = Math.max(0.50f, Math.min(1.25f, h / 24.0f));
        HudRenderUtil.drawLiquidGlassCorners(
                x,
                y,
                w,
                h,
                radiusTL,
                radiusTR,
                radiusBR,
                radiusBL,
                glassScale,
                false,
                glassAlpha,
                tintArgb
        );
    }
}
