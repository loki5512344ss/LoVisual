/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.deferred;

import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.Renderer2D.Deferred2DLayer;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.OrderedUiBatcher;

public record DeferredOrderedSubmit(Deferred2DLayer layer, ViewportContext viewport, int[] framebufferScissor, OrderedUiBatcher batcher) implements Deferred2DSubmit {
    @Override
    public void submit() {
        if (batcher == null) return;
        batcher.flush(true);
        Renderer2D.flushUiLayer();
    }

    @Override
    public void release() {
        Renderer2D.releaseDeferredBatcher(batcher);
    }
}
