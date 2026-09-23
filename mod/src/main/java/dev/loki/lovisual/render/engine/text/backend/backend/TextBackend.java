/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.text.backend.backend;

import dev.loki.lovisual.render.engine.core.frame.RenderFrameContext;
import dev.loki.lovisual.render.engine.rhi.resource.LoVisualRhi;

import java.util.List;
import dev.loki.lovisual.render.engine.text.backend.backend.TextDrawCommand;
import dev.loki.lovisual.render.engine.text.backend.placement.TextPlacementResolver;
import dev.loki.lovisual.render.engine.text.backend.placement.TextPlacementTransform;

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
