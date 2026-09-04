/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.render.engine.core.LoVisualRenderSystem;
import dev.loki.lovisual.util.logging.DebugLog;

@Mixin(RenderPass.class)
public abstract class RenderPassMixin {
    @Inject(method = "setPipeline", at = @At("RETURN"))
    private void lovisual$applyRhiPipelineState(RenderPipeline pipeline, CallbackInfo ci) {
        try {
            LoVisualRenderSystem.rhi().pipelineState().applyPipelineState(pipeline);
        } catch (Throwable t) {
            DebugLog.error("[ShapeClip] pipeline state hook failed for " + pipeline, t);
        }
    }
}
