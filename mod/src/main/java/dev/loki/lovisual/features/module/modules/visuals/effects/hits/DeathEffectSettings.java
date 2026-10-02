package dev.loki.lovisual.features.module.modules.visuals.effects.hits;

import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.modules.visuals.effects.hits.death.DeathEffectHelper;

/** Настройки режимов смерти KillEffect (Shatter, Ascend, Explosion, Disintegrate); вынесены из модуля ради лимита строк. */
abstract class DeathEffectSettings extends Module {

    protected abstract String currentMode();

    protected final boolean isMode(String value) {
        return value.equals(currentMode());
    }

    protected final boolean isDeathMode() {
        return DeathEffectHelper.handles(currentMode());
    }

    protected final NumberValue<Float> deathPower = visibleWhen(
            num("killEffectDeathPower", "death_power", 1.0f, 0.3f, 3.0f), this::isDeathMode);
    protected final NumberValue<Float> deathDuration = visibleWhen(
            num("killEffectDeathDuration", "death_duration", 2.5f, 1.0f, 6.0f), this::isDeathMode);
    protected final NumberValue<Float> deathSpin = visibleWhen(
            num("killEffectDeathSpin", "death_spin", 1.0f, 0.2f, 3.0f), () -> isMode("Shatter") || isMode("Ascend"));
    protected final BooleanValue deathBounce = visibleWhen(
            bool("killEffectDeathBounce", "death_bounce", true), () -> isMode("Shatter"));
    protected final NumberValue<Integer> deathSparks = visibleWhen(
            num("killEffectDeathSparks", "death_sparks", 70, 10, 150), () -> isMode("Explosion"));
    protected final ModeValue blastColorMode = visibleWhen(
            modeSetting("killEffectBlastColor", "blast_color", "Natural", "Natural", "Client", "Custom"),
            () -> isMode("Explosion"));

    protected final boolean customBlastColor() {
        return isMode("Explosion") && "Custom".equals(blastColorMode.get());
    }
}
