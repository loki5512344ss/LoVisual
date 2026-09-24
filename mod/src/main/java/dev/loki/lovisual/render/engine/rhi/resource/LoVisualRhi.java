package dev.loki.lovisual.render.engine.rhi.resource;

import dev.loki.lovisual.render.engine.rhi.blit.TextureBlitter;
import dev.loki.lovisual.render.engine.rhi.clip.ShapeClipBackend;
import dev.loki.lovisual.render.engine.rhi.fullscreen.FullscreenBackend;
import dev.loki.lovisual.render.engine.rhi.msaa.MsaaControl;
import dev.loki.lovisual.render.engine.rhi.pipeline.layout.RenderPipelineRegistry;
import dev.loki.lovisual.render.engine.rhi.resource.manager.RenderResourceManager;
import dev.loki.lovisual.render.engine.rhi.state.PipelineStateBackend;
import dev.loki.lovisual.render.engine.rhi.upload.DynamicMeshBackend;
import dev.loki.lovisual.render.engine.rhi.command.FullscreenDrawCommand;
import dev.loki.lovisual.render.engine.rhi.command.RhiDrawCommand;
import dev.loki.lovisual.render.engine.rhi.stats.RhiStats;

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
