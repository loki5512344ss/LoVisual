/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.world;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

import java.util.List;

/**
 * World command buffer owned by Renderer3D facade.
 */
public final class WorldCommandBuffer {
    private final WorldBatcher batcher = new WorldBatcher();

    public void beginFrame() {
        batcher.beginFrame();
    }

    public MeshBuilder batch(RenderPipeline pipeline,
                             Renderer3D.DepthMode depthMode,
                             float lineWidth,
                             Renderer3D.BatchBindings bindings) {
        return batcher.batch(pipeline, depthMode, lineWidth, bindings);
    }

    public List<WorldDrawCommand> commands() {
        return batcher.commands();
    }

    public int size() {
        return batcher.commandCount();
    }

    public void clearAfterSubmit() {
        batcher.clearAfterSubmit();
    }
}
