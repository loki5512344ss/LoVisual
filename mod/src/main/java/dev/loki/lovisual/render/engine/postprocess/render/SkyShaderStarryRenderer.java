package dev.loki.lovisual.render.engine.postprocess.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.features.module.modules.visuals.weather.WorldTweaks;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.sky.CustomSkyboxRenderer;
import dev.loki.lovisual.render.engine.renderer.FullScreenRenderer;
import dev.loki.lovisual.render.engine.renderer.MeshRenderer;
import dev.loki.lovisual.render.engine.uniform.impl.sky.postprocess.SkyShaderStarryUniforms;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Fullscreen "Starry" sky pass: procedural star field + milky way band + a round moon
 * placed opposite the sun (driven by the world day cycle). Mirrors the scaffolding of
 * {@link SkyShaderRenderer#renderNebula}; split out to keep both files within the size limit.
 */
public enum SkyShaderStarryRenderer {
    ;
    private static final Matrix4f IDENTITY = new Matrix4f();
    private static ProjectionMatrixBuffer projection;
    private static long startMillis = -1L;

    public static void reset() {
        startMillis = -1L;
    }

    public static void render(Minecraft mc, float speed, float intensity, float alpha,
                              float starDensity, float milkyWayIntensity, float moonSize,
                              boolean showMoon, int moonColorArgb, int starColorArgb) {
        if (mc == null || mc.level == null || mc.player == null) return;

        RenderTarget fb = mc.gameRenderer.mainRenderTarget();
        if (fb == null) return;

        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        if (w <= 0 || h <= 0) return;

        FullScreenRenderer.ensureInit();
        if (startMillis < 0) startMillis = Util.getMillis();
        float time = (Util.getMillis() - startMillis) / 1000.0f;

        Camera cam = mc.gameRenderer.mainCamera();
        Quaternionf rot = cam.rotation();
        float fov = (float) mc.options.fov().get().intValue();

        float[] moonDir = moonDirection(mc);
        if (!showMoon) {
            // The shader clamps the angular radius, so hiding the moon means pointing it
            // below the horizon where horizonFade discards it.
            moonDir = new float[]{0f, -1f, 0f};
        }
        float moonPhase = ((WorldTweaks.getServerTimeOfDay() / 24000f) % 8f) / 8f;

        SkyShaderStarryUniforms.update(w, h, time, speed, intensity, alpha, fov,
                rot.x(), rot.y(), rot.z(), rot.w(),
                moonDir[0], moonDir[1], moonDir[2], moonSize,
                moonPhase, milkyWayIntensity, starDensity,
                channel(moonColorArgb, 16), channel(moonColorArgb, 8), channel(moonColorArgb, 0),
                channel(starColorArgb, 16), channel(starColorArgb, 8), channel(starColorArgb, 0));

        var mv = RenderSystem.getModelViewStack();
        boolean pushedModelView = false;
        GpuBufferSlice prevProj = RenderSystem.getProjectionMatrixBuffer();
        ProjectionType prevProjType = RenderSystem.getProjectionType();
        Matrix4f prevMeshProjection = MeshRenderer.projection();
        boolean prevRendering3D = RenderState.rendering3D;

        try {
            mv.pushMatrix();
            pushedModelView = true;
            mv.identity();

            if (projection == null) {
                projection = new ProjectionMatrixBuffer("lovisual-sky-shader-starry-projection");
            }
            Matrix4f proj = IDENTITY.identity();
            RenderSystem.setProjectionMatrix(projection.getBuffer(proj), ProjectionType.PERSPECTIVE);
            MeshRenderer.setProjection(proj);
            RenderState.rendering3D = false;

            FullScreenRenderer.begin("LoVisual Fullscreen Pass")
                    .attachment(fb)
                    .pipeline(LoVisualRenderPipelines.SKY_SHADER_STARRY)
                    .uniform("SkyShaderStarry", SkyShaderStarryUniforms.get())
                    .end();
        } finally {
            if (pushedModelView) {
                mv.popMatrix();
            }
            MeshRenderer.setProjection(prevMeshProjection);
            RenderState.rendering3D = prevRendering3D;
            if (prevProj != null && prevProjType != null) {
                RenderSystem.setProjectionMatrix(prevProj, prevProjType);
            }
        }
    }

    /**
     * World-space direction to the moon: opposite the sun along its day arc
     * (CustomSkyboxRenderer sunDir = (-sin a, cos a, 0), so moon = (sin a, -cos a, 0)).
     * Sun angle comes from the last rendered sky pass; fallback derives it from the day clock.
     *
     * @return normalized xyz
     */
    private static float[] moonDirection(Minecraft mc) {
        float sunAngleRad;
        if (CustomSkyboxRenderer.hasLastSunAngle()) {
            sunAngleRad = CustomSkyboxRenderer.getLastSunAngle();
        } else {
            float timeOfDay = (WorldTweaks.getServerTimeOfDay() % 24000L) / 24000f;
            sunAngleRad = timeOfDay * (float) (Math.PI * 2.0);
        }
        float x = (float) Math.sin(sunAngleRad);
        float y = (float) -Math.cos(sunAngleRad);
        return new float[]{x, y, 0f};
    }

    private static float channel(int argb, int shift) {
        return ((argb >>> shift) & 0xFF) / 255f;
    }
}
