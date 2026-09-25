package dev.loki.lovisual.render.engine.renderer.facade;

import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

/** Instance fields and constructor shared by the Renderer2D facade hierarchy. */
public class Renderer2DBase {

    protected final boolean textured;
    protected final MeshBuilder texturedTriangles;
    protected final int[] gradientTmp = new int[4];
    protected double alpha = 1.0;

    protected Renderer2DBase(boolean textured) {
        this.textured = textured;
        this.texturedTriangles = textured ? new MeshBuilder(LoVisualRenderPipelines.UI_TEXTURED) : null;
    }

    protected Renderer2D self() {
        return (Renderer2D) this;
    }
}
