/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model;

import dev.loki.lovisual.features.gui.hud.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.HudNotifier;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.HudNotifier.NotifyType;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model.ToastModel.Toast;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.SvgRenderOptions;

/**
 * Color and icon styling for HudNotifier toasts: palettes, notification type colors,
 * the small ARGB helpers shared by the toast drawing routines and the module-toggle
 * switch artwork.
 */
public final class ToastStyle {

    public static final String ICON_NO = "I";
    public static final String ICON_INFO = "J";
    public static final String ICON_YES = "K";
    public static final String ICON_WARN = "L";

    private ToastStyle() {
    }

    public static ToastPalette paletteFor() {
        int progressTrack = 0x33FFFFFF;
        int progressFill = 0xC4FFFFFF;
        int bg = 0x52000000;
        return new ToastPalette(
                bg,
                bg,
                0xFFF7FAFF,
                progressTrack,
                progressFill,
                0xFFFFFFFF
        );
    }

    public static NotifyType resolveType(String text, Boolean enabled) {
        if (enabled != null) return enabled ? NotifyType.YES : NotifyType.NO;
        String lower = text.toLowerCase();
        if (lower.contains("warn") || lower.contains("warning") || lower.contains("cooldown")) {
            return NotifyType.WARN;
        }
        if (lower.contains("error") || lower.contains("failed")
                || lower.contains("fail") || lower.contains("denied")
                || lower.contains("blocked") || lower.contains("disabled")) {
            return NotifyType.NO;
        }
        return NotifyType.INFO;
    }

    public static String iconFor(NotifyType type) {
        return switch (type) {
            case YES -> ICON_YES;
            case NO -> ICON_NO;
            case WARN -> ICON_WARN;
            case INFO -> ICON_INFO;
        };
    }

    public static int colorFor(NotifyType type) {
        return switch (type) {
            case YES -> 0xFF6DFF56;
            case NO -> 0xFFFF5656;
            case WARN -> 0xFFF5FF5E;
            case INFO -> 0xFF4AA3FF;
        };
    }

    public static int moduleToggleColor(float progress, float anim) {
        Themes.Theme theme = Theme.theme();
        int off = theme != null ? HudRenderUtil.mixColor(theme.textMuted(), theme.textPrimary(), 0.18f) : 0xFFC6D2CD;
        int on = theme != null ? theme.accent() : 0xFF5CC8E7;
        int color = HudRenderUtil.mixColor(off, on, AnimationUtility.clamp01(progress));
        return applyAlpha(color, anim);
    }

    public static String resolveLine(String text, Boolean enabled) {
        if (enabled != null) {
            return text + " - " + (enabled ? "enabled!" : "disabled!");
        }
        return text;
    }

    public static int applyAlpha(int argb, float alpha) {
        int a = (int) (((argb >>> 24) & 0xFF) * alpha);
        return (argb & 0x00FFFFFF) | ((a & 0xFF) << 24);
    }

    public static int scaleAlpha(int argb, float factor) {
        factor = AnimationUtility.clamp01(factor);
        int a = (argb >>> 24) & 0xFF;
        int scaledA = Math.round(a * factor);
        return (argb & 0x00FFFFFF) | ((scaledA & 0xFF) << 24);
    }

    public static int darken(int argb, float factor) {
        factor = AnimationUtility.clamp(factor, 0f, 1f);
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;

        r = Math.round(r * factor);
        g = Math.round(g * factor);
        b = Math.round(b * factor);

        return ((a & 0xFF) << 24)
                | ((r & 0xFF) << 16)
                | ((g & 0xFF) << 8)
                | (b & 0xFF);
    }

    public static void applyColor(RenderColor out, int argb) {
        out.a = (argb >>> 24) & 0xFF;
        out.r = (argb >>> 16) & 0xFF;
        out.g = (argb >>> 8) & 0xFF;
        out.b = argb & 0xFF;
    }

    public static void drawModuleToggle(Renderer2D renderer,
                                  Toast toast,
                                  float x,
                                  float y,
                                  float w,
                                  float h,
                                  int fallbackAccent,
                                  float anim,
                                  long now) {
        if (renderer == null || w <= 0.1f || h <= 0.1f) return;

        float progress = toast.toggleProgress(now);
        int tint = moduleToggleColor(progress, anim);
        if (((tint >>> 24) & 0xFF) <= 0) {
            tint = fallbackAccent;
        }

        float svgY = y + (h - w) * 0.5f;
        SvgRenderOptions options = SvgRenderOptions.overrideColor(tint);
        if (progress <= 0.001f) {
            renderer.svg("toggle-left", x, svgY, w, w, options);
            return;
        }
        if (progress >= 0.999f) {
            renderer.svg("toggle-right", x, svgY, w, w, options);
            return;
        }

        renderer.svg("toggle-track", x, svgY, w, w, options);

        float thumbTravel = w * (3f / 24f);
        float thumbX = x + AnimationUtility.lerp(-thumbTravel, thumbTravel, progress);
        renderer.svg("toggle-thumb", thumbX, svgY, w, w, options);
    }

    public static final class ToastPalette {
        public final int bgLeft;
        public final int bgRight;
        public final int text;
        public final int progressTrack;
        public final int progressFill;
        public final int glassTint;

        public ToastPalette(int bgLeft,
                            int bgRight,
                            int text,
                            int progressTrack,
                            int progressFill,
                            int glassTint) {
            this.bgLeft = bgLeft;
            this.bgRight = bgRight;
            this.text = text;
            this.progressTrack = progressTrack;
            this.progressFill = progressFill;
            this.glassTint = glassTint;
        }
    }
}
