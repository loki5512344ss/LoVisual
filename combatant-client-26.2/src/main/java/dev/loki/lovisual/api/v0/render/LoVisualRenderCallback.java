/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.api.v0.render;

@FunctionalInterface
public interface LoVisualRenderCallback {
    void render(LoVisualRenderContext context);
}
