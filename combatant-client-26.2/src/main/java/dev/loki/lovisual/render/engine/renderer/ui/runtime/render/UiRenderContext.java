/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.TextRenderer;

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
