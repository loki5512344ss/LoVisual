/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.postprocess.post;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.postprocess.graph.PostProcessGraph;
import dev.loki.lovisual.render.engine.profiler.frame.RenderCostProfiler;
import dev.loki.lovisual.render.engine.renderer.FullScreenRenderer;
import dev.loki.lovisual.render.engine.renderer.MeshRenderer;
import dev.loki.lovisual.render.engine.rhi.command.FullscreenDrawCommand;
import dev.loki.lovisual.render.engine.uniform.impl.post.PostProcessUniforms;
import dev.loki.lovisual.Lifecycle;

import java.util.OptionalDouble;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessPass;

public enum PostProcessManager {
    ;
    private static final PostProcessGraph GRAPH = new PostProcessGraph();
    private static GpuSampler sampler;

    public static void register(PostProcessPass pass) {
        GRAPH.addLegacy(pass);
    }

    public static PostProcessGraph graph() {
        return GRAPH;
    }

    public static void renderAll(PostProcessPass.Phase phase, float tickDelta) {
        if (!Lifecycle.canRunRender()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer.mainRenderTarget() == null) return;

        FullScreenRenderer.ensureInit();
        ensureSampler();

        var mv = RenderSystem.getModelViewStack();
        Matrix4f previousProjection = MeshRenderer.projection();
        GpuBufferSlice previousProjectionBuffer = RenderSystem.getProjectionMatrixBuffer();
        ProjectionType previousProjectionType = RenderSystem.getProjectionType();
        boolean previousRendering3D = RenderState.rendering3D;
        mv.pushMatrix();
        mv.identity();
        MeshRenderer.setProjection(new Matrix4f().identity());
        RenderState.rendering3D = false;

        try {
            GRAPH.execute(
                    phase,
                    tickDelta,
                    LoVisualRenderSystem.ensureFrameContext(),
                    LoVisualRenderSystem.rhi(),
                    PostProcessManager::copy
            );
        } finally {
            MeshRenderer.setProjection(previousProjection);
            if (previousProjectionBuffer != null && previousProjectionType != null) {
                RenderSystem.setProjectionMatrix(previousProjectionBuffer, previousProjectionType);
            }
            RenderState.rendering3D = previousRendering3D;
            mv.popMatrix();
        }
    }

    private static void ensureSampler() {
        if (sampler != null) return;
        sampler = RenderSystem.getDevice().createSampler(
                AddressMode.CLAMP_TO_EDGE,
                AddressMode.CLAMP_TO_EDGE,
                FilterMode.LINEAR,
                FilterMode.LINEAR,
                1,
                OptionalDouble.empty()
        );
    }

    public static GpuSampler getSampler() {
        ensureSampler();
        return sampler;
    }

    public static void copy(GpuTextureView src, GpuTextureView dst) {
        try (RenderCostProfiler.Scope ignored = RenderCostProfiler.postPass("copy")) {
            if (src == null || dst == null || src == dst) return;
            if (LoVisualRenderSystem.rhi().textureBlitter().copyFast(src, dst)) {
                return;
            }

            ensureSampler();
            PostProcessUniforms.update(0.0f, 0.0f, 0.0f, 0.0f);
            LoVisualRenderSystem.rhi().drawFullscreen(
                    FullscreenDrawCommand.builder("LoVisual PostProcess Copy")
                            .colorAttachment(dst)
                            .pipeline(LoVisualRenderPipelines.DAMAGE_TINT)
                            .uniform("PostProcess", PostProcessUniforms.get())
                            .sampler("u_Texture", src, sampler)
                            .build()
            );
            LoVisualRenderSystem.rhi().stats().textureShaderCopy();
        }
    }
}
