/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.api.v0.clickgui;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.TextRenderer;

public record LoVisualClickGuiRenderContext(
        Renderer2D renderer,
        TextRenderer regularFont,
        TextRenderer mediumFont
) {
}
