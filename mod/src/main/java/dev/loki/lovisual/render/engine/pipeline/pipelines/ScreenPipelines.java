package dev.loki.lovisual.render.engine.pipeline.pipelines;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import dev.loki.lovisual.render.engine.pipeline.ExtendedRenderPipelineBuilder;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.vertex.LoVisualVertexFormats;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Factory templates for LoVisual's fullscreen pos2 pipelines (post effects, hand effects,
 * shader skies) plus the vanilla-textured GUI lookup pipeline. Samplers are declared
 * before the single uniform buffer, matching the original declaration order.
 */
public final class ScreenPipelines {
    private ScreenPipelines() {
    }

    /**
     * Minimal vanilla-textured pipeline used for GUI texture lookups.
     */
    public static RenderPipeline guiTextureLookup() {
        return new ExtendedRenderPipelineBuilder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation(PipelineTemplates.location("gui_texture_lookup"))
                .withFragmentShader(LoVisualRenderPipelines.SHADER_GUI_TEXTURE_LOOKUP_FRAG)
                .build();
    }

    /**
     * Fullscreen pos2 pass: damage-tint vertex shader, no depth test, translucent, no culling.
     */
    public static RenderPipeline screen(String name, Identifier fragmentShader, String uniform, String... samplers) {
        ExtendedRenderPipelineBuilder builder = base(name)
                .withVertexFormat(LoVisualVertexFormats.POS2, PrimitiveTopology.TRIANGLES)
                .withVertexShader(LoVisualRenderPipelines.SHADER_DAMAGE_TINT_VERT)
                .withFragmentShader(fragmentShader);
        for (String sampler : samplers) {
            builder.withSampler(sampler);
        }
        builder.withUniform(uniform, UniformType.UNIFORM_BUFFER);
        return PipelineTemplates.finish(builder, false, BlendFunction.TRANSLUCENT, false);
    }

    /**
     * Fullscreen hand effect with the standard "u_Src"/"u_Mask" sampler pair.
     */
    public static RenderPipeline handEffect(String name, Identifier fragmentShader, String uniform) {
        return screen(name, fragmentShader, uniform, "u_Src", "u_Mask");
    }

    /**
     * Fullscreen chams blend effect sampling two shader textures plus the mask.
     */
    public static RenderPipeline handBlendEffect(String name, Identifier fragmentShader) {
        return screen(name, fragmentShader, "HandBlend", "u_Shader1", "u_Shader2", "u_Mask");
    }

    private static ExtendedRenderPipelineBuilder base(String name) {
        return new ExtendedRenderPipelineBuilder(PipelineTemplates.MESH_UNIFORMS)
                .withLocation(PipelineTemplates.location(name));
    }
}
