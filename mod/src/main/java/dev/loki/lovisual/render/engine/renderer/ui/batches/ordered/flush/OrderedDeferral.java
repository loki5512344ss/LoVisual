/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.flush;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.batches.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.TextBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.OrderedUiBatcher;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.order.OrderState;
import dev.loki.lovisual.render.engine.renderer.ui.blur.kawase.SharedKawaseBlur;
import dev.loki.lovisual.render.engine.renderer.ui.deferred.DeferredOrderedSubmit;
import dev.loki.lovisual.render.engine.renderer.ui.deferred.sched.UiDeferredScheduler;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;

/**
 * Отложенная отправка order-списка: текущий батчер уходит в очередь deferred-слоя,
 * а в Renderer2D.UI_BATCHER подменяется свежим экземпляром из пула.
 */
public final class OrderedDeferral {
    private OrderedDeferral() {
    }

    public static void defer(OrderedUiBatcher batcher, OrderState state, SharedKawaseBlur blur, boolean finish) {
        if (!state.hasPendingWork()) {
            Renderer2D.BATCH_STATS.noteEmpty(batcher.isActive(), state.poolTotal());
            if (finish) {
                blur.reset();
                batcher.deactivate();
            }
            return;
        }

        int batches = state.entryCount();
        int vertices = 0;
        int indices = 0;
        for (int i = 0; i < state.entries().size(); i++) {
            Object entry = state.entries().get(i);
            if (entry instanceof DrawBatch drawBatch) {
                if (drawBatch.mesh.isBuilding()) drawBatch.mesh.end();
                vertices += drawBatch.mesh.getVertexCount();
                indices += drawBatch.mesh.getIndicesCount();
            } else if (entry instanceof TextBatch textBatch && textBatch.mesh != null) {
                if (textBatch.mesh.isBuilding()) textBatch.mesh.end();
                vertices += textBatch.mesh.getVertexCount();
                indices += textBatch.mesh.getIndicesCount();
            }
        }

        UiDeferredScheduler.enqueue(new DeferredOrderedSubmit(
                UiDeferredScheduler.layerForCurrentPhase(state.onlyItems()),
                UiDeferredScheduler.snapshotViewport(),
                ScissorFunction.currentFramebufferScissor(),
                batcher
        ));

        OrderedUiBatcher replacement = UiDeferredScheduler.obtainBatcher();
        replacement.prepareAsReplacement(!finish);
        Renderer2D.UI_BATCHER = replacement;

        Renderer2D.BATCH_STATS.update(batcher.isActive(), batches, 0, vertices, indices, state.poolTotal());
        Renderer2D.BATCH_STATS.setActive(!finish);
    }
}
