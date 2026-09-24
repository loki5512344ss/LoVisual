package dev.loki.lovisual.features.module.modules.visuals.effects.ambient;

import net.minecraft.resources.Identifier;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.renderer.Renderer3DBatchBindings;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.engine.uniform.impl.hand.effects.TextureTintUniforms;

/**
 * Фабрики батчей WorldParticles: спрайты, линии, оболочка пузыря,
 * тонированная текстурная пачка (воронка).
 */
final class WorldParticlesBatches {
    private WorldParticlesBatches() {}

    static MeshBuilder sprite(Renderer3D renderer, boolean useDepth, Renderer3D.DepthMode depthMode) {
        return renderer.batchTextured(
                useDepth ? LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE_LIQUID_IGNORE
                        : LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE,
                TextureStorage.BLOOM,
                depthMode
        );
    }

    static MeshBuilder lines(Renderer3D renderer, boolean useDepth, Renderer3D.DepthMode depthMode, float width) {
        float prevLineWidth = RenderState.lineWidth;
        try {
            RenderState.lineWidth = Math.max(0.5f, width);
            return renderer.batch(
                    useDepth ? LoVisualRenderPipelines.WORLD_COLORED_LINES_LIQUID_IGNORE
                            : LoVisualRenderPipelines.WORLD_COLORED_LINES,
                    depthMode
            );
        } finally {
            RenderState.lineWidth = prevLineWidth;
        }
    }

    static MeshBuilder bubbleShell(Renderer3D renderer, boolean useDepth, Renderer3D.DepthMode depthMode) {
        return renderer.batch(
                useDepth ? LoVisualRenderPipelines.WORLD_COLORED_LIQUID_IGNORE
                        : LoVisualRenderPipelines.WORLD_COLORED,
                depthMode
        );
    }

    static MeshBuilder tintedTexture(Renderer3D renderer, boolean useDepth, Renderer3D.DepthMode depthMode,
                                     Identifier texture) {
        return renderer.batch(
                useDepth
                        ? LoVisualRenderPipelines.WORLD_TEXTURED_TINT_ADDITIVE_LIQUID_IGNORE
                        : LoVisualRenderPipelines.WORLD_TEXTURED_TINT_ADDITIVE,
                depthMode,
                Renderer3DBatchBindings.none()
                        .withSampler("u_Texture", texture)
                        .withUniform("TextureTint", TextureTintUniforms.get())
        );
    }
}
