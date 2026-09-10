/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.core.frame;
import dev.loki.lovisual.render.engine.core.frame.RenderFrameContext;
import dev.loki.lovisual.render.engine.core.frame.RenderPhase;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;

/**
 * AutoCloseable phase guard. Every render hook should use this instead of raw enterPhase().
 * It restores the previous RenderFrameContext phase and closes profiler scopes even on exceptions.
 */
public final class RenderPhaseScope implements AutoCloseable {
    private final RenderPhase phase;
    private final RenderFrameContext previous;
    private final AutoCloseable profilerScope;
    private boolean closed;

    public RenderPhaseScope(RenderPhase phase, RenderFrameContext previous, AutoCloseable profilerScope) {

        this.phase = phase == null ? RenderPhase.NONE : phase;
        this.previous = previous;
        this.profilerScope = profilerScope;
    }

    public RenderPhase phase() {
        return phase;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try {
            if (profilerScope != null) {
                profilerScope.close();
            }
        } catch (Exception ignored) {
            // Profiling must never break render lifecycle recovery.
        } finally {
            LoVisualRenderSystem.restorePhase(previous);
        }
    }
}
