/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

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
