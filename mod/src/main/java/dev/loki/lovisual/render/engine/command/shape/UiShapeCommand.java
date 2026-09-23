/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

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
