package dev.loki.lovisual.features.module.modules.visuals.scene;

import net.minecraft.util.Mth;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;

final class BlockHighlightColors {

    private static final double RAINBOW_SPATIAL_SCALE = 12.0;

    int outlineAt(double x, double y, double z,
                  BlockHighlight.ColorMode mode,
                  int speed,
                  int primaryArgb,
                  int secondaryArgb) {
        return AnimatedRenderColors.resolve(
                toAnimated(mode),
                speed,
                spatialIndex(x, y, z),
                primaryArgb,
                secondaryArgb,
                true
        );
    }

    int breakingArgb(int from, int to, float progress) {
        return interpolateArgb(from, to, progress);
    }

    boolean usesSecondary(BlockHighlight.ColorMode mode) {
        return AnimatedRenderColors.usesSecondary(toAnimated(mode));
    }

    AnimatedRenderColors.Mode toAnimated(BlockHighlight.ColorMode mode) {
        if (mode == null) return AnimatedRenderColors.Mode.STATIC;
        return switch (mode) {
            case RAINBOW -> AnimatedRenderColors.Mode.RAINBOW;
            case LIGHT_RAINBOW -> AnimatedRenderColors.Mode.LIGHT_RAINBOW;
            case SKY -> AnimatedRenderColors.Mode.SKY;
            case FADE -> AnimatedRenderColors.Mode.FADE;
            case DOUBLE_COLOR -> AnimatedRenderColors.Mode.DOUBLE_COLOR;
            case ANALOGOUS -> AnimatedRenderColors.Mode.ANALOGOUS;
            case THEME -> AnimatedRenderColors.Mode.THEME;
            case STATIC -> AnimatedRenderColors.Mode.STATIC;
        };
    }

    int spatialIndex(double x, double y, double z) {
        int xs = (int) Math.round(x * (RAINBOW_SPATIAL_SCALE + 1.0));
        int ys = (int) Math.round(y * (RAINBOW_SPATIAL_SCALE - 2.0));
        int zs = (int) Math.round(z * (RAINBOW_SPATIAL_SCALE + 3.0));
        return Math.floorMod(xs + ys + zs, 3600);
    }

    float stageToProgress(int stage) {
        if (stage < 0) return 0.0f;
        return Mth.clamp(stage / 10.0f, 0.0f, 1.0f);
    }

    int interpolateArgb(int from, int to, float amount) {
        amount = Mth.clamp(amount, 0.0f, 1.0f);
        int a = interpolateChannel(alpha(from), alpha(to), amount);
        int r = interpolateChannel(red(from), red(to), amount);
        int g = interpolateChannel(green(from), green(to), amount);
        int b = interpolateChannel(blue(from), blue(to), amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    int interpolateChannel(int from, int to, float amount) {
        return from + Math.round((to - from) * amount);
    }

    int red(int argb) {
        return (argb >>> 16) & 255;
    }

    int green(int argb) {
        return (argb >>> 8) & 255;
    }

    int blue(int argb) {
        return argb & 255;
    }

    int alpha(int argb) {
        return (argb >>> 24) & 255;
    }
}
