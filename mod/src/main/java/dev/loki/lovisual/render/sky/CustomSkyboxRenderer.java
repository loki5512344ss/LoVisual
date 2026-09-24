package dev.loki.lovisual.render.sky;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.scene.visual.ReimaginedVisual;
import dev.loki.lovisual.features.module.modules.visuals.weather.sky.ShaderSky;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.renderer.MeshRenderer;

/**
 * Custom sky renderer entry point. Orchestrates the vanilla sky disc, the optional Reimagined shader
 * skybox / procedural ShaderSky passes and the shader sun, restoring vanilla sun/moon and dark-disc
 * rendering for whatever it does not replace.
 *
 * <p>Projection math lives in {@code impl.SkyboxProjection}, the fullscreen sky passes in
 * {@code impl.SkyboxShaderPasses} and the sun billboard in {@code impl.SkyboxSunRenderer}.</p>
 */
public enum CustomSkyboxRenderer {
    ;
    private static final float MIN_TWILIGHT_SKYBOX_ALPHA = 0.38f;
    private static float lastSunAngle;
    private static boolean lastSunAngleValid;

    public static float getLastSunAngle() {
        return lastSunAngle;
    }

    public static boolean hasLastSunAngle() {
        return lastSunAngleValid;
    }

    public static void render(Camera camera, GpuBufferSlice fog, SkyRenderState sky, SkyRenderer skyRendering) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null || skyRendering == null) return;
        lastSunAngle = sky.sunAngle;
        lastSunAngleValid = true;

        RenderSystem.setShaderFog(fog);
        skyRendering.renderSkyDisc(sky.skyColor);

        DeltaTracker tickCounter = mc.getDeltaTracker();
        float tickDelta = tickCounter != null ? tickCounter.getGameTimeDeltaPartialTick(true) : 0.0f;

        float fov = camera.getFov();
        Matrix4f projection = SkyboxProjection.build(mc, camera, fov, tickDelta);
        Matrix4f previousProjection = MeshRenderer.projection();
        MeshRenderer.setProjection(projection);

        boolean prevRendering3D = RenderState.rendering3D;
        RenderState.rendering3D = false;

        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        Matrix4f camRot = new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf()));
        mv.mul(camRot);
        float twilightFade = getTwilightFade(sky);
        float skyboxAlpha = Mth.lerp(twilightFade, 1.0f, MIN_TWILIGHT_SKYBOX_ALPHA);

        ReimaginedVisual visual = Modules.get(ReimaginedVisual.class);
        ShaderSky skyModule = Modules.get(ShaderSky.class);
        boolean useShaderSky = skyModule != null && skyModule.isEnabled();
        boolean useShaderSkybox = !useShaderSky && visual != null && visual.isShaderSkyboxEnabled();

        if (useShaderSky) {
            SkyboxShaderPasses.renderSky(mc, mc.gameRenderer.mainRenderTarget(), camera, Theme.theme().accent() & 0x00FFFFFF, skyboxAlpha, tickDelta, fov, projection);
        } else if (useShaderSkybox) {
            SkyboxShaderPasses.renderSkybox(mc, mc.gameRenderer.mainRenderTarget(), camera, fog, sky, skyboxAlpha, tickDelta, fov, projection);
        }
        if (SkyboxSunRenderer.shouldRender()) {
            SkyboxSunRenderer.render(mc, mc.gameRenderer.mainRenderTarget(), camera, sky, tickDelta);
        }

        mv.popMatrix();
        MeshRenderer.setProjection(previousProjection);
        RenderState.rendering3D = prevRendering3D;

        if (!SkyboxSunRenderer.shouldRender()) {
            PoseStack skyMatrices = new PoseStack();
            skyRendering.renderSunriseAndSunset(skyMatrices, sky.sunAngle, sky.sunriseAndSunsetColor);
        }
        if (sky.shouldRenderDarkDisc) {
            skyRendering.renderDarkDisc();
        }
    }

    private static float smoothstep(float x) {
        float t = Mth.clamp((x - (float) 0.0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static float getTwilightFade(SkyRenderState sky) {
        float sunriseAlpha = ARGB.alphaFloat(sky.sunriseAndSunsetColor);
        return smoothstep(sunriseAlpha);
    }
}
