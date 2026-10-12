package dev.loki.lovisual.mixins.optimize.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.loki.lovisual.features.module.modules.misc.optimize.gl.RenderBackendOps;
import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stage 3d: TBO cache — when {@code GlCommandEncoder.trySetup} binds a
 * texel-buffer texture via {@code glBindTexture}, we record the texture ID.
 * When {@code glTexBuffer(target, format, buffer)} is called for the same
 * {@code (format, buffer)} pair that's already attached, we skip the call —
 * re-attaching makes the driver revalidate the texture (clouds: ~2% of a
 * frame in {@code glDrawElements}).
 *
 *
 * <p>Gated on the {@code microOpts} toggle via
 * {@link RenderBackendOps#tboCacheSupported()}. When the toggle is off, both
 * {@code WrapOperation}s call the original.
 */
@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public abstract class GlCommandEncoderTexBufferMixin {

    @WrapOperation(
            method = "trySetup",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/GL33C;glBindTexture(II)V")
    )
    private void lovisual$trackBoundTboTexture(int target, int texture, Operation<Void> original) {
        RenderBackendOps backend = RenderBackendOps.current();
        if (backend != null && backend.tboCacheSupported()) {
            backend.trackBoundTboTexture(texture);
        }
        original.call(target, texture);
    }

    @WrapOperation(
            method = "trySetup",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/GL33C;glTexBuffer(III)V")
    )
    private void lovisual$texBufferOnce(int target, int format, int buffer, Operation<Void> original) {
        RenderBackendOps backend = RenderBackendOps.current();
        if (backend != null && backend.tboAlreadyAttached(format, buffer)) {
            // Same (format, buffer) pair already attached to the bound texture — skip the re-attach.
            return;
        }
        original.call(target, format, buffer);
        if (backend != null) {
            backend.markTboAttached(format, buffer);
        }
    }
}
