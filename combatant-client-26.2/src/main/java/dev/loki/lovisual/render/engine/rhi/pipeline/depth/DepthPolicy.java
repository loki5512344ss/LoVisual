/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.rhi.pipeline.depth;

public enum DepthPolicy {
    NONE,
    MAIN_FRAMEBUFFER,
    PRE_TRANSLUCENT,
    CUSTOM;

    public boolean needsDepthAttachment() {
        return this == MAIN_FRAMEBUFFER || this == PRE_TRANSLUCENT || this == CUSTOM;
    }
}
