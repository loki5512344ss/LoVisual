/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui;

import dev.loki.lovisual.render.engine.command.UiBatchBackendCommand;
import dev.loki.lovisual.render.engine.command.UiCommand;
import dev.loki.lovisual.render.engine.command.UiCommandBuffer;
import dev.loki.lovisual.render.engine.command.UiStatsSnapshot;
import dev.loki.lovisual.render.engine.core.RenderFrameContext;
import dev.loki.lovisual.render.engine.rhi.LoVisualRhi;

/**
 * UI rendering gateway: Renderer2D records normalized commands here, the compiler builds a
 * UI pass plan, and the executor owns the future RHI execution point. Renderer2D batcher mesh
 * draws are represented explicitly as backend commands.
 */
public final class UiRendererSubsystem {
    private final UiCommandBuffer commands = new UiCommandBuffer();
    private final UiPassCompiler compiler = new UiPassCompiler();
    private final UiPassExecutor executor = new UiPassExecutor();
    private final UiEffectGraph effects = new UiEffectGraph();
    private UiBatchPlan lastPlan = UiBatchPlan.EMPTY;

    public UiCommandBuffer commands() {
        return commands;
    }

    public void beginFrame(RenderFrameContext context) {
        commands.beginFrame(context);
    }

    public void record(UiCommand command) {
        commands.add(command);
    }

    public void recordBackendCommand(String backend, String batchType) {
        commands.add(new UiBatchBackendCommand(backend, batchType));
    }

    public boolean hasPendingCommands() {
        return commands.size() > 0;
    }

    public void flush(RenderFrameContext context, LoVisualRhi rhi) {
        commands.beginFrame(context);
        if (commands.size() == 0) {
            lastPlan = UiBatchPlan.EMPTY;
            executor.execute(lastPlan, context, rhi);
            return;
        }
        lastPlan = compiler.compile(commands);
        effects.execute(commands, context, rhi);
        executor.execute(lastPlan, context, rhi);
        commands.clear();
    }

    public UiBatchPlan lastPlan() {
        return lastPlan;
    }

    public UiStatsSnapshot statsSnapshot() {
        return commands.statsSnapshot();
    }
}
