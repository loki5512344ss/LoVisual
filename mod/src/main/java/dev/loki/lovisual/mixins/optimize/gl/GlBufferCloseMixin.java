package dev.loki.lovisual.mixins.optimize.gl;

import dev.loki.lovisual.features.module.modules.misc.optimize.gl.RenderBackendOps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stage 3 step 2 (completes the TBO cache): only {@code GlBuffer.Direct}
 * deletes its GL handle; transient buffers are slices of a shared one. A
 * deleted buffer's id can be reused by a new buffer, so the attachment cache
 * must not outlive it — this hook clears the cache on every Direct close.
 *
 *
 * <p>Unconditional (clearing a tiny map on buffer close is free); correctness
 * beats the micro-saving of keeping stale attachments around.
 */
@Mixin(targets = "com.mojang.blaze3d.opengl.GlBuffer$Direct")
public abstract class GlBufferCloseMixin {

    @Inject(method = "close", at = @At("HEAD"))
    private void lovisual$forgetTboCache(CallbackInfo ci) {
        RenderBackendOps backend = RenderBackendOps.current();
        if (backend != null) {
            backend.forgetTboCache();
        }
    }
}
