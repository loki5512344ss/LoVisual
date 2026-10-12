package dev.loki.lovisual.mixins.render.level.particles;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/**
 * Vanilla bug: FireworkParticles$Starter.createParticle assumes ParticleEngine.createParticle never returns null.
 * Our ParticleManagerMixin returns null when a ParticleSpawnEvent is cancelled (e.g. Optimize.no_particles).
 * This causes NPE: sparkParticle is null -> setTrail() crashes.
 * Fix: guard every SparkParticle setter to be null-safe. SparkParticle is a private inner class, so we use Object
 * with @Coerce to avoid InvalidInjectionException (expected SparkParticle vs Object).
 */
@Mixin(targets = "net.minecraft.client.particle.FireworkParticles$Starter")
public abstract class FireworkParticlesStarterMixin {

    @WrapOperation(method = "createParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/FireworkParticles$SparkParticle;setTrail(Z)V"))
    private void lovisual$guardSetTrail(@Coerce Object instance, boolean trail, Operation<Void> original) {
        if (instance == null) return;
        original.call(instance, trail);
    }

    @WrapOperation(method = "createParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/FireworkParticles$SparkParticle;setTwinkle(Z)V"))
    private void lovisual$guardSetTwinkle(@Coerce Object instance, boolean twinkle, Operation<Void> original) {
        if (instance == null) return;
        original.call(instance, twinkle);
    }

    @WrapOperation(method = "createParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/FireworkParticles$SparkParticle;setAlpha(F)V"))
    private void lovisual$guardSetAlpha(@Coerce Object instance, float alpha, Operation<Void> original) {
        if (instance == null) return;
        original.call(instance, alpha);
    }

    @WrapOperation(method = "createParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/FireworkParticles$SparkParticle;setColor(I)V"))
    private void lovisual$guardSetColor(@Coerce Object instance, int color, Operation<Void> original) {
        if (instance == null) return;
        original.call(instance, color);
    }

    @WrapOperation(method = "createParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/FireworkParticles$SparkParticle;setFadeColor(I)V"))
    private void lovisual$guardSetFadeColor(@Coerce Object instance, int fadeColor, Operation<Void> original) {
        if (instance == null) return;
        original.call(instance, fadeColor);
    }
}
