package dev.loki.lovisual.render.engine.command.texture;
import dev.loki.lovisual.render.engine.command.core.UiCommand;
import dev.loki.lovisual.render.engine.command.core.UiCommandKind;

public record UiTextCommand(String text, float x, float y, int argb, float size) implements UiCommand {
    @Override
    public UiCommandKind kind() {
        return UiCommandKind.TEXT;
    }
}
