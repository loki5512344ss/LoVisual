/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins;

import com.mojang.blaze3d.opengl.GlTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.mixininterface.IMsaaTexture;
import dev.loki.lovisual.render.engine.core.LoVisualRenderSystem;

@Mixin(GlTexture.class)
public abstract class GlTextureMixin implements IMsaaTexture {
    @Final
    @Shadow
    protected int id;

    @Unique
    private int lovisual$samples = 1;

    @Override
    public void lovisual$setSamples(int samples) {
        lovisual$samples = samples;
    }

    @Override
    public int lovisual$getSamples() {
        return lovisual$samples;
    }

    @Override
    public boolean lovisual$isMsaa() {
        return lovisual$samples > 1;
    }

    @Override
    public int lovisual$getGlId() {
        return id;
    }

    @Inject(method = "destroyImmediately", at = @At("HEAD"))
    private void lovisual$onFree(CallbackInfo ci) {
        LoVisualRenderSystem.rhi().msaa().unregisterTexture(id);
    }
}

