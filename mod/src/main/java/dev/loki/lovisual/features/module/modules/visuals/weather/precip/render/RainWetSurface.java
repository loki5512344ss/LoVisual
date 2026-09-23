package dev.loki.lovisual.features.module.modules.visuals.weather.precip;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.core.system.LoVisualWorldMatrices;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessContext;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessManager;
import dev.loki.lovisual.render.engine.renderer.FullScreenRenderer;
import dev.loki.lovisual.render.engine.uniform.impl.world.liquid.WetSurfaceUniforms;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Пресеты + wet-ground SSR pass.
 */
final class RainWetSurface {
    private RainWetSurface() {}

    static int reflectionSteps(Rain rain) {
        return switch (rain.reflectionQuality.get()) {
            case HIGH -> 16; case BALANCED -> 10; case PERFORMANCE -> 6;
        };
    }

    static int binarySteps(Rain rain) {
        return switch (rain.reflectionQuality.get()) {
            case HIGH -> 6; case BALANCED -> 4; case PERFORMANCE -> 3;
        };
    }

    static float rainAmount(Rain rain) {
        return Mth.clamp(0.55f + rain.density.get() * RainPhysics.presetDensity(rain) * 0.28f, 0.55f, 1.45f);
    }

    static boolean playerSeesSky(Rain rain, Minecraft mc) {
        if (!rain.skyCheck.get() || mc.player == null || mc.level == null) return true;
        try { return mc.level.canSeeSky(mc.player.blockPosition()); }
        catch (Throwable ignored) { return true; }
    }

    static boolean isActiveWet(Rain rain) {
        Minecraft mc = Minecraft.getInstance();
        return rain.isEnabled() && rain.wetGround.get() && rain.wetSurfaceDepthSupported
                && mc.player != null && mc.level != null
                && (!rain.onlyWhenRaining.get() || mc.level.isRaining())
                && playerSeesSky(rain, mc);
    }

    static boolean renderWet(Rain rain, PostProcessContext ctx, GpuTextureView src, GpuTextureView dst) {
        if (!isActiveWet(rain) || ctx == null || src == null || dst == null) return false;
        GpuTextureView mainDepth = ctx.mainDepth();
        boolean hasDepth = mainDepth != null;
        if (!hasDepth) mainDepth = src;
        Matrix4f projection = LoVisualWorldMatrices.renderProjectionMatrix();
        if (projection == null) projection = RenderState.worldProjection;
        rain.wetSurfaceProjection.set(projection);
        rain.wetSurfaceInvView.set(new Matrix4f().rotation(RenderState.cameraRotation));
        WetSurfaceUniforms.update(
                rain.wetSurfaceProjection, rain.wetSurfaceInvView,
                (float) RenderState.cameraPos.x, (float) RenderState.cameraPos.y, (float) RenderState.cameraPos.z,
                ctx.width() > 0 ? 1.0f / ctx.width() : 0.0f, ctx.height() > 0 ? 1.0f / ctx.height() : 0.0f,
                rain.clock, hasDepth, rain.wetness.get(), rain.puddleCoverage.get(),
                rain.reflectionStrength.get(), rain.puddleRipples.get() ? rain.rippleStrength.get() : 0.0f,
                rain.reflectionDistance.get(), reflectionSteps(rain), rainAmount(rain),
                rain.puddleRipples.get(), binarySteps(rain), 0.18f);
        try {
            FullScreenRenderer.ensureInit();
            FullScreenRenderer.begin("LoVisual WetSurface Pass").attachment(dst)
                    .pipeline(LoVisualRenderPipelines.WET_SURFACE).uniform("WetSurface", WetSurfaceUniforms.get())
                    .sampler("u_Texture", src, PostProcessManager.getSampler())
                    .sampler("u_MainDepth", mainDepth, PostProcessManager.getSampler()).end();
        } catch (Throwable t) { rain.wetSurfaceDepthSupported = false; return false; }
        return true;
    }
}
