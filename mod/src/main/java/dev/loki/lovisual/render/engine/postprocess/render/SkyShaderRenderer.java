package dev.loki.lovisual.render.engine.postprocess.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.FullScreenRenderer;
import dev.loki.lovisual.render.engine.renderer.MeshRenderer;
import dev.loki.lovisual.render.engine.uniform.impl.sky.postprocess.SkyShaderNebulaUniforms;
import dev.loki.lovisual.render.engine.uniform.impl.sky.postprocess.SkyShaderUniforms;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Fullscreen SkyShader pass: sky / water / caustic procedural GLSL effect drawn behind the world,
 * with a camera-direction-driven ray so it reads correctly regardless of look direction.
 */
public enum SkyShaderRenderer {
    ;

    public enum Mode {
        SKY, WATER, CAUSTIC, NEBULA
    }

    private static final Matrix4f IDENTITY = new Matrix4f();
    private static ProjectionMatrixBuffer projection;
    private static long startMillis = -1L;

    public static void reset() {
        startMillis = -1L;
    }

    public static void render(Minecraft mc, Mode mode, float speed, float scale, float intensity,
                               float alpha, int colorRgb) {
        if (mc == null || mc.level == null || mc.player == null) return;

        RenderTarget fb = mc.gameRenderer.mainRenderTarget();
        if (fb == null) return;

        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        if (w <= 0 || h <= 0) return;

        FullScreenRenderer.ensureInit();

        if (startMillis < 0) startMillis = Util.getMillis();
        float time = (Util.getMillis() - startMillis) / 1000.0f;

        float cr = ((colorRgb >>> 16) & 0xFF) / 255f;
        float cg = ((colorRgb >>> 8) & 0xFF) / 255f;
        float cb = (colorRgb & 0xFF) / 255f;

        Camera cam = mc.gameRenderer.mainCamera();
        float yawRad = (float) Math.toRadians(-cam.yRot());
        float pitchRad = (float) Math.toRadians(cam.xRot());
        float fov = (float) mc.options.fov().get().intValue();

        SkyShaderUniforms.update(time, speed, scale, intensity, w, h, alpha, fov, cr, cg, cb, yawRad, pitchRad);

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
                projection = new ProjectionMatrixBuffer("lovisual-sky-shader-projection");
            }
            Matrix4f proj = IDENTITY.identity();
            RenderSystem.setProjectionMatrix(projection.getBuffer(proj), ProjectionType.PERSPECTIVE);
            MeshRenderer.setProjection(proj);
            RenderState.rendering3D = false;

            RenderPipeline pipeline = switch (mode) {
                case WATER -> LoVisualRenderPipelines.SKY_SHADER_WATER;
                case CAUSTIC -> LoVisualRenderPipelines.SKY_SHADER_CAUSTIC;
                default -> LoVisualRenderPipelines.SKY_SHADER_SKY;
            };

            FullScreenRenderer.begin("LoVisual Fullscreen Pass")
                    .attachment(fb)
                    .pipeline(pipeline)
                    .uniform("SkyShader", SkyShaderUniforms.get())
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

    /** Domain-warped nebula sky (quaternion-based ray dir instead of yaw/pitch). */
    public static void renderNebula(Minecraft mc, float speed, float intensity, float alpha,
                                    int nebColor1Rgb, int nebColor2Rgb, int starColorRgb) {
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

        SkyShaderNebulaUniforms.update(w, h, time, speed, intensity, alpha, fov,
                rot.x(), rot.y(), rot.z(), rot.w(),
                channelR(nebColor1Rgb), channelG(nebColor1Rgb), channelB(nebColor1Rgb),
                channelR(nebColor2Rgb), channelG(nebColor2Rgb), channelB(nebColor2Rgb),
                channelR(starColorRgb), channelG(starColorRgb), channelB(starColorRgb));

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
                projection = new ProjectionMatrixBuffer("lovisual-sky-shader-projection");
            }
            Matrix4f proj = IDENTITY.identity();
            RenderSystem.setProjectionMatrix(projection.getBuffer(proj), ProjectionType.PERSPECTIVE);
            MeshRenderer.setProjection(proj);
            RenderState.rendering3D = false;

            FullScreenRenderer.begin("LoVisual Fullscreen Pass")
                    .attachment(fb)
                    .pipeline(LoVisualRenderPipelines.SKY_SHADER_NEBULA)
                    .uniform("SkyShaderData", SkyShaderNebulaUniforms.get())
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

    private static float channelR(int rgb) {
        return ((rgb >>> 16) & 0xFF) / 255f;
    }

    private static float channelG(int rgb) {
        return ((rgb >>> 8) & 0xFF) / 255f;
    }

    private static float channelB(int rgb) {
        return (rgb & 0xFF) / 255f;
    }
}
