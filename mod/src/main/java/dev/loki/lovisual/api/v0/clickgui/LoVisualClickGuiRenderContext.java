package dev.loki.lovisual.api.v0.clickgui;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

public record LoVisualClickGuiRenderContext(
        Renderer2D renderer,
        TextRenderer regularFont,
        TextRenderer mediumFont
) {
}
