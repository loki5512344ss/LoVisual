/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier;

import dev.loki.lovisual.features.gui.hud.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model.ToastModel.PreviewToast;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model.ToastModel.Toast;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model.ToastModel.ToastDraw;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model.ToastModel.ToastVisual;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import java.util.List;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.HudNotifier;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model.ToastModel;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model.ToastStyle;

/**
 * Layout and measurement math for HudNotifier toasts: box metrics, text measurement,
 * slide-in offsets and the per-frame blur backdrop pass over the toast draw list.
 */
public final class ToastGeometry {

    public static final float BASE_RADIUS = 4f;
    public static final float BASE_BOX_HEIGHT = 16f;
    public static final float BASE_LEFT_PAD = 7f;
    public static final float BASE_RIGHT_PAD = 9f;
    public static final float BASE_ICON_PIPE_GAP = 5f;
    public static final float BASE_TOGGLE_PIPE_GAP = 4.5f;
    public static final float BASE_PIPE_TEXT_GAP = 6f;
    public static final float BASE_TOGGLE_W = 20f;
    public static final float BASE_TOGGLE_H = 12f;
    public static final float BASE_TOAST_SLIDE = 7.0f;
    public static final float GLASS_BLUR_ALPHA = 255f;

    private ToastGeometry() {
    }

    public static ToastPosition toastSlideOffset(MessengerSide side, float anim, float baseScale) {
        float offset = (1.0f - ToastModel.clamp01(anim)) * BASE_TOAST_SLIDE * baseScale;
        return switch (side) {
            case LEFT -> new ToastPosition(-offset, 0f);
            case TOP -> new ToastPosition(0f, -offset);
            case BOTTOM -> new ToastPosition(0f, offset);
            default -> new ToastPosition(offset, 0f);
        };
    }

    public static ToastMetrics measureToast(Toast toast,
                                             TextRenderer lineRenderer,
                                             TextRenderer iconRenderer,
                                             float lineScale,
                                             float iconScale,
                                             float baseScale) {
        boolean moduleToggle = toast.visual == ToastVisual.MODULE_TOGGLE;
        String icon = ToastStyle.iconFor(toast.type);
        float iconW = moduleToggle ? BASE_TOGGLE_W * baseScale : measureWidth(iconRenderer, icon, iconScale);
        float iconH = moduleToggle ? BASE_TOGGLE_H * baseScale : measureHeight(iconRenderer, iconScale);
        float pipeW = measureWidth(lineRenderer, "|", lineScale);
        float textW = measureWidth(lineRenderer, toast.line, lineScale);
        if (ToastModel.PREVIEW_KEY.equals(toast.key)) {
            textW = Math.max(textW, previewMaxTextWidth(lineRenderer, lineScale));
        }
        float textH = measureHeight(lineRenderer, lineScale);

        float leftPad = BASE_LEFT_PAD * baseScale;
        float rightPad = BASE_RIGHT_PAD * baseScale;
        float iconPipeGap = (moduleToggle ? BASE_TOGGLE_PIPE_GAP : BASE_ICON_PIPE_GAP) * baseScale;
        float pipeTextGap = BASE_PIPE_TEXT_GAP * baseScale;

        float boxW = leftPad + iconW + iconPipeGap + pipeW + pipeTextGap + textW + rightPad;
        float boxH = Math.max(BASE_BOX_HEIGHT * baseScale, Math.max(textH, iconH) + (6.0f * baseScale));
        float radius = boxH * 0.5f;
        float iconX = leftPad;
        float iconY = (boxH - iconH) * 0.5f;
        float pipeX = iconX + iconW + iconPipeGap;
        float textX = pipeX + pipeW + pipeTextGap;
        float textY = (boxH - textH) * 0.5f + (0.8f * baseScale);

        return new ToastMetrics(boxW, boxH, radius, textScale(lineScale), iconScale, iconX, iconY, iconW, iconH, pipeX, textX, textY);
    }

    public static float previewMaxTextWidth(TextRenderer renderer, float scale) {
        float max = 0f;
        for (PreviewToast preview : ToastModel.PREVIEW_EXAMPLES) {
            max = Math.max(max, measureWidth(renderer, preview.line(), scale));
        }
        return max;
    }

    public static float textScale(float value) {
        return value;
    }

    public static float measureWidth(TextRenderer renderer, String text, float scale) {
        if (renderer == null || text == null || text.isEmpty()) return 0f;
        renderer.begin(scale, true, false);
        float width = (float) renderer.getWidth(text, false);
        renderer.end();
        return width;
    }

    public static float measureHeight(TextRenderer renderer, float scale) {
        if (renderer == null) return 0f;
        renderer.begin(scale, true, false);
        float height = (float) renderer.getHeight(false);
        renderer.end();
        return height;
    }

    public static void drawGlassBlurBatch(List<ToastDraw> draws, float baseScale) {
        if (draws == null || draws.isEmpty()) return;
        HudGlobalConfig hud = HudGlobalConfig.get();
        if (hud == null) return;

        Renderer2D.COLOR.blurComposite(composite -> {
            for (ToastDraw draw : draws) {
                if (draw == null || draw.metrics == null || draw.anim <= 0.001f) continue;
                float a = ToastModel.clamp01(draw.anim);
                float sideBottomR = Math.max(1f, draw.metrics.boxH * 0.5f);
                composite.roundedRectCorners(
                        draw.x,
                        draw.y,
                        draw.metrics.boxW,
                        draw.metrics.boxH,
                        0f,
                        0f,
                        sideBottomR,
                        sideBottomR,
                        hud.getBlurRadius(),
                        1.0f,
                        (GLASS_BLUR_ALPHA / 255.0f) * a,
                        0xFFFFFF
                );
            }
        });
    }

    public enum MessengerSide {
        RIGHT,
        LEFT,
        TOP,
        BOTTOM;

        public static MessengerSide from(String name) {
            if (name == null) return RIGHT;
            return switch (name.toLowerCase()) {
                case "left" -> LEFT;
                case "top" -> TOP;
                case "bottom" -> BOTTOM;
                default -> RIGHT;
            };
        }
    }

    public record ToastPosition(float x, float y) {
    }

    public static final class ToastMetrics {
        public final float boxW;
        public final float boxH;
        public final float radius;
        public final float textScale;
        public final float iconScale;
        public final float iconX;
        public final float iconY;
        public final float iconW;
        public final float iconH;
        public final float pipeX;
        public final float textX;
        public final float textY;

        public ToastMetrics(float boxW,
                            float boxH,
                            float radius,
                            float textScale,
                            float iconScale,
                            float iconX,
                            float iconY,
                            float iconW,
                            float iconH,
                            float pipeX,
                            float textX,
                            float textY) {
            this.boxW = boxW;
            this.boxH = boxH;
            this.radius = radius;
            this.textScale = textScale;
            this.iconScale = iconScale;
            this.iconX = iconX;
            this.iconY = iconY;
            this.iconW = iconW;
            this.iconH = iconH;
            this.pipeX = pipeX;
            this.textX = textX;
            this.textY = textY;
        }
    }
}
