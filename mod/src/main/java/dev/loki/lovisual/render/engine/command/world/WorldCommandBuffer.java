package dev.loki.lovisual.render.engine.command.world;

import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.core.frame.RenderFrameContext;
import dev.loki.lovisual.render.engine.rhi.command.RhiDrawCommand;

import java.util.ArrayList;
import java.util.List;
import dev.loki.lovisual.render.engine.command.world.RenderCommandBuffer;

public final class WorldCommandBuffer implements RenderCommandBuffer {
    private final List<RhiDrawCommand> commands = new ArrayList<>();

    public void add(RhiDrawCommand command) {
        if (command != null) commands.add(command);
    }

    @Override
    public void clear() {
        commands.clear();
    }

    @Override
    public int size() {
        return commands.size();
    }

    @Override
    public void submit(RenderFrameContext context) {
        for (RhiDrawCommand command : commands) {
            LoVisualRenderSystem.rhi().drawMesh(command);
        }
        clear();
    }
}
