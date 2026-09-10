/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.entity.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.events.impl.movement.PlayerSafeWalkEvent;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.player.interact.NoDelay;
import dev.loki.lovisual.features.module.modules.combat.pvp.TPSSync;
import dev.loki.lovisual.mixininterface.entity.IPlayerAttackCooldown;
import dev.loki.lovisual.util.aiming.rotation.RotationManager;

@Mixin(Player.class)
public abstract class PlayerEntityMixin implements IPlayerAttackCooldown {

    @Shadow
    @Final
    private ItemCooldowns cooldowns;

    @Inject(method = "isStayingOnGroundSurface", at = @At("RETURN"), cancellable = true)
    private void lovisual$safeWalk(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if ((Object) this != Minecraft.getInstance().player) return;
        if (!Events.BUS.hasListeners(PlayerSafeWalkEvent.class)) return;

        PlayerSafeWalkEvent event = new PlayerSafeWalkEvent();
        Events.BUS.post(event);
        if (event.isSafeWalk()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isMobilityRestricted", at = @At("HEAD"), cancellable = true)
    private void lovisual$noStunBlindnessSprint(CallbackInfoReturnable<Boolean> cir) {
        // NoStun module removed; no mobility override remains.
    }

    @ModifyExpressionValue(
            method = "causeExtraKnockback",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"
            )
    )
    private float lovisual$fixKnockbackYaw(float original) {
        if ((Object) this != Minecraft.getInstance().player) {
            return original;
        }
        return RotationManager.INSTANCE.getMovementRotation().yaw();
    }

    @ModifyExpressionValue(
            method = "doSweepAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"
            )
    )
    private float lovisual$fixSweepYaw(float original) {
        if ((Object) this != Minecraft.getInstance().player) {
            return original;
        }
        return RotationManager.INSTANCE.getMovementRotation().yaw();
    }

    @ModifyExpressionValue(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private Vec3 lovisual$fixTravelRotationVector(Vec3 original) {
        if ((Object) this != Minecraft.getInstance().player) {
            return original;
        }
        return RotationManager.INSTANCE.getMovementRotation().directionVector();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void handleShieldCooldown(CallbackInfo ci) {
        NoDelay noDelay = Modules.get(NoDelay.class);
        if (noDelay == null || !noDelay.shouldDisableShieldCooldown()) return;

        cooldowns.removeCooldown(BuiltInRegistries.ITEM.getKey(Items.SHIELD));
    }

    /**
     * Синхронизация attack cooldown с серверным TPS.
     * НЕ трогает ItemCooldownManager.
     * НЕ влияет на shield.
     */
    @ModifyReturnValue(
            method = "getCurrentItemAttackStrengthDelay",
            at = @At("RETURN")
    )
    private float lovisual$tpsSyncAttackCooldown(float original) {
        float result = original;

        TPSSync tpsSync = Modules.get(TPSSync.class);
        if (tpsSync != null && tpsSync.isEnabled()) {
            float serverDelta = tpsSync.getServerTickDelta();
            if (serverDelta > 0.0f && serverDelta < 1.0f) {
                // замедляем заполнение cooldown’а под TPS
                result = result / serverDelta;
            }
        }

        return result;
    }

    @Override
    public float lovisual$getAttackCooldownProgress(float tickDelta) {
        Player self = (Player) (Object) this;
        return self.getAttackStrengthScale(tickDelta);
    }

    @Override
    public boolean lovisual$isAttackCharged(float tickDelta) {
        return lovisual$getAttackCooldownProgress(tickDelta) >= 1.0f;
    }
}
