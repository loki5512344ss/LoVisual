/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.rhi.backend.vulkan.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.loki.lovisual.mixininterface.IRenderPipeline;
import dev.loki.lovisual.render.engine.rhi.clip.ShapeClipBackend;
import dev.loki.lovisual.render.engine.rhi.clip.ShapeClipRenderPassContract;
import dev.loki.lovisual.render.engine.rhi.msaa.MsaaControl;
import dev.loki.lovisual.render.engine.rhi.state.PipelineStateBackend;

/**
 * Vulkan pipeline state bridge. Fixed Vulkan state is injected while pipelines are compiled.
 */
public final class VulkanPipelineStateBackend implements PipelineStateBackend {
    private final MsaaControl msaa;
    private final ShapeClipBackend clip;

    public VulkanPipelineStateBackend(MsaaControl msaa, ShapeClipBackend clip) {
        this.msaa = msaa;
        this.clip = clip;
    }

    @Override
    public void applyPipelineState(RenderPipeline pipeline) {
        msaa.applyPipelineState(pipeline);

        ShapeClipRenderPassContract contract = ShapeClipRenderPassContract.NONE;
        if (pipeline instanceof IRenderPipeline lovisualPipeline) {
            contract = lovisualPipeline.lovisual$getShapeClipContract();
        }
        clip.bindPipeline(pipeline, contract);
        clip.applyNativeState();
    }

    @Override
    public void resetRenderPassState() {
        msaa.resetPipelineState();
        clip.endRenderPass();
    }
}
