/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.command;

import dev.loki.lovisual.render.engine.renderer.ui.draw.UiPaint;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiShape;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiStroke;

public record UiPathCommand(UiShape path, UiPaint paint, UiStroke stroke, boolean fill) implements UiCommand {
    @Override
    public UiCommandKind kind() {
        return UiCommandKind.PATH;
    }
}
