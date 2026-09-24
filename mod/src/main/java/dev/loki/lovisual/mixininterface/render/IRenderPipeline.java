package dev.loki.lovisual.mixininterface.render;

import dev.loki.lovisual.render.engine.rhi.clip.ShapeClipRenderPassContract;

public interface IRenderPipeline {
    void lovisual$setLineSmooth(boolean lineSmooth);

    boolean lovisual$getLineSmooth();

    void lovisual$setShapeClipContract(ShapeClipRenderPassContract contract);

    ShapeClipRenderPassContract lovisual$getShapeClipContract();
}
