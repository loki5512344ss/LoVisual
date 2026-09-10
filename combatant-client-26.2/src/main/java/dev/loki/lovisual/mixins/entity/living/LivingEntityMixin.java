/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.entity.living;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.config.values.collection.ItemCooldownRulesValue;
import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.events.impl.player.PlayerJumpEvent;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.combat.pvp.PvpCooldowns;
import dev.loki.lovisual.features.module.modules.combat.pvp.TPSSync;
import dev.loki.lovisual.features.module.modules.visuals.visibility.NoRender;
import dev.loki.lovisual.features.module.modules.visuals.weather.WorldTweaks;
import dev.loki.lovisual.util.aiming.rotation.RotationManager;
import dev.loki.lovisual.util.aiming.data.Rotation;
import dev.loki.lovisual.util.aiming.features.MovementCorrection;
import dev.loki.lovisual.util.player.inventory.core.InventorySwap;
import dev.loki.lovisual.util.pvp.client.CooldownsState;
import dev.loki.lovisual.util.pvp.opponents.TotemPopCounter;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private float lovisual$itemUseRemainder = 0f;

    @Unique
    private float lovisual$effectTickRemainder = 0f;

    @Shadow
    protected int useItemRemaining;
    @Unique
    private PlayerJumpEvent lovisual$playerJumpEvent;

    @Shadow
    protected abstract float getJumpPower();

    @Inject(method = "getAttributeValue", at = @At("RETURN"))
    private void lovisual$noStunSlownessMovementSpeed(Holder<Attribute> attribute, CallbackInfoReturnable<Double> cir) {
        if (!attribute.equals(Attributes.MOVEMENT_SPEED)) return;
        return;

    }

    @ModifyReturnValue(method = "getMainHandItem", at = @At("RETURN"))
    private ItemStack lovisual$applyInventorySwapLeaseForMainHand(ItemStack original) {
        Minecraft mc = Minecraft.getInstance();
        if ((Object) this == mc.player) {
            InventorySwap.INSTANCE.isHotbarLeased();
        }

        return original;
    }

    /**
     * Optional local cooldown synthesis for servers that lock consumables without sending vanilla cooldown packets.
     * This never cancels item use; it only starts a visible local timer when explicitly configured.
     */
    @Inject(method = "updatingUsingItem", at = @At("HEAD"))
    private void lovisual$onFinishEating(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        Minecraft mc = Minecraft.getInstance();
        if (self != mc.player) return;

        ItemStack stack = self.getUseItem();
        if (stack == null || stack.isEmpty()) return;
        if (self.getUseItemRemainingTicks() != 1) return;

        PvpCooldowns cooldowns = Modules.get(PvpCooldowns.class);
        if (cooldowns == null) return;

        Item item = stack.getItem();
        cooldowns.tryStartLocalCooldown(item, ItemCooldownRulesValue.Trigger.CONSUME_FINISH);
    }

    @Inject(method = "updatingUsingItem", at = @At("HEAD"), cancellable = true)
    private void lovisual$fixItemUseSpeed(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) {
            lovisual$itemUseRemainder = 0f;
            return;
        }

        if (!player.level().isClientSide()) return;
        if (!player.isUsingItem()) {
            lovisual$itemUseRemainder = 0f;
            return;
        }

        float scale = lovisual$clientTickScale();
        if (scale >= 0.999f) {
            lovisual$itemUseRemainder = 0f;
            return;
        }

        lovisual$itemUseRemainder += scale;
        if (lovisual$itemUseRemainder < 1.0f) {
            ci.cancel();
            return;
        }

        lovisual$itemUseRemainder -= 1.0f;
    }

    @Redirect(
            method = "tickEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/effect/MobEffectInstance;tickClient()V"
            )
    )
    private void lovisual$scaleClientEffects(MobEffectInstance instance) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide()) {
            instance.tickClient();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (!(self instanceof Player player) || mc == null || player != mc.player) {
            instance.tickClient();
            return;
        }

        float scale = lovisual$clientTickScale();
        if (scale >= 0.999f) {
            lovisual$effectTickRemainder = 0f;
            instance.tickClient();
            return;
        }

        lovisual$effectTickRemainder += scale;
        if (lovisual$effectTickRemainder < 1.0f) {
            return;
        }
        lovisual$effectTickRemainder -= 1.0f;
        instance.tickClient();
    }

    @Unique
    private float lovisual$clientTickScale() {
        // Timer module removed; scale only by server TPS when active.
        float serverDelta = 1.0f;
        TPSSync tps = Modules.get(TPSSync.class);
        if (tps != null && tps.isEnabled()) {
            serverDelta = tps.getServerTickDelta();
        }

        if (serverDelta > 1.0f) return 1.0f;
        return Math.max(serverDelta, 0.0f);
    }

    /**
     * Local totem cooldowns are handled from the confirmed entity-status packet when explicitly configured.
     */
    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"))
    private void lovisual$onTotemUse(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        // no local use blocking or prediction here
    }

    /**
     * Сброс кулдаунов при смерти.
     */
    @Inject(method = "die", at = @At("HEAD"))
    private void lovisual$onDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player) {
            TotemPopCounter.onPlayerDeath(player.getUUID());
        }

        PvpCooldowns cooldowns = Modules.get(PvpCooldowns.class);
        if (cooldowns == null || cooldowns.isSystemEnabled()) {
            CooldownsState.MANAGER.clear();
        }
    }

    @Inject(
            method = "getBlockSpeedFactor",
            at = @At("HEAD"),
            cancellable = true
    )
    private void weathercontrol$dontDampRiptide(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof Player player)) return;
        if (!player.isAutoSpinAttack()) return;

        Level world = player.level();
        if (world == null) return;

        if (WorldTweaks.isServerRaining(world)) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void preventCrash(CallbackInfo ci) {
        // ElytraFly module removed; no crash-prevention override remains.
    }

    @ModifyExpressionValue(
            method = "updateFallFlyingMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private Vec3 lovisual$modifyGlidingRotationVector(Vec3 original) {
        LivingEntity self = (LivingEntity) (Object) this;
        Minecraft mc = Minecraft.getInstance();
        if (!(self instanceof LocalPlayer player) || mc == null || player != mc.player) {
            return original;
        }

        Rotation rotation = RotationManager.INSTANCE.getMovementRotation();
        var rotationTarget = RotationManager.INSTANCE.getActiveRotationTarget();
        if (rotationTarget == null
                || rotationTarget.movementCorrection == MovementCorrection.OFF
                || rotationTarget.movementCorrection == MovementCorrection.STRICT) {
            return original;
        }

        return rotation.directionVector();
    }

    @ModifyExpressionValue(
            method = "updateFallFlyingMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F"
            )
    )
    private float lovisual$modifyGlidingPitch(float original) {
        LivingEntity self = (LivingEntity) (Object) this;
        Minecraft mc = Minecraft.getInstance();
        if (!(self instanceof LocalPlayer player) || mc == null || player != mc.player) {
            return original;
        }

        Rotation rotation = RotationManager.INSTANCE.getMovementRotation();
        var rotationTarget = RotationManager.INSTANCE.getActiveRotationTarget();
        if (rotationTarget == null || rotationTarget.movementCorrection == MovementCorrection.OFF) {
            return original;
        }

        return rotation.pitch();
    }

    @Inject(method = "hasEffect", at = @At("HEAD"), cancellable = true)
    private void killHasEffect(Holder<MobEffect> effect,
                               CallbackInfoReturnable<Boolean> cir) {
        NoRender noRender = Modules.get(NoRender.class);
        if (noRender == null || !noRender.off("bad_effects")) return;

        if (isBad(effect)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getEffect", at = @At("HEAD"), cancellable = true)
    private void killGetEffect(Holder<MobEffect> effect,
                               CallbackInfoReturnable<MobEffectInstance> cir) {
        NoRender noRender = Modules.get(NoRender.class);
        if (noRender == null || !noRender.off("bad_effects")) return;

        if (isBad(effect)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void lovisual$onJumpHead(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof LocalPlayer player)) {
            lovisual$playerJumpEvent = null;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || player != mc.player) {
            lovisual$playerJumpEvent = null;
            return;
        }

        PlayerJumpEvent event = new PlayerJumpEvent(self.getYRot(), getJumpPower());
        Events.BUS.post(event);
        if (event.isCancelled()) {
            lovisual$playerJumpEvent = null;
            ci.cancel();
            return;
        }

        lovisual$playerJumpEvent = event;
    }

    @ModifyExpressionValue(
            method = "jumpFromGround",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getJumpPower()F"
            )
    )
    private float lovisual$modifyJumpMotion(float original) {
        return lovisual$playerJumpEvent != null ? lovisual$playerJumpEvent.getMotion() : original;
    }

    @ModifyExpressionValue(
            method = "jumpFromGround",
            at = @At(
                    value = "NEW",
                    target = "(DDD)Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private Vec3 lovisual$fixJumpRotation(Vec3 original) {
        LivingEntity self = (LivingEntity) (Object) this;
        Minecraft mc = Minecraft.getInstance();
        if (!(self instanceof LocalPlayer player) || mc == null || player != mc.player) {
            return original;
        }

        Rotation rotation = RotationManager.INSTANCE.getMovementRotation();
        var rotationTarget = RotationManager.INSTANCE.getActiveRotationTarget();
        if (rotationTarget == null || rotationTarget.movementCorrection == MovementCorrection.OFF) {
            return original;
        }

        float yaw = rotation.yaw() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw) * 0.2F, 0.0, Mth.cos(yaw) * 0.2F);
    }

    @Redirect(
            method = "jumpFromGround",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"
            )
    )
    private float lovisual$omniSprintJumpYaw(LivingEntity instance) {
        return lovisual$playerJumpEvent != null ? lovisual$playerJumpEvent.getYaw() : instance.getYRot();
    }

    @Inject(method = "jumpFromGround", at = @At("RETURN"))
    private void lovisual$onJumpReturn(CallbackInfo ci) {
        lovisual$playerJumpEvent = null;
    }

    @Unique
    private boolean isBad(Holder<MobEffect> entry) {
        MobEffect eff = entry.value();
        return eff == MobEffects.BLINDNESS.value()
                || eff == MobEffects.DARKNESS.value()
                || eff == MobEffects.NAUSEA.value();
    }

    @Inject(
            method = "getBlockSpeedFactor",
            at = @At("RETURN"),
            cancellable = true
    )
    private void nostun$soulSandVelocity(CallbackInfoReturnable<Float> cir) {
        // NoStun module removed; no velocity override remains.
    }

}
