package dev.loki.lovisual.mixins.optimize.gl.fbo;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.loki.lovisual.features.module.modules.misc.optimize.gl.RenderBackendOps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stage 3 step 2: vanilla attaches the depth texture to a scratch FBO,
 * clears, detaches and unbinds; a cached FBO (see {@code FrameBufferCacheMixin})
 * already holds the attachment, so the clear becomes one
 * {@code glClearNamedFramebufferfv}. Cancels the vanilla path when the
 * in-place clear ran.
 *
 *
 * <p>Gated on the {@code fbo_share} toggle via
 * {@link RenderBackendOps#clearDepthInPlace(int, double)}; a closed texture
 * or a missing DSA capability falls through to vanilla unchanged.
 */
@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public abstract class GlCommandEncoderDepthClearMixin {

    @Inject(method = "clearDepthTexture", at = @At("HEAD"), cancellable = true)
    private void lovisual$clearInPlace(GpuTexture depth, double value, CallbackInfo ci) {
        if (!(depth instanceof GlTexture gl) || gl.isClosed()) {
            return;
        }
        RenderBackendOps backend = RenderBackendOps.current();
        if (backend != null && backend.clearDepthInPlace(gl.glId(), value)) {
            ci.cancel();
        }
    }
}
