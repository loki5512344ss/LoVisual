/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.api.v0.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.loki.lovisual.render.engine.pipeline.CombatantRenderPipelines;
import dev.loki.lovisual.render.engine.pipeline.DepthTestFunction;
import dev.loki.lovisual.render.engine.pipeline.ExtendedRenderPipelineBuilder;
import dev.loki.lovisual.render.engine.rhi.clip.ShapeClipRenderPassContract;
import net.minecraft.resources.Identifier;

@SuppressWarnings("unused")
public final class LoVisualRenderPipelineBuilder {
    private final ExtendedRenderPipelineBuilder delegate;

    private LoVisualRenderPipelineBuilder(RenderPipeline.Snippet... snippets) {
        this.delegate = new ExtendedRenderPipelineBuilder(snippets);
    }

    public static LoVisualRenderPipelineBuilder create(RenderPipeline.Snippet... snippets) {
        return new LoVisualRenderPipelineBuilder(snippets);
    }

    public static RenderPipeline.Snippet meshUniforms() {
        return CombatantRenderPipelines.meshUniforms();
    }

    public LoVisualRenderPipelineBuilder location(Identifier id) {
        delegate.withLocation(id);
        return this;
    }

    public LoVisualRenderPipelineBuilder vertexFormat(VertexFormat format, PrimitiveTopology mode) {
        delegate.withVertexFormat(format, mode);
        return this;
    }

    public LoVisualRenderPipelineBuilder vertexShader(Identifier id) {
        delegate.withVertexShader(id);
        return this;
    }

    public LoVisualRenderPipelineBuilder fragmentShader(Identifier id) {
        delegate.withFragmentShader(id);
        return this;
    }

    public LoVisualRenderPipelineBuilder depthTest(DepthTestFunction function) {
        delegate.withDepthTestFunction(function);
        return this;
    }

    public LoVisualRenderPipelineBuilder depthWrite(boolean write) {
        delegate.withDepthWrite(write);
        return this;
    }

    public LoVisualRenderPipelineBuilder blend(BlendFunction blend) {
        delegate.withBlend(blend);
        return this;
    }

    public LoVisualRenderPipelineBuilder cull(boolean cull) {
        delegate.withCull(cull);
        return this;
    }

    public LoVisualRenderPipelineBuilder sampler(String name) {
        delegate.withSampler(name);
        return this;
    }

    public LoVisualRenderPipelineBuilder uniform(String name, UniformType type) {
        delegate.withUniform(name, type);
        return this;
    }

    public LoVisualRenderPipelineBuilder lineSmooth() {
        delegate.withLineSmooth();
        return this;
    }

    public LoVisualRenderPipelineBuilder shapeClipSupport() {
        delegate.withShapeClipSupport();
        return this;
    }

    public LoVisualRenderPipelineBuilder shapeClipContract(ShapeClipRenderPassContract contract) {
        delegate.withShapeClipContract(contract);
        return this;
    }

    public RenderPipeline build() {
        return delegate.build();
    }

    public RenderPipeline buildAndRegister() {
        return CombatantRenderPipelines.registerAddonPipeline(build());
    }
}
