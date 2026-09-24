package dev.loki.lovisual.render.engine.renderer.ui.deferred;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;


import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.renderer.Renderer2D.Deferred2DLayer;

public record DeferredActionSubmit(Deferred2DLayer layer, ViewportContext viewport, int[] framebufferScissor, Runnable action) implements Deferred2DSubmit {
    @Override
    public void submit() {
        if (action != null) {
            action.run();
        }
    }
}
