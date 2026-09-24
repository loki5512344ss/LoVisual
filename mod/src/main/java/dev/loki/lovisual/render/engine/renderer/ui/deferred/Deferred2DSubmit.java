package dev.loki.lovisual.render.engine.renderer.ui.deferred;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;


import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.renderer.Renderer2D.Deferred2DLayer;

public interface Deferred2DSubmit {
    Deferred2DLayer layer();

    ViewportContext viewport();

    int[] framebufferScissor();

    void submit();

    default void release() {
    }
}
