package dev.loki.lovisual.render.engine.command.effect;

import dev.loki.lovisual.render.engine.renderer.ui.draw.effect.UiEffectSpec;
import dev.loki.lovisual.render.engine.command.core.UiCommand;
import dev.loki.lovisual.render.engine.command.core.UiCommandKind;

public record UiEffectRegionCommand(UiEffectSpec effect) implements UiCommand {
    @Override
    public UiCommandKind kind() {
        return UiCommandKind.EFFECT_REGION;
    }
}
