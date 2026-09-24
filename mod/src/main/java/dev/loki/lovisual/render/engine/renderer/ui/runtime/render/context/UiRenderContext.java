package dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiProjectionMode;

public record UiRenderContext(Renderer2D renderer,
                              TextRenderer textRenderer,
                              GuiGraphicsExtractor drawContext,
                              float tickDelta,
                              UiProjectionMode projectionMode,
                              float alpha) {
    public UiRenderContext {
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }

    public UiRenderContext(Renderer2D renderer,
                           TextRenderer textRenderer,
                           GuiGraphicsExtractor drawContext,
                           float tickDelta) {
        this(renderer, textRenderer, drawContext, tickDelta, UiProjectionMode.CURRENT, 1.0f);
    }

    public UiRenderContext(Renderer2D renderer,
                           TextRenderer textRenderer,
                           GuiGraphicsExtractor drawContext,
                           float tickDelta,
                           UiProjectionMode projectionMode) {
        this(renderer, textRenderer, drawContext, tickDelta, projectionMode, 1.0f);
    }
}
