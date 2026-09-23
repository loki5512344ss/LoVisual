/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.render.engine.rhi.command.FullscreenDrawCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.rhi.resource.GpuMeshHandle;
import dev.loki.lovisual.render.engine.rhi.resource.MeshOwnership;
import dev.loki.lovisual.render.engine.rhi.command.RhiDrawCommand;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

import java.util.HashMap;
import dev.loki.lovisual.render.engine.uniform.impl.world.MeshUniforms;

/**
 * RenderPass-based mesh renderer for 26.2 GPU API.
 * - Uploads MeshBuilder to immediate GPU buffers.
 * - Creates RenderPass with color/depth attachments.
 * - Writes MeshData UBO (ProjMat + ModelViewMat) via MeshUniforms.
 */
public final class MeshRenderer {
    private static final MeshRenderer INSTANCE = new MeshRenderer();
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static boolean taken;
    private final HashMap<String, GpuBufferSlice> uniforms = new HashMap<>();
    private final HashMap<String, SamplerBinding> samplers = new HashMap<>();
    private @Nullable GpuTextureView colorAttachment;
    private @Nullable GpuTextureView depthAttachment;
    private @Nullable Integer clearColorArgb; // OptionalInt in renderpass
    private @Nullable RenderPipeline pipeline;
    private @Nullable MeshBuilder mesh;
    private @Nullable GpuBuffer vertexBuffer;
    private @Nullable GpuBuffer indexBuffer;
    private @Nullable Matrix4f transform;

    private MeshRenderer() {
    }

    /**
     * Set per-frame projection matrix used for MeshData UBO. Call from render hook once per frame.
     */
    public static void setProjection(Matrix4f projection) {
        PROJECTION.set(projection);
    }

    public static Matrix4f projection() {
        return new Matrix4f(PROJECTION);
    }

    public static MeshRenderer begin() {
        if (taken) throw new IllegalStateException("Previous MeshRenderer.begin() was not ended.");
        taken = true;
        return INSTANCE;
    }

    /**
     * Apply camera Y translation in MV (X/Z handled in MeshBuilder for precision).
     */
    private static void applyCameraPosY(Matrix4fStack mv) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        Vec3 cameraPos = mc.gameRenderer.mainCamera().position();
        mv.translate(0.0f, (float) -cameraPos.y, 0.0f);
    }

    public MeshRenderer attachments(GpuTextureView color, @Nullable GpuTextureView depth) {
        this.colorAttachment = color;
        this.depthAttachment = depth;
        return this;
    }

    public MeshRenderer attachments(RenderTarget framebuffer) {
        this.colorAttachment = framebuffer.getColorTextureView();
        this.depthAttachment = framebuffer.getDepthTextureView();
        return this;
    }

    /**
     * Optional clear (ARGB). If null -> no clear.
     */
    public MeshRenderer clearColor(@Nullable Integer argb) {
        this.clearColorArgb = argb;
        return this;
    }

    public MeshRenderer pipeline(RenderPipeline pipeline) {
        this.pipeline = pipeline;
        return this;
    }

    public MeshRenderer mesh(GpuBuffer vertices, GpuBuffer indices) {
        this.vertexBuffer = vertices;
        this.indexBuffer = indices;
        return this;
    }

    public MeshRenderer mesh(MeshBuilder mesh) {
        this.mesh = mesh;
        return this;
    }

    public MeshRenderer mesh(MeshBuilder mesh, Matrix4f matrix) {
        this.mesh = mesh;
        return this.transform(matrix);
    }

    public MeshRenderer mesh(MeshBuilder mesh, PoseStack matrices) {
        this.mesh = mesh;
        return this.transform(matrices.last().pose());
    }

    public MeshRenderer transform(@Nullable Matrix4f matrix) {
        this.transform = matrix;
        return this;
    }

    public MeshRenderer uniform(String name, GpuBufferSlice slice) {
        if (name != null && slice != null) uniforms.put(name, slice);
        return this;
    }

    public MeshRenderer sampler(String name, GpuTextureView view, GpuSampler sampler) {
        if (name != null && view != null && sampler != null) samplers.put(name, new SamplerBinding(view, sampler));
        return this;
    }

    public void end() {
        GpuMeshHandle uploaded = null;
        try {
            if (pipeline == null) return;
            if (colorAttachment == null) return;

            if (mesh != null && mesh.isBuilding()) mesh.end();

            final int indexCount;
            if (mesh != null) {
                indexCount = mesh.getIndicesCount();
            } else if (indexBuffer != null) {
                indexCount = (int) (indexBuffer.size() / Integer.BYTES);
            } else {
                indexCount = 0;
            }

            if (indexCount <= 0) return;

            if (mesh != null && FullScreenRenderer.isLegacyFullscreenMesh(mesh) && depthAttachment == null && transform == null) {
                FullscreenDrawCommand.Builder fullscreen =
                        FullscreenDrawCommand.builder("LoVisual MeshRenderer Fullscreen Compatibility")
                                .pipeline(pipeline)
                                .colorAttachment(colorAttachment)
                                .clearColor(clearColorArgb);
                for (var e : uniforms.entrySet()) {
                    fullscreen.uniform(e.getKey(), e.getValue());
                }
                for (var e : samplers.entrySet()) {
                    SamplerBinding b = e.getValue();
                    fullscreen.sampler(e.getKey(), b.view, b.sampler);
                }
                LoVisualRenderSystem.rhi().drawFullscreen(fullscreen.build());
                return;
            }

            if (mesh != null) {
                uploaded = LoVisualRenderSystem.rhi().dynamicMeshes().upload(mesh);
            } else if (vertexBuffer != null && indexBuffer != null) {
                uploaded = new GpuMeshHandle(vertexBuffer, indexBuffer, 0L, 0, indexCount, com.mojang.blaze3d.IndexType.INT, MeshOwnership.EXTERNAL);
            } else {
                return;
            }

            RhiDrawCommand.Builder command = RhiDrawCommand.builder("LoVisual MeshRenderer")
                    .pipeline(pipeline)
                    .colorAttachment(colorAttachment)
                    .depthAttachment(depthAttachment)
                    .clearColor(clearColorArgb)
                    .mesh(uploaded)
                    .transform(transform)
                    .applyWorldCameraY(RenderState.rendering3D);

            for (var e : uniforms.entrySet()) {
                command.uniform(e.getKey(), e.getValue());
            }
            for (var e : samplers.entrySet()) {
                SamplerBinding b = e.getValue();
                command.sampler(e.getKey(), b.view, b.sampler);
            }

            LoVisualRenderSystem.rhi().drawMesh(command.build());
            uploaded = null; // backend owns release for this draw
        } finally {
            if (uploaded != null) uploaded.close();
            // reset instance state
            colorAttachment = null;
            depthAttachment = null;
            clearColorArgb = null;
            pipeline = null;
            mesh = null;
            vertexBuffer = null;
            indexBuffer = null;
            transform = null;
            uniforms.clear();
            samplers.clear();
            taken = false;
        }
    }

    private record SamplerBinding(GpuTextureView view, GpuSampler sampler) {
    }
}
