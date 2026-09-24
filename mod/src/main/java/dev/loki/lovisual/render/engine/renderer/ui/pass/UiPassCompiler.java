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
