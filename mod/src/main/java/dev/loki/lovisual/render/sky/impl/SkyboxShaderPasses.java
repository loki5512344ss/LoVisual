package dev.loki.lovisual.render.sky;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.scene.visual.ReimaginedVisual;
import dev.loki.lovisual.features.module.modules.visuals.weather.sky.ShaderSky;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.FullScreenRenderer;
import dev.loki.lovisual.render.engine.renderer.MeshRenderer;
import dev.loki.lovisual.render.engine.uniform.impl.sky.skybox.ShaderSkyUniforms;
import dev.loki.lovisual.render.engine.uniform.impl.sky.skybox.SkyboxShaderUniforms;

/**
 * Fullscreen shader sky passes for {@link CustomSkyboxRenderer}: the Reimagined shader skybox and
 * the procedural ShaderSky (Pulsar/Thunder). Each uploads its uniform block, then draws the shared
 * fullscreen quad through the deferred {@link MeshRenderer} while temporarily flattening projection
 * and the model-view stack.
 */
final class SkyboxShaderPasses {
    private SkyboxShaderPasses() {
    }

    static void renderSkybox(Minecraft mc,
                             RenderTarget framebuffer,
                             Camera camera,
                             GpuBufferSlice fog,
                             SkyRenderState sky,
                             float alpha,
                             float tickDelta,
                             float fov,
                             Matrix4f worldProjection) {
        if (alpha <= 0.001f || fog == null) return;
        ReimaginedVisual visual = Modules.get(ReimaginedVisual.class);
        if (visual == null || !visual.isShaderSkyboxEnabled()) return;

        float time = AnimationUtility.time(0.001f);
        float width = mc.getWindow() != null ? mc.getWindow().getWidth() : 1.0f;
        float height = mc.getWindow() != null ? mc.getWindow().getHeight() : 1.0f;
        float yawRad = (float) Math.toRadians(-camera.yRot());
        float pitchRad = (float) Math.toRadians(camera.xRot());
        float shaderFov = SkyboxProjection.extractFov(worldProjection, fov);
        SkyboxShaderUniforms.update(
                visual.getShaderSkyboxColor(),
                sky.skyColor & 0x00FFFFFF,
                alpha,
                visual.getSkyboxSkyFogBlend(),
                time,
                visual.getSkyboxShaderMotionSpeed(),
                5.0f,
                0.01f,
                width,
                height,
                yawRad,
                pitchRad,
                shaderFov,
                visual.isSkyboxShaderAuroraEnabled() ? 1.0f : 0.0f,
                visual.getSkyboxShaderAuroraIntensity(),
                visual.getSkyboxShaderAuroraSpeed(),
                visual.getSkyboxShaderSmallStars(),
                visual.getSkyboxShaderDustStars(),
                visual.getSkyboxShaderMediumStars(),
                visual.getSkyboxShaderLargeStars(),
                visual.getSkyboxShaderStarBrightness(),
                visual.getSkyboxShaderTwinkleStrength(),
                visual.getSkyboxShaderLayerMask()
        );

        FullScreenRenderer.ensureInit();
        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        MeshRenderer.setProjection(new Matrix4f());
        try {
            MeshRenderer.begin()
                    .attachments(framebuffer)
                    .pipeline(LoVisualRenderPipelines.WORLD_REIMAGINED_SKYBOX_SHADER)
                    .mesh(FullScreenRenderer.vbo, FullScreenRenderer.ibo)
                    .uniform("Fog", fog)
                    .uniform("SkyboxShader", SkyboxShaderUniforms.get())
                    .end();
        } finally {
            MeshRenderer.setProjection(worldProjection);
            mv.popMatrix();
        }
    }

    static void renderSky(Minecraft mc,
                          RenderTarget framebuffer,
                          Camera camera,
                          int skyColor,
                          float alpha,
                          float tickDelta,
                          float fov,
                          Matrix4f worldProjection) {
        if (alpha <= 0.001f) return;
        ShaderSky skyModule = Modules.get(ShaderSky.class);
        if (skyModule == null || !skyModule.isEnabled()) return;

        RenderPipeline pipeline = pipelineForMode(skyModule.getMode());
        if (pipeline == null) return;

        float time = AnimationUtility.time(0.001f);
        float width = mc.getWindow() != null ? mc.getWindow().getWidth() : 1.0f;
        float height = mc.getWindow() != null ? mc.getWindow().getHeight() : 1.0f;
        float yawRad = (float) Math.toRadians(-camera.yRot());
        float pitchRad = (float) Math.toRadians(camera.xRot());
        float shaderFov = SkyboxProjection.extractFov(worldProjection, fov);
        ShaderSkyUniforms.update(
                skyColor,
                alpha * skyModule.getAlpha(),
                time,
                skyModule.getSpeed(),
                skyModule.getScale(),
                skyModule.getIntensity(),
                width,
                height,
                yawRad,
                pitchRad,
                shaderFov,
                skyModule.getThunderInterval(),
                skyModule.getThunderChance(),
                skyModule.getThunderGlow()
        );

        FullScreenRenderer.ensureInit();
        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        MeshRenderer.setProjection(new Matrix4f());
        try {
            MeshRenderer.begin()
                    .attachments(framebuffer)
                    .pipeline(pipeline)
                    .mesh(FullScreenRenderer.vbo, FullScreenRenderer.ibo)
                    .uniform("ShaderSky", ShaderSkyUniforms.get())
                    .end();
        } finally {
            MeshRenderer.setProjection(worldProjection);
            mv.popMatrix();
        }
    }

    private static RenderPipeline pipelineForMode(String mode) {
        return switch (mode) {
            case "Pulsar" -> LoVisualRenderPipelines.WORLD_SHADER_SKY_PULSAR;
            default -> LoVisualRenderPipelines.WORLD_SHADER_SKY_THUNDER;
        };
    }
}
