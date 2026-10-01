package dev.loki.lovisual.features.module.modules.combat.targetesp.state;

import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.relations.category.CategoryService;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Resolves TargetESP colors from the module's color settings, optionally mixed with relation colors. */
public final class TargetEspColors {
    private final ModeValue mode;
    private final NumberValue<Integer> speed;
    private final NumberValue<Integer> phase;
    private final NumberValue<Integer> spread;
    private final RGBAColorValue primary;
    private final RGBAColorValue secondary;
    private final BooleanValue useRelations;
    private final NumberValue<Float> relationMix;

    public TargetEspColors(ModeValue mode, NumberValue<Integer> speed, NumberValue<Integer> phase,
                           NumberValue<Integer> spread, RGBAColorValue primary, RGBAColorValue secondary,
                           BooleanValue useRelations, NumberValue<Float> relationMix) {
        this.mode = mode;
        this.speed = speed;
        this.phase = phase;
        this.spread = spread;
        this.primary = primary;
        this.secondary = secondary;
        this.useRelations = useRelations;
        this.relationMix = relationMix;
    }

    public boolean usesSecondary() {
        return AnimatedRenderColors.usesSecondary(animatedMode());
    }

    /** @param target entity whose relation color is mixed in, or null to skip relations */
    public int argb(int count, LivingEntity target) {
        int shifted = phase.get() + Math.round(count * spread.get() / 90.0f);
        int animated = AnimatedRenderColors.resolve(
                animatedMode(), speed.get(), shifted, primary.getArgb(), secondary.getArgb(), true);
        if (!useRelations.get() || target == null) {
            return animated;
        }
        int relation = AnimatedRenderColors.withAlpha(relationColor(target, animated), (animated >>> 24) & 0xFF);
        return AnimatedRenderColors.mixArgb(animated, relation, relationMix.get());
    }

    private int relationColor(LivingEntity entity, int fallback) {
        if (entity instanceof Player player) {
            return CategoryService.getColor(player);
        }
        String name = entity.getName() != null ? entity.getName().getString() : null;
        if (name == null || name.isBlank()) {
            return fallback;
        }
        return CategoryService.getColor(name);
    }

    private AnimatedRenderColors.Mode animatedMode() {
        return switch (mode.get()) {
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
