/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.pipeline.pipelines;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.loki.lovisual.render.engine.pipeline.ExtendedRenderPipelineBuilder;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.vertex.LoVisualVertexFormats;
import net.minecraft.resources.Identifier;

/**
 * Factory templates for LoVisual's world-space (3D) pipelines. They all share the mesh
 * uniform snippet, disable depth writing and culling; only depth test, blending, shaders
 * and vertex format vary. Shader identifiers come from the public facade constants.
 */
public final class WorldPipelines {
    private WorldPipelines() {
    }

    /**
     * Solid colored triangles; {@code depthTest} selects GEQUAL vs no depth test.
     */
    public static RenderPipeline colored(String name, boolean depthTest, BlendFunction blend) {
        return PipelineTemplates.finish(base(name)
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, PrimitiveTopology.TRIANGLES)
                .withVertexShader(LoVisualRenderPipelines.SHADER_POS_COLOR_VERT)
                .withFragmentShader(LoVisualRenderPipelines.SHADER_POS_COLOR_FRAG), depthTest, blend, false);
    }

    /**
     * Smooth debug lines rendered with the plain color shader.
     */
    public static RenderPipeline coloredLines(String name, boolean depthTest) {
        return PipelineTemplates.finish(base(name)
                .withLineSmooth()
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, PrimitiveTopology.DEBUG_LINES)
                .withVertexShader(LoVisualRenderPipelines.SHADER_POS_COLOR_VERT)
                .withFragmentShader(LoVisualRenderPipelines.SHADER_POS_COLOR_FRAG), depthTest, BlendFunction.TRANSLUCENT, false);
    }

    /**
     * Screen-space wide line expansion; triangles, never depends on glLineWidth.
     */
    public static RenderPipeline wideLines(String name, boolean depthTest) {
        return PipelineTemplates.finish(base(name)
                .withVertexFormat(LoVisualVertexFormats.POS3_COLOR_LINE, PrimitiveTopology.TRIANGLES)
                .withVertexShader(LoVisualRenderPipelines.SHADER_WIDE_LINE_VERT)
                .withFragmentShader(LoVisualRenderPipelines.SHADER_POS_COLOR_FRAG), depthTest, BlendFunction.TRANSLUCENT, false);
    }

    /**
     * Textured triangles sampling "u_Texture".
     */
    public static RenderPipeline textured(String name, boolean depthTest, BlendFunction blend) {
        return PipelineTemplates.finish(base(name)
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.TRIANGLES)
                .withVertexShader(LoVisualRenderPipelines.SHADER_POS_TEX_COLOR_VERT)
                .withFragmentShader(LoVisualRenderPipelines.SHADER_POS_TEX_COLOR_FRAG)
                .withSampler("u_Texture"), depthTest, blend, false);
    }

    /**
     * Textured triangles with shader-side tint ("TextureTint" uniform), additive blending.
     */
    public static RenderPipeline tintedTextured(String name, boolean depthTest) {
        return PipelineTemplates.finish(base(name)
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.TRIANGLES)
                .withVertexShader(LoVisualRenderPipelines.SHADER_POS_TEX_COLOR_VERT)
                .withFragmentShader(LoVisualRenderPipelines.SHADER_POS_TEX_COLOR_TINT_FRAG)
                .withSampler("u_Texture")
                .withUniform("TextureTint", UniformType.UNIFORM_BUFFER), depthTest, PipelineTemplates.ADDITIVE, false);
    }

    /**
     * World decal quads with SDF fill/stroke.
     */
    public static RenderPipeline decalSdf(String name, boolean depthTest) {
        return PipelineTemplates.finish(base(name)
                .withVertexFormat(LoVisualVertexFormats.POS3_TEXTURE_COLOR_PARAMS2, PrimitiveTopology.TRIANGLES)
                .withVertexShader(LoVisualRenderPipelines.SHADER_POS_TEX_COLOR_PARAMS2_VERT)
                .withFragmentShader(LoVisualRenderPipelines.SHADER_WORLD_DECAL_SDF_FRAG), depthTest, BlendFunction.TRANSLUCENT, false);
    }

    /**
     * World text quads; MSDF variants add the "MsdfText" uniform after "u_Texture".
     */
    public static RenderPipeline worldText(String name, boolean depthTest, Identifier fragmentShader, boolean msdf) {
        ExtendedRenderPipelineBuilder builder = base(name)
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.TRIANGLES)
                .withVertexShader(LoVisualRenderPipelines.SHADER_TEXT_VERT)
                .withFragmentShader(fragmentShader)
                .withSampler("u_Texture");
        if (msdf) {
            builder.withUniform("MsdfText", UniformType.UNIFORM_BUFFER);
        }
        return PipelineTemplates.finish(builder, depthTest, BlendFunction.TRANSLUCENT, false);
    }

    /**
     * Textured triangles with procedural sun shading ("SkySun" uniform), additive.
     */
    public static RenderPipeline skySun(String name) {
        return PipelineTemplates.finish(base(name)
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.TRIANGLES)
                .withVertexShader(LoVisualRenderPipelines.SHADER_POS_TEX_COLOR_VERT)
                .withFragmentShader(LoVisualRenderPipelines.SHADER_SKY_SUN_FRAG)
                .withUniform("SkySun", UniformType.UNIFORM_BUFFER), false, PipelineTemplates.ADDITIVE, false);
    }

    /**
     * Textured skybox with vanilla sky/fog blending ("SkyboxShader" uniform + fog snippet).
     */
    public static RenderPipeline texturedSkyboxFog(String name, Identifier fragmentShader, BlendFunction blend) {
        return PipelineTemplates.finish(fogBase(name)
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.TRIANGLES)
                .withVertexShader(LoVisualRenderPipelines.SHADER_POS_TEX_COLOR_SKY_FOG_VERT)
                .withFragmentShader(fragmentShader)
                .withSampler("u_Texture")
                .withUniform("SkyboxShader", UniformType.UNIFORM_BUFFER), false, blend, false);
    }

    /**
     * Fullscreen procedural skybox with sky/fog blending ("SkyboxShader" uniform + fog snippet).
     */
    public static RenderPipeline proceduralSkyboxFog(String name, Identifier fragmentShader) {
        return PipelineTemplates.finish(fogBase(name)
                .withVertexFormat(LoVisualVertexFormats.POS2, PrimitiveTopology.TRIANGLES)
                .withVertexShader(LoVisualRenderPipelines.SHADER_DAMAGE_TINT_VERT)
                .withFragmentShader(fragmentShader)
                .withUniform("SkyboxShader", UniformType.UNIFORM_BUFFER), false, BlendFunction.TRANSLUCENT, false);
    }

    private static ExtendedRenderPipelineBuilder base(String name) {
        return new ExtendedRenderPipelineBuilder(PipelineTemplates.MESH_UNIFORMS)
                .withLocation(PipelineTemplates.location(name));
    }

    private static ExtendedRenderPipelineBuilder fogBase(String name) {
        return new ExtendedRenderPipelineBuilder(PipelineTemplates.MESH_UNIFORMS, PipelineTemplates.FOG_UNIFORMS)
                .withLocation(PipelineTemplates.location(name));
    }
}
