package dev.loki.lovisual.features.module.modules.visuals.hats.tazik;

import dev.loki.lovisual.features.relations.category.CategoryService;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Colour math for the Tazik hat: palette resolution (relations vs. manual) plus
 * per-vertex shading (bowl and rim). All state is passed in explicitly — the
 * facade reads its settings and hands the unwrapped values to these statics.
 */
public final class TazikHatPalette {

    private TazikHatPalette() {
    }

    /** Spatial frequency of the rainbow cycle around the bowl circumference. */
    private static final double RAINBOW_SPATIAL_SCALE = 12.0;

    /** Base ARGB triple used to shade every vertex of the hat. */
    public record Palette(int primary, int secondary, int rim) {
    }

    /** Relations-driven palette if {@code useRelations}; otherwise straight from settings. */
    public static Palette resolve(Player player,
                                  boolean useRelations,
                                  int primaryArgb,
                                  int secondaryArgb,
                                  int rimArgb) {
        if (useRelations) {
            int relation = CategoryService.getColor(player);
            int primary = withAlpha(relation, alphaOr(primaryArgb, 255));
            int secondary = withAlpha(AnimatedRenderColors.analogousColor(relation), alphaOr(secondaryArgb, 170));
            int rim = withAlpha(brighten(relation, 1.35f), alphaOr(rimArgb, 238));
            return new Palette(primary, secondary, rim);
        }
        return new Palette(primaryArgb, secondaryArgb, rimArgb);
    }

    /** Bowl vertex shade: animated base, then vertical + circumference light, then alpha. */
    public static int colorAt(Palette palette,
                              int index,
                              float verticalLight,
                              float alphaMul,
                              AnimatedRenderColors.Mode mode,
                              int speed) {
        int dynamic = getDynamicColor(palette.primary, palette.secondary, mode, speed, index);
        float circumference = 0.5f + 0.5f * Mth.sin(index * 0.47f + GameClock.millis() * 0.0007f);
        int mixed = multiplyRgb(dynamic, verticalLight + circumference * 0.16f);
        return withAlpha(mixed, Math.round(alpha(mixed) * alphaMul));
    }

    /** Rim vertex shade: animated base offset by 120°, then lerp toward a brightened primary. */
    public static int rimColorAt(Palette palette,
                                 int index,
                                 float alphaMul,
                                 AnimatedRenderColors.Mode mode,
                                 int speed) {
        int dynamic = getDynamicColor(palette.rim, palette.secondary, mode, speed, index + 120);
        float t = 0.5f + 0.5f * Mth.sin(index * 0.71f + GameClock.millis() * 0.0011f);
        int mixed = mix(dynamic, brighten(palette.primary, 1.35f), t);
        return withAlpha(mixed, Math.round(alpha(mixed) * alphaMul));
    }

    /** Maps a vertex index around the bowl to a stable 0..3600 angle for AnimatedRenderColors. */
    private static int getDynamicColor(int primary,
                                       int secondary,
                                       AnimatedRenderColors.Mode mode,
                                       int speed,
                                       int index) {
        int spatialIndex = Math.floorMod((int) Math.round(index * RAINBOW_SPATIAL_SCALE), 3600);
        return AnimatedRenderColors.resolve(mode, speed, spatialIndex, primary, secondary, true);
    }

    /** ARGB-safe linear blend of two colours, alpha included. */
    static int mix(int a, int b, float t) {
        float clamped = AnimationUtility.clamp01(t);
        int ca = Math.round(AnimationUtility.lerp(alpha(a), alpha(b), clamped));
        int cr = Math.round(AnimationUtility.lerp(red(a), red(b), clamped));
        int cg = Math.round(AnimationUtility.lerp(green(a), green(b), clamped));
        int cb = Math.round(AnimationUtility.lerp(blue(a), blue(b), clamped));
        return (ca << 24) | (cr << 16) | (cg << 8) | cb;
    }

    /** Scale RGB channels by {@code factor}, keeping alpha intact. */
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

    /** Fall back to {@code fallback} when the source colour is fully transparent. */
    static int alphaOr(int argb, int fallback) {
        int a = alpha(argb);
        return a <= 0 ? Mth.clamp(fallback, 0, 255) : a;
    }
}
