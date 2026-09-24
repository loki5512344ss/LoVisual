package dev.loki.lovisual.render.engine.command.shape;

import dev.loki.lovisual.render.engine.renderer.ui.draw.paint.UiPaint;
import dev.loki.lovisual.render.engine.renderer.ui.draw.shape.UiShape;
import dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiStroke;
import dev.loki.lovisual.render.engine.command.core.UiCommand;
import dev.loki.lovisual.render.engine.command.core.UiCommandKind;

public record UiShapeCommand(UiShape shape, UiPaint paint, UiStroke stroke, boolean fill) implements UiCommand {
    @Override
    public UiCommandKind kind() {
        return UiCommandKind.SHAPE;
    }
}
