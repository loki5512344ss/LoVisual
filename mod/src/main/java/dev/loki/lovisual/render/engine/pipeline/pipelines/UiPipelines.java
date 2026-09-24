package dev.loki.lovisual.render.engine.pipeline.pipelines;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.loki.lovisual.render.engine.pipeline.ExtendedRenderPipelineBuilder;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.vertex.LoVisualVertexFormats;
import net.minecraft.resources.Identifier;

/**
 * Factory templates for LoVisual's UI (2D) pipelines: the base colored/textured set and
 * the batched SDF shape pipelines built on the local-color rect-params vertex formats.
 * Samplers are declared before extra uniform buffers, matching the original order.
 */
public final class UiPipelines {
    private UiPipelines() {
    }

    /**
     * UI colored primitives (pos2 + color); the canvas check in the builder still applies.
     */
    public static RenderPipeline colored(String name, PrimitiveTopology topology) {
        return PipelineTemplates.finish(base(name)
                .withVertexFormat(LoVisualVertexFormats.POS2_COLOR, topology)
                .withVertexShader(LoVisualRenderPipelines.SHADER_POS_COLOR_VERT)
                .withFragmentShader(LoVisualRenderPipelines.SHADER_POS_COLOR_FRAG), false, BlendFunction.TRANSLUCENT, true);
    }

    /**
     * UI textured triangles (pos2 + tex + color); {@code uniform} may be null.
     */
    public static RenderPipeline textured(String name, Identifier vertexShader, Identifier fragmentShader,
                                          BlendFunction blend, boolean cull, String uniform, String... samplers) {
        ExtendedRenderPipelineBuilder builder = base(name)
                .withVertexFormat(LoVisualVertexFormats.POS2_TEXTURE_COLOR, PrimitiveTopology.TRIANGLES)
                .withVertexShader(vertexShader)
                .withFragmentShader(fragmentShader);
        for (String sampler : samplers) {
            builder.withSampler(sampler);
        }
        if (uniform != null) {
            builder.withUniform(uniform, UniformType.UNIFORM_BUFFER);
        }
        return PipelineTemplates.finish(builder, false, blend, cull);
    }

    /**
     * UI text with liquid glass sampling; MSDF variants add the "MsdfText" uniform.
     */
    public static RenderPipeline textLiquidGlass(String name, Identifier fragmentShader, boolean msdf) {
        ExtendedRenderPipelineBuilder builder = base(name)
                .withVertexFormat(LoVisualVertexFormats.POS2_TEXTURE_COLOR, PrimitiveTopology.TRIANGLES)
                .withVertexShader(LoVisualRenderPipelines.SHADER_TEXT_VERT)
                .withFragmentShader(fragmentShader)
                .withSampler("u_Texture")
                .withSampler("u_SceneTexture")
                .withSampler("u_BlurTexture")
                .withUniform("UIBatch", UniformType.UNIFORM_BUFFER);
        if (msdf) {
            builder.withUniform("MsdfText", UniformType.UNIFORM_BUFFER);
        }
        return PipelineTemplates.finish(builder, false, BlendFunction.TRANSLUCENT, true);
    }

    /**
     * Batched SDF shape pipeline (local-color rect-params formats) with the shared UI batch
     * uniforms; optional texture samplers keep their original declaration order.
     */
    public static RenderPipeline batch(String name, VertexFormat format, Identifier vertexShader, Identifier fragmentShader,
                                       BlendFunction blend, boolean cull, String... samplers) {
        ExtendedRenderPipelineBuilder builder = new ExtendedRenderPipelineBuilder(PipelineTemplates.MESH_UNIFORMS, PipelineTemplates.UI_BATCH_UNIFORMS)
                .withLocation(PipelineTemplates.location(name))
                .withVertexFormat(format, PrimitiveTopology.TRIANGLES)
                .withVertexShader(vertexShader)
                .withFragmentShader(fragmentShader);
        for (String sampler : samplers) {
            builder.withSampler(sampler);
        }
        return PipelineTemplates.finish(builder, false, blend, cull);
    }

    private static ExtendedRenderPipelineBuilder base(String name) {
        return new ExtendedRenderPipelineBuilder(PipelineTemplates.MESH_UNIFORMS)
                .withLocation(PipelineTemplates.location(name));
    }
}
