/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.command;

import dev.loki.lovisual.render.engine.core.RenderFrameContext;

public interface RenderCommandBuffer {
    void clear();

    int size();

    void submit(RenderFrameContext context);
}
