package dev.loki.lovisual.mixins.optimize.gl.fbo;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.FrameBufferAttachment;
import com.mojang.blaze3d.opengl.FrameBufferCache;
import dev.loki.lovisual.render.engine.optimize.gl.SharedDepthFbos;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Stage 3 step 2: feeds {@link SharedDepthFbos} from the vanilla FBO cache.
 * {@code getFbo} records "this depth texture lives in that FBO" (mip 0 only —
 * higher mips are never the depth attachment of the cached FBO);
 * {@code destroyFbo} drops the mapping before the FBO is deleted, because its
 * GL id may be reused by the next one.
 *
 *
 * <p>Bookkeeping only — runs regardless of the {@code fbo_share} toggle (a
 * map put/remove per FBO create/destroy is free); the toggle gates the clear
 * path in {@code GlCommandEncoderDepthClearMixin}.
 */
@Mixin(FrameBufferCache.class)
public abstract class FrameBufferCacheMixin {

    @Shadow
    @Final
    private Object2IntMap<FrameBufferCache.CacheKey> cache;

    @Inject(method = "getFbo", at = @At("RETURN"))
    private void lovisual$rememberDepth(DirectStateAccess dsa,
                                        List<FrameBufferAttachment> colors,
                                        FrameBufferAttachment depth,
                                        CallbackInfoReturnable<Integer> cir) {
        if (depth != null && depth.fboMipLevel() == 0) {
            SharedDepthFbos.remember(depth.glId(), cir.getReturnValueI());
        }
    }

    @Inject(method = "destroyFbo", at = @At("HEAD"))
    private void lovisual$forget(FrameBufferCache.CacheKey key, CallbackInfo ci) {
        if (cache.containsKey(key)) {
            SharedDepthFbos.forget(cache.getInt(key));
        }
    }
}
