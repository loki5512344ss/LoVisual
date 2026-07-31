/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.command;

import dev.loki.lovisual.render.engine.renderer.ui.draw.UiEffectSpec;

public record UiEffectRegionCommand(UiEffectSpec effect) implements UiCommand {
    @Override
    public UiCommandKind kind() {
        return UiCommandKind.EFFECT_REGION;
    }
}
