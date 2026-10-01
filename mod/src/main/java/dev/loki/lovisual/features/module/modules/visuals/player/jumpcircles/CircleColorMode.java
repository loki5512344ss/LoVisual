package dev.loki.lovisual.features.module.modules.visuals.player.jumpcircles;

import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;

import java.util.List;

/** Colour source options for JumpCircles, mapped onto the shared animated render colours. */
public enum CircleColorMode implements EnumValue.AliasProvider {
    STATIC(AnimatedRenderColors.Mode.STATIC),
    RAINBOW(AnimatedRenderColors.Mode.RAINBOW),
    LIGHT_RAINBOW(AnimatedRenderColors.Mode.LIGHT_RAINBOW),
    SKY(AnimatedRenderColors.Mode.SKY),
    FADE(AnimatedRenderColors.Mode.FADE),
    DOUBLE_COLOR(AnimatedRenderColors.Mode.DOUBLE_COLOR),
    ANALOGOUS(AnimatedRenderColors.Mode.ANALOGOUS),
    THEME(AnimatedRenderColors.Mode.THEME);

    private final AnimatedRenderColors.Mode animated;

    CircleColorMode(AnimatedRenderColors.Mode animated) {
        this.animated = animated;
    }

    public AnimatedRenderColors.Mode animated() {
        return animated;
    }

    @Override
    public List<String> aliases() {
        return switch (this) {
            case LIGHT_RAINBOW -> List.of("LightRainbow");
            case DOUBLE_COLOR -> List.of("DoubleColor");
            case THEME -> List.of("Theme");
            default -> List.of();
        };
    }
}
