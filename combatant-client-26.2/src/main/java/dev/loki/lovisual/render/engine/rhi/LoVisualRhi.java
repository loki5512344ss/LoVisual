/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.rhi;

import dev.loki.lovisual.render.engine.rhi.blit.TextureBlitter;
import dev.loki.lovisual.render.engine.rhi.clip.ShapeClipBackend;
import dev.loki.lovisual.render.engine.rhi.fullscreen.FullscreenBackend;
import dev.loki.lovisual.render.engine.rhi.msaa.MsaaControl;
import dev.loki.lovisual.render.engine.rhi.pipeline.RenderPipelineRegistry;
import dev.loki.lovisual.render.engine.rhi.resource.RenderResourceManager;
import dev.loki.lovisual.render.engine.rhi.state.PipelineStateBackend;
import dev.loki.lovisual.render.engine.rhi.upload.DynamicMeshBackend;

public interface LoVisualRhi extends AutoCloseable {
    DynamicMeshBackend dynamicMeshes();

    FullscreenBackend fullscreen();

    TextureBlitter textureBlitter();

    MsaaControl msaa();

    ShapeClipBackend shapeClip();

    PipelineStateBackend pipelineState();

    RhiStats stats();

    RenderPipelineRegistry pipelines();

    RenderResourceManager resources();

    void beginFrame(long frameId);

    void endRenderSubmission();

    void framePresented();

    void drawMesh(RhiDrawCommand command);

    void drawFullscreen(FullscreenDrawCommand command);

    @Override
    void close();
}
