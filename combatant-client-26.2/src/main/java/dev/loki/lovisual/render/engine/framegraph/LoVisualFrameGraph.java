/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.framegraph;

import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.core.frame.RenderFrameContext;
import dev.loki.lovisual.render.engine.core.frame.RenderPhase;
import dev.loki.lovisual.render.engine.core.frame.RenderPhaseScope;

import java.util.function.Consumer;

public final class LoVisualFrameGraph {
    private final PassScheduler scheduler = new PassScheduler();

    public void add(RenderPhase phase, Consumer<RenderFrameContext> handler) {
        add(phase, phase == null ? "unnamed" : phase.name().toLowerCase(), handler);
    }

    public void add(RenderPhase phase, String label, Consumer<RenderFrameContext> handler) {
        if (handler == null) return;
        scheduler.add(new RenderPassNode(phase, label, handler));
    }

    public void execute(RenderPhase phase, RenderFrameContext baseContext) {
        String label = "framegraph:" + (phase == null ? "none" : phase.name().toLowerCase());
        try (RenderPhaseScope ignored = LoVisualRenderSystem.phase(phase, label)) {
            RenderFrameContext ctx = LoVisualRenderSystem.currentContext();
            if (ctx == null && baseContext != null) ctx = baseContext.withPhase(phase);
            if (ctx == null) return;
            for (RenderPassNode node : scheduler.nodes(phase)) {
                try (RenderPhaseScope nodeScope = LoVisualRenderSystem.phase(phase, "pass:" + node.label())) {
                    node.execute(LoVisualRenderSystem.currentContext());
                }
            }
        }
    }

    public void clear() {
        scheduler.clear();
    }
}
