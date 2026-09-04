/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.text.backend;

import dev.loki.lovisual.render.engine.core.RenderFrameContext;
import dev.loki.lovisual.render.engine.rhi.LoVisualRhi;

import java.util.List;

public interface TextBackend {
    String id();

    boolean supports(TextDrawCommand command);

    default boolean supports(TextDrawCommand command, TextPlacementTransform placement) {
        return supports(command);
    }

    default void draw(TextDrawCommand command, RenderFrameContext context, LoVisualRhi rhi) {
        draw(command, TextPlacementResolver.resolve(command, context), context, rhi);
    }

    default void drawBatch(List<TextDrawCommand> commands, int start, int end, RenderFrameContext context, LoVisualRhi rhi) {
        for (int i = start; i < end; i++) {
            draw(commands.get(i), context, rhi);
        }
    }

    void draw(TextDrawCommand command, TextPlacementTransform placement, RenderFrameContext context, LoVisualRhi rhi);
}
