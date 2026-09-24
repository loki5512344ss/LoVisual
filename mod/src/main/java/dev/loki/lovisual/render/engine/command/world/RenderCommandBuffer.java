package dev.loki.lovisual.render.engine.command.world;

import dev.loki.lovisual.render.engine.core.frame.RenderFrameContext;

public interface RenderCommandBuffer {
    void clear();

    int size();

    void submit(RenderFrameContext context);
}
