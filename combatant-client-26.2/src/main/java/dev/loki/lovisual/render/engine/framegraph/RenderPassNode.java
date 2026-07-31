/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.framegraph;

import dev.loki.lovisual.render.engine.core.RenderFrameContext;
import dev.loki.lovisual.render.engine.core.RenderPhase;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A named frame-graph node bound to a LoVisual render phase.
 */
public final class RenderPassNode {
    private final RenderPhase phase;
    private final String label;
    private final Consumer<RenderFrameContext> renderer;

    public RenderPassNode(RenderPhase phase, String label, Consumer<RenderFrameContext> renderer) {
        this.phase = phase == null ? RenderPhase.NONE : phase;
        this.label = label == null || label.isBlank() ? this.phase.name().toLowerCase() : label;
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    public RenderPhase phase() {
        return phase;
    }

    public String label() {
        return label;
    }

    public void execute(RenderFrameContext context) {
        renderer.accept(context);
    }
}
