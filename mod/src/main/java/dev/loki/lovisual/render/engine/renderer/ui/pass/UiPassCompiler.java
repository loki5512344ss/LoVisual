/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.pass;

import dev.loki.lovisual.render.engine.command.core.UiCommandBuffer;
import dev.loki.lovisual.render.engine.renderer.ui.batches.UiBatcher;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.UiBatchPlan;

public final class UiPassCompiler {
    private final UiBatcher batcher = new UiBatcher();

    public UiBatchPlan compile(UiCommandBuffer commands) {
        return batcher.compile(commands);
    }
}
