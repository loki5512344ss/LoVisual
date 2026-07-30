/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.command;

import dev.loki.lovisual.render.engine.core.CombatantRenderSystem;
import dev.loki.lovisual.render.engine.core.RenderFrameContext;
import dev.loki.lovisual.render.engine.rhi.RhiDrawCommand;

import java.util.ArrayList;
import java.util.List;

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
            CombatantRenderSystem.rhi().drawMesh(command);
        }
        clear();
    }
}
