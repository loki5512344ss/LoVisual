/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.rhi.backend.gl.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.loki.lovisual.mixininterface.IRenderPipeline;
import dev.loki.lovisual.render.engine.rhi.clip.ShapeClipBackend;
import dev.loki.lovisual.render.engine.rhi.clip.ShapeClipRenderPassContract;
import dev.loki.lovisual.render.engine.rhi.msaa.MsaaControl;
import dev.loki.lovisual.render.engine.rhi.state.PipelineStateBackend;

import static org.lwjgl.opengl.GL11C.*;

/**
 * GL implementation for out-of-band RenderPipeline state.
 *
 * <p>GL_LINE_SMOOTH, MSAA and shape-clip render-pass state are applied through RHI. The command
 * encoder mixin owns only the hook points, not the GL policy.</p>
 */
public final class SodiumGlPipelineStateBackend implements PipelineStateBackend {
    private final MsaaControl msaa;
    private final ShapeClipBackend clip;

    public SodiumGlPipelineStateBackend(MsaaControl msaa, ShapeClipBackend clip) {
        this.msaa = msaa;
        this.clip = clip;
    }

    private static void applyLineSmooth(RenderPipeline pipeline) {
        boolean enabled = pipeline instanceof IRenderPipeline lovisualPipeline && lovisualPipeline.lovisual$getLineSmooth();
        if (enabled) {
            glEnable(GL_LINE_SMOOTH);
            // Wide lines are expanded into geometry before draw submission. Keep native GL lines at 1px: relying
            // on glLineWidth for thickness is undefined on modern core-profile drivers and produces GL_INVALID_VALUE
            // on common AMD/Mesa paths where the supported aliased line range is effectively [1, 1].
            glLineWidth(1.0f);
        } else {
            resetLineSmooth();
        }
    }

    private static void resetLineSmooth() {
        glDisable(GL_LINE_SMOOTH);
        glLineWidth(1.0f);
    }

    @Override
    public void applyPipelineState(RenderPipeline pipeline) {
        applyLineSmooth(pipeline);
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
        resetLineSmooth();
        msaa.resetPipelineState();
        clip.endRenderPass();
    }
}
