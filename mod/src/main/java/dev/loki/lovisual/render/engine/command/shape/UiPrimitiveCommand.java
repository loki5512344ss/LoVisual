package dev.loki.lovisual.render.engine.command.shape;
import dev.loki.lovisual.render.engine.command.core.UiCommand;
import dev.loki.lovisual.render.engine.command.core.UiCommandKind;

public record UiPrimitiveCommand(float x, float y, float width, float height, int argb) implements UiCommand {
    @Override
    public UiCommandKind kind() {
        return UiCommandKind.PRIMITIVE;
    }
}
