/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.entity.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.events.impl.input.SprintControlEvent;
import dev.loki.lovisual.events.impl.movement.MovementInputEvent;
import dev.loki.lovisual.events.impl.network.EventPostSync;
import dev.loki.lovisual.events.impl.network.EventSync;
import dev.loki.lovisual.events.impl.player.PostPlayerUpdateEvent;
import dev.loki.lovisual.events.impl.world.EventPushOutOfBlocks;
import dev.loki.lovisual.util.combat.rotation.SprintController;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.camera.Freecam;
import dev.loki.lovisual.mixininterface.entity.ILocalPlayer;
import dev.loki.lovisual.mixins.accessors.entity.EntityAccessor;
import dev.loki.lovisual.mixins.accessors.misc.InputAccessor;
import dev.loki.lovisual.mixins.accessors.entity.LocalPlayerAccessor;
import dev.loki.lovisual.util.aiming.rotation.RotationManager;
import dev.loki.lovisual.util.aiming.data.Rotation;

import java.util.ArrayList;
import java.util.List;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin implements ILocalPlayer {

    @Shadow
    public ClientInput input;
    @Unique
    private float lovisual$lastYaw;
    @Unique
    private float lovisual$lastPitch;
    @Unique
    private double lovisual$rotationPrevX;
    @Unique
    private double lovisual$rotationPrevZ;
    @Unique
    private float lovisual$rotationPrevBodyYaw;
    @Unique
    private boolean lovisual$rotationBodyYawInitialized;
    @Unique
    private float freecam$lastHealth = Float.NaN;
    @Unique
    private boolean frozen = false;
    @Unique
    private Vec3 savedVelocity = Vec3.ZERO;
    @Unique
    private double savedX, savedY, savedZ;
    @Unique
    private float savedBodyYaw;
    @Unique
    private float savedHeadYaw;
    @Unique
    private Runnable lovisual$eventPostAction;
    @Unique
    private EventSync lovisual$activeSyncEvent;
    @Unique
    private boolean lovisual$postUpdateLock = false;
    @Unique
    private Input lastInput;
    @Unique
    private boolean pendingSpoof = false;
    @Unique
    private int boostTicks = 0;
    @Unique
    private double boostX = 0, boostZ = 0;
    @Unique
    private boolean wasGround = true;

    @Unique
    private static boolean lovisual$sameInput(Input a, Input b) {
        if (a == b) return true;
        if (a == null || b == null) return false;

        return a.forward() == b.forward()
                && a.backward() == b.backward()
                && a.left() == b.left()
                && a.right() == b.right()
                && a.jump() == b.jump()
                && a.shift() == b.shift()
                && a.sprint() == b.sprint();
    }

    @Unique
    private static float lovisual$impulse(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0f;
        }
        return positive ? 1.0f : -1.0f;
    }

    @Unique
    private static float lovisual$simulateBodyYaw(
            float yaw,
            float prevBodyYaw,
            double prevX,
            double prevZ,
            double currentX,
            double currentZ,
            float handSwingProgress
    ) {
        double motionX = currentX - prevX;
        double motionZ = currentZ - prevZ;
        float motionSquared = (float) (motionX * motionX + motionZ * motionZ);
        float bodyYaw = prevBodyYaw;

        if (motionSquared > 0.0025000002F) {
            float movementYaw = (float) Mth.atan2(motionZ, motionX) * Mth.RAD_TO_DEG - 90.0F;
            float yawDiff = Math.abs(Mth.wrapDegrees(yaw) - movementYaw);
            bodyYaw = (yawDiff > 95.0F && yawDiff < 265.0F) ? movementYaw - 180.0F : movementYaw;
        }

        if (handSwingProgress - 0.2F > 0.0F) {
            bodyYaw = yaw;
        }

        float deltaYaw = Mth.wrapDegrees(bodyYaw - prevBodyYaw);
        bodyYaw = prevBodyYaw + deltaYaw * 0.3F;

        float yawOffsetDiff = Mth.wrapDegrees(yaw - bodyYaw);
        float maxHeadRotation = 52.0F;
        if (Math.abs(yawOffsetDiff) > maxHeadRotation) {
            bodyYaw += yawOffsetDiff - Math.signum(yawOffsetDiff) * maxHeadRotation;
        }

        return bodyYaw;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void lovisual$cacheLastRotations(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        lovisual$lastYaw = self.getYRot();
        lovisual$lastPitch = self.getXRot();
        RotationManager.INSTANCE.runTickLifecycle();
        if (!lovisual$rotationBodyYawInitialized) {
            lovisual$rotationPrevX = self.getX();
            lovisual$rotationPrevZ = self.getZ();
            lovisual$rotationPrevBodyYaw = self.getVisualRotationYInDegrees();
            lovisual$rotationBodyYawInitialized = true;
        }
    }

    @Inject(method = "handlePortalTransitionEffect", at = @At("HEAD"), cancellable = true)
    private void lovisual$disablePortalNausea(boolean inPortal, CallbackInfo ci) {

        LocalPlayer self = (LocalPlayer) (Object) this;
        PortalProcessor portalManager = null;
        if (self instanceof EntityAccessor accessor) {
            portalManager = accessor.lovisual$getPortalManager();
        }
        if (portalManager != null && portalManager.isInsidePortalThisTick()) {
            if (self instanceof LocalPlayerAccessor accessor) {
                accessor.lovisual$setNauseaIntensity(0.0f);
                accessor.lovisual$setLastNauseaIntensity(0.0f);
            }
            ci.cancel();
        }
    }

    @Override
    public float lovisual$getLastYaw() {
        return lovisual$lastYaw;
    }

    @Override
    public float lovisual$getLastPitch() {
        return lovisual$lastPitch;
    }

    @Override
    public void lovisual$setLastYaw(float yaw) {
        this.lovisual$lastYaw = yaw;
    }

    @Override
    public void lovisual$setLastPitch(float pitch) {
        this.lovisual$lastPitch = pitch;
    }

    @ModifyExpressionValue(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;canStartSprinting()Z"
            )
    )
    private boolean lovisual$sprintMovementStart(boolean original) {
        return lovisual$postSprintControl(original, SprintControlEvent.Source.MOVEMENT_TICK);
    }

    @ModifyExpressionValue(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Input;sprint()Z"
            )
    )
    private boolean lovisual$sprintMovementInput(boolean original) {
        return lovisual$postSprintControl(original, SprintControlEvent.Source.MOVEMENT_TICK);
    }

    @ModifyExpressionValue(
            method = "sendIsSprintingIfNeeded",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isSprinting()Z"
            )
    )
    private boolean lovisual$sprintNetwork(boolean original) {
        if (SprintController.INSTANCE.isSprintBlocked()) {
            return false;
        }

        return original;
    }

    @Unique
    private boolean lovisual$postSprintControl(boolean original, SprintControlEvent.Source source) {
        LocalPlayer self = (LocalPlayer) (Object) this;

        if (self.input == null) {
            return original;
        }

        if (!Events.BUS.hasListeners(SprintControlEvent.class)) {
            return original;
        }

        Vec2 movement = self.input.getMoveVector();
        SprintControlEvent event = new SprintControlEvent(
                movement.y,
                movement.x,
                original,
                source
        );
        Events.BUS.post(event);
        return event.shouldSprint();
    }

    @Inject(method = "modifyInput", at = @At("RETURN"), cancellable = true)
    private void boostWhenUsing(Vec2 input, CallbackInfoReturnable<Vec2> cir) {
        // NoStun module removed; no item-use speed boost remains.
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void lovisual$elytraFlyVanilla(CallbackInfo ci) {
        // ElytraFly module removed; no vanilla elytra override remains.
    }

    @Shadow
    protected abstract void sendPosition();

    @Inject(method = "applyInput", at = @At("HEAD"), cancellable = true)
    private void freecam$blockWASD(CallbackInfo ci) {
        Freecam fc = Modules.get(Freecam.class);
        if (fc != null && fc.isEnabled() && fc.isCameraInput()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/ClientInput;tick()V",
                    shift = At.Shift.AFTER
            )
    )
    private void freecam$afterInputTick(CallbackInfo ci) {
        Freecam fc = Modules.get(Freecam.class);
        if (fc == null || !fc.isEnabled() || !fc.isCameraInput()) return;

        ((InputAccessor) this.input).setMovementVector(Vec2.ZERO);

        boolean sneak = false;
        boolean sprint = this.input.keyPresses.sprint();

        this.input.keyPresses = new Input(
                false, false, false, false,
                false,
                sneak,
                sprint
        );
    }

    @Inject(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/ClientInput;tick()V",
                    shift = At.Shift.AFTER
            )
    )
    private void lovisual$afterInputTickSprint(CallbackInfo ci) {
        if (this.input == null) return;

        Input original = this.input.keyPresses;
        if (original == null) return;

        Input input = original;

        if (Events.BUS.hasListeners(MovementInputEvent.class)) {
            MovementInputEvent event = new MovementInputEvent(input);
            Events.BUS.post(event);

            Input eventInput = event.toPlayerInput();
            if (!lovisual$sameInput(input, eventInput)) {
                input = eventInput;
            }
        }

        Input transformedInput = lovisual$transformDirection(input);
        if (!lovisual$sameInput(input, transformedInput)) {
            input = transformedInput;
        }

        if (Events.BUS.hasListeners(SprintControlEvent.class)) {
            float forward = lovisual$impulse(input.forward(), input.backward());
            float sideways = lovisual$impulse(input.left(), input.right());

            SprintControlEvent event = new SprintControlEvent(
                    forward,
                    sideways,
                    input.sprint(),
                    SprintControlEvent.Source.INPUT
            );
            Events.BUS.post(event);

            if (event.shouldSprint() != input.sprint()) {
                input = new Input(
                        input.forward(),
                        input.backward(),
                        input.left(),
                        input.right(),
                        input.jump(),
                        input.shift(),
                        event.shouldSprint()
                );
            }
        }

        if (!lovisual$sameInput(original, input)) {
            lovisual$applyPlayerInput(input);
        }
    }

    @Unique
    private void lovisual$applyPlayerInput(Input input) {
        if (this.input == null || input == null) {
            return;
        }

        InputAccessor accessor = (InputAccessor) this.input;
        accessor.setPlayerInput(input);

        float forward = lovisual$impulse(input.forward(), input.backward());
        float sideways = lovisual$impulse(input.left(), input.right());

        accessor.setMovementVector(new Vec2(sideways, forward).normalized());
    }

    @Unique
    private Input lovisual$transformDirection(Input input) {
        // Free correction now runs at KeyboardInput.tick(), matching the input pipeline.
        // Keep this post-input hook inert to avoid double-transforming movement input.
        return input;
    }

    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Input;shift()Z",
                    ordinal = 0
            )
    )
    private boolean freecam$noFlyDown0(Input in) {
        Freecam fc = Modules.get(Freecam.class);
        if (fc != null && fc.isEnabled() && fc.isCameraInput()) {
            LocalPlayer self = (LocalPlayer) (Object) this;
            if (self.getAbilities().flying) return false;
        }
        return in.shift();
    }

    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Input;shift()Z",
                    ordinal = 1
            ),
            require = 0
    )
    private boolean freecam$noFlyDown1(Input in) {
        Freecam fc = Modules.get(Freecam.class);
        if (fc != null && fc.isEnabled() && fc.isCameraInput()) {
            LocalPlayer self = (LocalPlayer) (Object) this;
            if (self.getAbilities().flying) return false;
        }
        return in.shift();
    }

    @Inject(method = "hurtTo", at = @At("HEAD"))
    private void freecam$onUpdateHealth(float newHealth, CallbackInfo ci) {
        Freecam fc = Modules.get(Freecam.class);

        if (fc == null || !fc.isEnabled() || !fc.disableOnDamage()) {
            freecam$lastHealth = newHealth;
            return;
        }

        if (!Float.isNaN(freecam$lastHealth) && newHealth < freecam$lastHealth) {
            fc.toggle();
        }

        freecam$lastHealth = newHealth;
    }

    @Unique
    private boolean shouldFreezeNow() {
        Freecam freecam = Modules.get(Freecam.class);
        return freecam != null && freecam.freezePlayer() && freecam.isCameraInput();
    }

    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void lovisual$eventSync(CallbackInfo ci) {
        if (!Events.BUS.hasListeners(EventSync.class)) return;
        lovisual$eventPostAction = null;
        LocalPlayer self = (LocalPlayer) (Object) this;
        EventSync event = new EventSync(self.getYRot(), self.getXRot());
        Events.BUS.post(event);
        lovisual$activeSyncEvent = event;
        lovisual$eventPostAction = event.getPostAction();
        if (event.isCancelled()) {
            lovisual$activeSyncEvent = null;
            lovisual$eventPostAction = null;
            ci.cancel();
        }
    }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void lovisual$eventPostSync(CallbackInfo ci) {
        if (Events.BUS.hasListeners(EventPostSync.class)) {
            Events.BUS.post(new EventPostSync());
        }
        if (lovisual$eventPostAction != null) {
            lovisual$eventPostAction.run();
            lovisual$eventPostAction = null;
        }
        lovisual$activeSyncEvent = null;
    }

    @ModifyExpressionValue(
            method = "sendPosition",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"
            )
    )
    private float lovisual$eventSyncYaw(float original) {
        lovisual$updateVisualBodyYaw();

        EventSync event = lovisual$activeSyncEvent;
        if (event != null && event.hasRotationOverride()) return event.getYaw();

        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        return rotation != null ? rotation.yaw() : original;
    }

    @ModifyExpressionValue(
            method = "sendPosition",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"
            )
    )
    private float lovisual$eventSyncPitch(float original) {
        EventSync event = lovisual$activeSyncEvent;
        if (event != null && event.hasRotationOverride()) return Mth.clamp(event.getPitch(), -90.0f, 90.0f);

        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        return rotation != null ? Mth.clamp(rotation.pitch(), -90.0f, 90.0f) : original;
    }

    @ModifyExpressionValue(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"
            )
    )
    private float lovisual$visualTickYaw(float original) {
        lovisual$updateVisualBodyYaw();

        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        return rotation != null ? rotation.yaw() : original;
    }

    @ModifyExpressionValue(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"
            )
    )
    private float lovisual$visualTickPitch(float original) {
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        return rotation != null ? Mth.clamp(rotation.pitch(), -90.0f, 90.0f) : original;
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;sendPosition()V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void lovisual$postPlayerUpdate(CallbackInfo ci) {
        if (!Events.BUS.hasListeners(PostPlayerUpdateEvent.class)) return;
        if (lovisual$postUpdateLock) return;

        PostPlayerUpdateEvent event = new PostPlayerUpdateEvent();
        Events.BUS.post(event);

        if (!event.isCancelled()) return;

        ci.cancel();

        int iterations = event.getIterations();
        if (iterations <= 0) return;

        LocalPlayer self = (LocalPlayer) (Object) this;
        for (int i = 0; i < iterations; i++) {
            lovisual$postUpdateLock = true;
            self.tick();
            lovisual$postUpdateLock = false;
            sendPosition();
        }
    }

    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void cancelPackets(CallbackInfo ci) {
        // Freeze module removed; only Freecam may still freeze movement.
    }

    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void lovisual$freezeTickMovement(CallbackInfo ci) {
        LocalPlayer p = (LocalPlayer) (Object) this;

        if (!shouldFreezeNow()) {
            if (frozen) {
                p.setDeltaMovement(savedVelocity);
                frozen = false;
            }
            return;
        }

        if (!frozen) {
            frozen = true;
            savedX = p.getX();
            savedY = p.getY();
            savedZ = p.getZ();
            savedVelocity = p.getDeltaMovement();

            if (Modules.get(Freecam.class) == null || !Modules.get(Freecam.class).isEnabled()) {
                savedBodyYaw = p.yBodyRot;
                savedHeadYaw = p.yHeadRot;
            }
        }

        p.setDeltaMovement(Vec3.ZERO);
        p.absSnapTo(savedX, savedY, savedZ);

        if (Modules.get(Freecam.class) == null || !Modules.get(Freecam.class).isEnabled()) {
            p.yBodyRot = savedBodyYaw;
            p.yHeadRot = savedHeadYaw;
        }

        ci.cancel();
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void lovisual$noStunTickMovement(CallbackInfo ci) {
        // NoStun module removed; no hurt-time input override remains.
    }

    @Unique
    private void lovisual$updateVisualBodyYaw() {
        LocalPlayer self = (LocalPlayer) (Object) this;
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        if (rotation == null) {
            lovisual$rotationPrevX = self.getX();
            lovisual$rotationPrevZ = self.getZ();
            lovisual$rotationPrevBodyYaw = self.getVisualRotationYInDegrees();
            return;
        }

        float newBodyYaw = lovisual$simulateBodyYaw(
                rotation.yaw(),
                lovisual$rotationPrevBodyYaw,
                lovisual$rotationPrevX,
                lovisual$rotationPrevZ,
                self.getX(),
                self.getZ(),
                self.attackAnim
        );

        self.setYBodyRot(newBodyYaw);
        lovisual$rotationPrevBodyYaw = newBodyYaw;
        lovisual$rotationPrevX = self.getX();
        lovisual$rotationPrevZ = self.getZ();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void detectPearlImpact(CallbackInfo ci) {
        // NoPush module removed; no pearl-phase spoofing remains.
    }

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void sendSpoofAfterPearl(CallbackInfo ci) {
        // NoPush module removed; no pearl-phase spoofing remains.
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onCollideBoost(CallbackInfo ci) {
        // Speed module removed; no entity-collision boost remains.
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void omniLongJump(CallbackInfo ci) {
        // Speed module removed; no jump boost remains.
    }

    @Inject(method = "modifyInput", at = @At("RETURN"), cancellable = true)
    private void applyLegitSpeed(Vec2 input, CallbackInfoReturnable<Vec2> cir) {
        // Speed module removed; no input speed multiplier remains.
    }

    @Inject(method = "closeContainer", at = @At("HEAD"), cancellable = true)
    private void lovisual$inventoryMove$deferCloseHandledScreen(CallbackInfo ci) {
        // InventoryMove module removed; no container close deferral remains.
    }

    @Inject(method = "closeContainer", at = @At("HEAD"), cancellable = true)
    private void lovisual$keepInventoryOpen(CallbackInfo ci) {
        // XCarry module removed; no inventory close suppression remains.
    }

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void lovisual$noPushOutOfBlocks(double x, double z, CallbackInfo ci) {
        if (!Events.BUS.hasListeners(EventPushOutOfBlocks.class)) return;
        EventPushOutOfBlocks event = new EventPushOutOfBlocks(x, z);
        Events.BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
