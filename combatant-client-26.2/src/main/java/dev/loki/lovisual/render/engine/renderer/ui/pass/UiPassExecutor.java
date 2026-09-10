/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.pass;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;


import dev.loki.lovisual.render.engine.core.frame.RenderFrameContext;
import dev.loki.lovisual.render.engine.rhi.resource.LoVisualRhi;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.UiBatchPlan;

/**
 * Executes compiled UI passes. Renderer2D ordered batches remain the production executor;
 * the normalized command stream is the source that future specialized passes lower from.
 */
public final class UiPassExecutor {
    private UiBatchPlan lastPlan = UiBatchPlan.EMPTY;

    public void execute(UiBatchPlan plan, RenderFrameContext context, LoVisualRhi rhi) {
        lastPlan = plan != null ? plan : UiBatchPlan.EMPTY;
    }

    public UiBatchPlan lastPlan() {
        return lastPlan;
    }
}
