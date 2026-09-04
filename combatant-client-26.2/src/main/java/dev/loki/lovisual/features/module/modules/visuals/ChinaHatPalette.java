/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.features.relations.CategoryService;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Color resolution helpers for ChinaHat. Shared color-math and per-vertex shading,
 * mirroring the TazikHat palette pipeline but reusable by the facade module.
 */
final class ChinaHatPalette {
    private ChinaHatPalette() {
    }

    private static final double RAINBOW_SPATIAL_SCALE = 12.0;

    record Palette(int primary, int secondary, int rim) {
    }

    static Palette resolve(Player player,
                           boolean useRelations,
                           int primaryArgb,
                           int secondaryArgb,
                           int rimArgb) {
        if (useRelations) {
            int relation = CategoryService.getColor(player);
            int primary = withAlpha(relation, alphaOr(primaryArgb, 255));
            int secondary = withAlpha(
                    AnimatedRenderColors.analogousColor(relation),
                    alphaOr(secondaryArgb, 170)
            );
            int rim = withAlpha(brighten(relation, 1.35f), alphaOr(rimArgb, 238));

            return new Palette(primary, secondary, rim);
        }

        return new Palette(primaryArgb, secondaryArgb, rimArgb);
    }

    static int colorAt(Palette palette,
                       AnimatedRenderColors.Mode mode,
                       int speed,
                       int index,
                       float verticalLight,
                       float alphaMul) {
        int dynamic = getDynamicColor(palette.primary, palette.secondary, mode, speed, index);

        float circumference = 0.5f + 0.5f * Mth.sin(index * 0.47f + GameClock.millis() * 0.0007f);
        int mixed = multiplyRgb(dynamic, verticalLight + circumference * 0.16f);

        return withAlpha(mixed, Math.round(alpha(mixed) * alphaMul));
    }

    static int rimColorAt(Palette palette,
                          AnimatedRenderColors.Mode mode,
                          int speed,
                          int index,
                          float alphaMul) {
        int dynamic = getDynamicColor(palette.rim, palette.secondary, mode, speed, index + 120);

        float t = 0.5f + 0.5f * Mth.sin(index * 0.71f + GameClock.millis() * 0.0011f);
        int mixed = mix(dynamic, brighten(palette.primary, 1.35f), t);

        return withAlpha(mixed, Math.round(alpha(mixed) * alphaMul));
    }

    static int getDynamicColor(int primary,
                               int secondary,
                               AnimatedRenderColors.Mode mode,
                               int speed,
                               int index) {
        int spatialIndex = Math.floorMod((int) Math.round(index * RAINBOW_SPATIAL_SCALE), 3600);
        return AnimatedRenderColors.resolve(mode, speed, spatialIndex, primary, secondary, true);
    }

    static int mix(int a, int b, float t) {
        float clamped = AnimationUtility.clamp01(t);

        int aa = alpha(a);
        int ar = red(a);
        int ag = green(a);
        int ab = blue(a);

        int ba = alpha(b);
        int br = red(b);
        int bg = green(b);
        int bb = blue(b);

        int ca = Math.round(AnimationUtility.lerp(aa, ba, clamped));
        int cr = Math.round(AnimationUtility.lerp(ar, br, clamped));
        int cg = Math.round(AnimationUtility.lerp(ag, bg, clamped));
        int cb = Math.round(AnimationUtility.lerp(ab, bb, clamped));

        return (ca << 24) | (cr << 16) | (cg << 8) | cb;
    }

    static int multiplyRgb(int argb, float factor) {
        int a = alpha(argb);
        int r = Mth.clamp(Math.round(red(argb) * factor), 0, 255);
        int g = Mth.clamp(Math.round(green(argb) * factor), 0, 255);
        int b = Mth.clamp(Math.round(blue(argb) * factor), 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    static int brighten(int argb, float factor) {
        return multiplyRgb(argb, factor);
    }

    static int withAlpha(int argb, int alpha) {
        int a = Mth.clamp(alpha, 0, 255);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    static int alphaOr(int argb, int fallback) {
        int alpha = alpha(argb);
        return alpha <= 0 ? Mth.clamp(fallback, 0, 255) : alpha;
    }

    static int red(int argb) {
        return (argb >>> 16) & 255;
    }

    static int green(int argb) {
        return (argb >>> 8) & 255;
    }

    static int blue(int argb) {
        return argb & 255;
    }

    static int alpha(int argb) {
        return (argb >>> 24) & 255;
    }
}
