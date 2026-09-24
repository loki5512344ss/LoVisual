package dev.loki.lovisual.render.engine.command.effect;
import dev.loki.lovisual.render.engine.command.core.UiCommand;
import dev.loki.lovisual.render.engine.command.core.UiCommandKind;

public record UiEffectCommand(UiCommandKind kind, float x, float y, float width, float height,
                              float radius) implements UiCommand {
    public UiEffectCommand {
        if (kind != UiCommandKind.BLUR_REGION && kind != UiCommandKind.LIQUID_GLASS_REGION) {
            throw new IllegalArgumentException("UiEffectCommand kind must be a UI effect region");
        }
    }
}
