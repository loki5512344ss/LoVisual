/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.render.level;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla bug: FireworkParticles$Starter.createParticle assumes ParticleEngine.createParticle never returns null.
 * Our ParticleManagerMixin returns null when a ParticleSpawnEvent is cancelled (e.g. FpsOptimizer.disable_particles).
 * This causes NPE: sparkParticle is null -> setTrail() crashes.
 * Fix: guard every SparkParticle setter to be null-safe. Use Object for private inner type.
 */
@Mixin(targets = "net.minecraft.client.particle.FireworkParticles$Starter")
public abstract class FireworkParticlesStarterMixin {

    @WrapOperation(method = "createParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/FireworkParticles$SparkParticle;setTrail(Z)V"))
    private void lovisual$guardSetTrail(Object instance, boolean trail, Operation<Void> original) {
        if (instance == null) return;
        original.call(instance, trail);
    }

    @WrapOperation(method = "createParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/FireworkParticles$SparkParticle;setTwinkle(Z)V"))
    private void lovisual$guardSetTwinkle(Object instance, boolean twinkle, Operation<Void> original) {
        if (instance == null) return;
        original.call(instance, twinkle);
    }

    @WrapOperation(method = "createParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/FireworkParticles$SparkParticle;setAlpha(F)V"))
    private void lovisual$guardSetAlpha(Object instance, float alpha, Operation<Void> original) {
        if (instance == null) return;
        original.call(instance, alpha);
    }

    @WrapOperation(method = "createParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/FireworkParticles$SparkParticle;setColor(I)V"))
    private void lovisual$guardSetColor(Object instance, int color, Operation<Void> original) {
        if (instance == null) return;
        original.call(instance, color);
    }

    @WrapOperation(method = "createParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/FireworkParticles$SparkParticle;setFadeColor(I)V"))
    private void lovisual$guardSetFadeColor(Object instance, int fadeColor, Operation<Void> original) {
        if (instance == null) return;
        original.call(instance, fadeColor);
    }
}
