package dev.loki.lovisual.features.module.modules.visuals.effects.ambient;

import net.minecraft.client.Minecraft;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.helpers.player.TrailPoint;

final class TrailsColor {
    boolean usesSecondary(String mode) {
        return AnimatedRenderColors.usesSecondary(parse(mode));
    }

    int getColorArgb(String mode, int speed, int phase, int primary, int secondary) {
        return AnimatedRenderColors.resolve(parse(mode), speed, phase, primary, secondary, true);
    }

    int currentColorPhase(float tickDelta, int phaseSetting) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return phaseSetting;
        return Math.round((mc.player.tickCount + tickDelta) * 4.0f) + phaseSetting;
    }

    int trailPointColor(TrailPoint point, int index, int size, float tickDelta,
                        int phaseBase, String mode, int speed, int primary, int secondary,
                        int lengthSpread, int ageSpread) {
        float pos = size <= 1 ? 0.0f : index / (float) (size - 1);
        float age = 1.0f - point.animation(tickDelta);
        int phase = phaseBase + Math.round(pos * lengthSpread) + Math.round(age * ageSpread);
        int animated = getColorArgb(mode, speed, phase, primary, secondary);
        if (parse(mode) == AnimatedRenderColors.Mode.STATIC) animated = point.color();
        float lift = (float) Math.pow(1.0f - pos, 2.0f) * 0.45f;
        return AnimatedRenderColors.mixArgb(animated, (animated & 0xFF000000) | 0x00FFFFFF, lift);
    }

    AnimatedRenderColors.Mode parse(String mode) {
        return switch (mode) {
            case "Rainbow" -> AnimatedRenderColors.Mode.RAINBOW;
            case "LightRainbow" -> AnimatedRenderColors.Mode.LIGHT_RAINBOW;
            case "Sky" -> AnimatedRenderColors.Mode.SKY;
            case "Fade" -> AnimatedRenderColors.Mode.FADE;
            case "DoubleColor" -> AnimatedRenderColors.Mode.DOUBLE_COLOR;
            case "Analogous" -> AnimatedRenderColors.Mode.ANALOGOUS;
            case "Theme" -> AnimatedRenderColors.Mode.THEME;
            default -> AnimatedRenderColors.Mode.STATIC;
        };
    }
}
