/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import dev.loki.lovisual.render.engine.core.ViewportContext;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.MeshRenderer;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.Renderer2D.Deferred2DLayer;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

public record DeferredTexturedSubmit(Deferred2DLayer layer, ViewportContext viewport, int[] framebufferScissor, String samplerName,
                                    GpuTextureView samplerView, GpuSampler sampler, MeshBuilder mesh) implements Deferred2DSubmit {
    @Override
    public void submit() {
        if (mesh == null || samplerView == null || sampler == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null) return;
        RenderTarget fb = mc.gameRenderer.mainRenderTarget();
        if (fb == null) return;
        MeshRenderer.begin()
                .attachments(fb.getColorTextureView(), null)
                .pipeline(LoVisualRenderPipelines.UI_TEXTURED)
                .mesh(mesh)
                .sampler(samplerName != null ? samplerName : "u_Texture", samplerView, sampler)
                .end();
        Renderer2D.flushUiLayer();
    }

    @Override
    public void release() {
        if (mesh != null) {
            mesh.close();
        }
    }
}
