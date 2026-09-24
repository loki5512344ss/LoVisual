package dev.loki.lovisual.render.engine.renderer;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.core.frame.RenderFrameContext;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.engine.world.WorldCommandBuffer;
import dev.loki.lovisual.render.engine.world.WorldPassCompiler;
import dev.loki.lovisual.render.engine.world.stats.WorldRenderStatsSnapshot;

/**
 *
 * Renderer3D is the public world-primitive API only:
 * - modules still call line/tri/quad/batch exactly as before;
 * - calls record CPU-side world commands into WorldCommandBuffer;
 * - WorldPassCompiler converts commands to RHI draw commands;
 * - mesh upload/draw/depth/fog/visibility are delegated to context/RHI/providers.
 * Batch bindings ({@link Renderer3DBatchBindings}) and culling ({@link Renderer3DCulling},
 * {@link Renderer3DCullOptions}) live as sibling types in the same package.
 */
public final class Renderer3D {
    private static final String DEFAULT_SAMPLER = "u_Texture";
    private static final WorldPassCompiler WORLD_COMPILER = new WorldPassCompiler();

    private final RenderPipeline linesPipeline;
    private final RenderPipeline trisPipeline;
    private final WorldCommandBuffer commands = new WorldCommandBuffer();

    public Renderer3D(RenderPipeline linesPipeline, RenderPipeline trisPipeline) {
        this.linesPipeline = linesPipeline;
        this.trisPipeline = trisPipeline;
    }

    public static WorldRenderStatsSnapshot worldStatsSnapshot() {
        return WORLD_COMPILER.statsSnapshot();
    }

    /**
     * Call once per frame/phase before modules write geometry.
     */
    public void begin() {
        RenderFrameContext ctx = LoVisualRenderSystem.ensureFrameContext();
        WORLD_COMPILER.beginFrame(ctx.frameId());
        commands.beginFrame();
    }

    public MeshBuilder lines() {
        return commands.batch(linesPipeline, DepthMode.MAIN, RenderState.lineWidth, Renderer3DBatchBindings.none());
    }

    public MeshBuilder tris() {
        return commands.batch(trisPipeline, DepthMode.MAIN, 1.0f, Renderer3DBatchBindings.none());
    }

    public MeshBuilder batch(RenderPipeline pipeline) {
        return batch(pipeline, DepthMode.MAIN);
    }

    public MeshBuilder batch(RenderPipeline pipeline, DepthMode depthMode) {
        return batch(pipeline, depthMode, Renderer3DBatchBindings.none());
    }

    public MeshBuilder batch(RenderPipeline pipeline, DepthMode depthMode, Renderer3DBatchBindings bindings) {
        return commands.batch(pipeline, depthMode, RenderState.lineWidth, bindings);
    }

    public MeshBuilder batchTextured(RenderPipeline pipeline, Identifier textureId) {
        return batchTextured(pipeline, textureId, DepthMode.MAIN);
    }

    public MeshBuilder batchTextured(RenderPipeline pipeline, Identifier textureId, DepthMode depthMode) {
        return batch(pipeline, depthMode,
                Renderer3DBatchBindings.none().withSampler(DEFAULT_SAMPLER, textureId));
    }

    public MeshBuilder batchTextured(RenderPipeline pipeline,
                                     GpuTextureView view,
                                     GpuSampler sampler) {
        return batchTextured(pipeline, view, sampler, DepthMode.MAIN);
    }

    public MeshBuilder batchTextured(RenderPipeline pipeline,
                                     GpuTextureView view,
                                     GpuSampler sampler,
                                     DepthMode depthMode) {
        return batch(pipeline, depthMode,
                Renderer3DBatchBindings.none().withSampler(DEFAULT_SAMPLER, view, sampler));
    }

    public MeshBuilder batchTextured(RenderPipeline pipeline,
                                     AbstractTexture texture,
                                     DepthMode depthMode) {
        if (texture == null) return null;
        return batchTextured(pipeline, texture.getTextureView(), texture.getSampler(), depthMode);
    }

    // -----------------------
    // Convenience API
    // -----------------------

    public void line(double x1, double y1, double z1,
                     double x2, double y2, double z2,
                     int r, int g, int b, int a) {

        MeshBuilder mesh = lines();
        if (mesh == null) return;
        mesh.ensureLineCapacity();
        int i1 = mesh.vec3(x1, y1, z1).color(r, g, b, a).next();
        int i2 = mesh.vec3(x2, y2, z2).color(r, g, b, a).next();
        mesh.line(i1, i2);
    }

    public void lineGradient(double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             int argb1, int argb2) {
        int a1 = (argb1 >>> 24) & 255;
        int r1 = (argb1 >>> 16) & 255;
        int g1 = (argb1 >>> 8) & 255;
        int b1 = argb1 & 255;
        int a2 = (argb2 >>> 24) & 255;
        int r2 = (argb2 >>> 16) & 255;
        int g2 = (argb2 >>> 8) & 255;
        int b2 = argb2 & 255;

        MeshBuilder mesh = lines();
        if (mesh == null) return;
        mesh.ensureLineCapacity();
        int i1 = mesh.vec3(x1, y1, z1).color(r1, g1, b1, a1).next();
        int i2 = mesh.vec3(x2, y2, z2).color(r2, g2, b2, a2).next();
        mesh.line(i1, i2);
    }

    public void triangle(double x1, double y1, double z1,
                         double x2, double y2, double z2,
                         double x3, double y3, double z3,
                         int r, int g, int b, int a) {

        MeshBuilder mesh = tris();
        if (mesh == null) return;
        mesh.ensureTriCapacity();
        int i1 = mesh.vec3(x1, y1, z1).color(r, g, b, a).next();
        int i2 = mesh.vec3(x2, y2, z2).color(r, g, b, a).next();
        int i3 = mesh.vec3(x3, y3, z3).color(r, g, b, a).next();
        mesh.triangle(i1, i2, i3);
    }

    public void quad(double x1, double y1, double z1,
                     double x2, double y2, double z2,
                     double x3, double y3, double z3,
                     double x4, double y4, double z4,
                     int r, int g, int b, int a) {

        MeshBuilder mesh = tris();
        if (mesh == null) return;
        mesh.ensureQuadCapacity();
        int i1 = mesh.vec3(x1, y1, z1).color(r, g, b, a).next();
        int i2 = mesh.vec3(x2, y2, z2).color(r, g, b, a).next();
        int i3 = mesh.vec3(x3, y3, z3).color(r, g, b, a).next();
        int i4 = mesh.vec3(x4, y4, z4).color(r, g, b, a).next();
        mesh.quad(i1, i2, i3, i4);
    }

    // -----------------------
    // Submit
    // -----------------------

    /**
     * Submit recorded world commands to the current main framebuffer.
     */
    public void render(PoseStack matrices) {
        Minecraft mc = Minecraft.getInstance();
        WORLD_COMPILER.submit(commands, mc != null ? mc.gameRenderer.mainRenderTarget() : null, matrices);
    }

    public enum DepthMode {
        MAIN,
        PRE_DEPTH,
        NONE
    }

    @FunctionalInterface
    public interface UniformResolver {
        GpuBufferSlice resolve();
    }
}
