/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.render.bridge;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.UiBounds;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;

import java.util.ArrayDeque;
import java.util.Deque;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiProjectionMode;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiRenderContext;

public final class UiClipStack {
    private final Deque<Boolean> pushed = new ArrayDeque<>();

    public boolean push(UiBounds bounds, UiRenderContext context) {
        if (bounds == null || bounds.width() <= 0.0f || bounds.height() <= 0.0f) {
            pushed.push(false);
            return false;
        }
        boolean ok = switch (context != null ? context.projectionMode() : UiProjectionMode.CURRENT) {
            case SCALED_SCREEN -> ScissorFunction.pushRaw(bounds.x(), bounds.y(), bounds.width(), bounds.height());
            case CURRENT, RAW_FRAMEBUFFER, UNSCALED_LOGICAL ->
                    ScissorFunction.pushRaw(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        };
        pushed.push(ok);
        return ok;
    }

    public void pop() {
        if (pushed.isEmpty()) return;
        if (pushed.pop()) {
            ScissorFunction.pop();
        }
    }
}
