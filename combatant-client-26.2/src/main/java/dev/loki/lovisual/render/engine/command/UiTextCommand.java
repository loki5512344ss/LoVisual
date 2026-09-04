/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.command;

public record UiTextCommand(String text, float x, float y, int argb, float size) implements UiCommand {
    @Override
    public UiCommandKind kind() {
        return UiCommandKind.TEXT;
    }
}
