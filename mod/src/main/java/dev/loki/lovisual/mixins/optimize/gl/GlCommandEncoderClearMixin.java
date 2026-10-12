package dev.loki.lovisual.mixins.optimize.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.GlStateManager;
import dev.loki.lovisual.features.module.modules.misc.optimize.gl.RenderBackendOps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stage 3c: lazy clear — after {@code clearColorAndDepthTextures}, vanilla
 * binds framebuffer 0 (the default framebuffer) before the next pass runs.
 * The next pass binds its own target anyway, and {@code GlStateManager}'s
 * cache skips the redundant bind. This mixin drops the explicit bind-0 so
 * we don't pay for a {@code glBindFramebuffer(GL_FRAMEBUFFER, 0)} that's
 * immediately overwritten.
 *
 *
 * <p>Gated on the {@code microOpts} toggle via {@link RenderBackendOps#skipBindFramebufferZero(int)}.
 * When the toggle is off, the original call runs unchanged.
 */
@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public abstract class GlCommandEncoderClearMixin {

    @WrapOperation(
            method = "clearColorAndDepthTextures",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_glBindFramebuffer(II)V")
    )
    private void lovisual$skipBindZero(int target, int framebuffer, Operation<Void> original) {
        RenderBackendOps backend = RenderBackendOps.current();
        if (backend != null && backend.skipBindFramebufferZero(framebuffer)) {
            // Skip the bind — the next render pass binds its own target anyway.
            return;
        }
        original.call(target, framebuffer);
    }
}
