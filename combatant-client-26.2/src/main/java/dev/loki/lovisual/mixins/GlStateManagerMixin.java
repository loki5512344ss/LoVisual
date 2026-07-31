/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins;

import com.mojang.blaze3d.opengl.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.render.engine.profiler.GlSyncTracker;

@Mixin(GlStateManager.class)
public class GlStateManagerMixin {
    @Inject(method = "_glFenceSync", at = @At("RETURN"))
    private static void lovisual$onFenceSync(int condition, int flags, CallbackInfoReturnable<Long> cir) {
        GlSyncTracker.onFence(cir.getReturnValue());
    }

    @Inject(method = "_glClientWaitSync", at = @At("HEAD"))
    private static void lovisual$onClientWaitSync(long sync, int flags, long timeout, CallbackInfoReturnable<Integer> cir) {
        GlSyncTracker.onWaitStart(sync);
    }

    @Inject(method = "_glClientWaitSync", at = @At("RETURN"))
    private static void lovisual$onClientWaitSyncReturn(long sync, int flags, long timeout, CallbackInfoReturnable<Integer> cir) {
        GlSyncTracker.onWaitEnd();
    }
}
