/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.command.core;

import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.core.frame.RenderFrameContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import dev.loki.lovisual.render.engine.command.core.UiCommand;
import dev.loki.lovisual.render.engine.command.core.UiCommandStats;
import dev.loki.lovisual.render.engine.command.world.RenderCommandBuffer;
import dev.loki.lovisual.render.engine.command.world.UiStatsSnapshot;

/**
 * High-level normalized UI command stream.
 */
public final class UiCommandBuffer implements RenderCommandBuffer {
    private final List<UiCommand> commands = new ArrayList<>();
    private final UiCommandStats stats = new UiCommandStats();

    public void beginFrame(RenderFrameContext context) {
        if (context != null) {
            stats.beginFrame(context.frameId());
        }
    }

    public void add(UiCommand command) {
        if (command == null) return;
        RenderFrameContext ctx = LoVisualRenderSystem.currentContext();
        if (ctx != null) stats.beginFrame(ctx.frameId());
        commands.add(command);
        stats.record(command);
    }

    public List<UiCommand> commands() {
        return commands;
    }

    public List<UiCommand> snapshot() {
        return Collections.unmodifiableList(commands);
    }

    public UiCommandStats stats() {
        return stats;
    }

    public UiStatsSnapshot statsSnapshot() {
        return stats.snapshot();
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
        beginFrame(context);
    }
}
