/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.pipeline.pipelines;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.shaders.UniformType;
import dev.loki.lovisual.render.engine.pipeline.DepthTestFunction;
import dev.loki.lovisual.render.engine.pipeline.ExtendedRenderPipelineBuilder;
import net.minecraft.resources.Identifier;

/**
 * Shared building blocks for the LoVisual pipeline factories: uniform snippets, blend
 * presets and the common depth/blend/cull tail that every custom pipeline applies.
 */
public final class PipelineTemplates {
    /** Additive blending (SRC_ALPHA, ONE) shared by all additive LoVisual pipelines. */
    public static final BlendFunction ADDITIVE = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE);

    public static final RenderPipeline.Snippet MESH_UNIFORMS = uniformSnippet("MeshData");
    public static final RenderPipeline.Snippet FOG_UNIFORMS = uniformSnippet("Fog");
    public static final RenderPipeline.Snippet UI_BATCH_UNIFORMS = uniformSnippet("UIBatch");

    private PipelineTemplates() {
    }

    /**
     * Snippet declaring a single {@link UniformType#UNIFORM_BUFFER} uniform.
     */
    public static RenderPipeline.Snippet uniformSnippet(String name) {
        return new ExtendedRenderPipelineBuilder()
                .withUniform(name, UniformType.UNIFORM_BUFFER)
                .buildSnippet();
    }

    /**
     * Location identifier for a "pipeline/&lt;name&gt;" entry in the lovisual namespace.
     */
    public static Identifier location(String name) {
        return Identifier.fromNamespaceAndPath("lovisual", "pipeline/" + name);
    }

    /**
     * Applies the shared pipeline tail (depth test, depth write off, blend, cull) and builds.
     */
    public static RenderPipeline finish(ExtendedRenderPipelineBuilder builder, boolean depthTest, BlendFunction blend, boolean cull) {
        return builder
                .withDepthTestFunction(depthTest ? DepthTestFunction.GEQUAL_DEPTH_TEST : DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(blend)
                .withCull(cull)
                .build();
    }
}
