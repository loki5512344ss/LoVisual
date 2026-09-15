/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.world.level;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.visibility.NoRender;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void lovisual$onEndCrystalRemoved(int entityId, Entity.RemovalReason reason, CallbackInfo ci) {
        if (!reason.shouldDestroy()) return;

        dev.loki.lovisual.features.module.modules.visuals.scene.EndCrystal module =
                Modules.get(dev.loki.lovisual.features.module.modules.visuals.scene.EndCrystal.class);
        if (module == null || !module.isEnabled()) return;

        Entity entity = ((ClientLevel) (Object) this).getEntity(entityId);
        if (entity instanceof EndCrystal crystal) {
            module.onCrystalDestroyed(crystal.position());
        }
    }

    @Inject(
            method = "doAddParticle(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void lovisual$noRenderEatParticles(
            ParticleOptions effect,
            boolean force,
            boolean canSpawnOnMinimal,
            double x, double y, double z,
            double vx, double vy, double vz,
            CallbackInfo ci
    ) {
        NoRender nr = Modules.get(NoRender.class);
        if (nr == null || !nr.isEnabled()) return;

        // частицы еды / питья
        if (nr.off("eat_particles")
                && effect instanceof ItemParticleOption) {
            ci.cancel();
        }

        if (nr.off("hit_particles")) {
            var type = effect.getType();
            if (type == ParticleTypes.DAMAGE_INDICATOR
                    || type == ParticleTypes.CRIT
                    || type == ParticleTypes.ENCHANTED_HIT) {
                ci.cancel();
            }
        }

        if (nr.off("sweep_particles")) {
            var type = effect.getType();
            if (type == ParticleTypes.SWEEP_ATTACK) {
                ci.cancel();
            }
        }
    }
}

