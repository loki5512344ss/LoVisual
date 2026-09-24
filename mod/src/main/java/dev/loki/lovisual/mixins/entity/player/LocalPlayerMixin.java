package dev.loki.lovisual.mixins.entity.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.loki.lovisual.events.impl.input.SprintControlEvent;
import dev.loki.lovisual.mixininterface.entity.ILocalPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin implements ILocalPlayer {

    @Shadow public ClientInput input;
    @Shadow protected abstract void sendPosition();

    @Unique private float lovisual$lastYaw;
    @Unique private float lovisual$lastPitch;
    @Unique private final LocalPlayerInputHandler lovisual$inputHandler = new LocalPlayerInputHandler();
    @Unique private final LocalPlayerRotationHandler lovisual$rotationHandler = new LocalPlayerRotationHandler();
    @Unique private final LocalPlayerNetworkHandler lovisual$networkHandler = new LocalPlayerNetworkHandler();
    @Unique private final LocalPlayerMovementHandler lovisual$movementHandler = new LocalPlayerMovementHandler();

    @Inject(method = "tick", at = @At("HEAD"))
    private void lovisual$cacheLastRotations(CallbackInfo ci) {
        lovisual$rotationHandler.onTickCache((LocalPlayer) (Object) this);
    }

    @Inject(method = "handlePortalTransitionEffect", at = @At("HEAD"), cancellable = true)
    private void lovisual$disablePortalNausea(boolean inPortal, CallbackInfo ci) {
        lovisual$movementHandler.onPortalNausea((LocalPlayer) (Object) this, ci);
    }

    @Override public float lovisual$getLastYaw() { return lovisual$lastYaw; }
    @Override public float lovisual$getLastPitch() { return lovisual$lastPitch; }
    @Override public void lovisual$setLastYaw(float yaw) { lovisual$lastYaw = yaw; }
    @Override public void lovisual$setLastPitch(float pitch) { lovisual$lastPitch = pitch; }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;canStartSprinting()Z"))
    private boolean lovisual$sprintMovementStart(boolean original) {
        return lovisual$inputHandler.postSprintControl((LocalPlayer) (Object) this, original, SprintControlEvent.Source.MOVEMENT_TICK);
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Input;sprint()Z"))
    private boolean lovisual$sprintMovementInput(boolean original) {
        return lovisual$inputHandler.postSprintControl((LocalPlayer) (Object) this, original, SprintControlEvent.Source.MOVEMENT_TICK);
    }

    @ModifyExpressionValue(method = "sendIsSprintingIfNeeded", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSprinting()Z"))
    private boolean lovisual$sprintNetwork(boolean original) {
        return lovisual$networkHandler.shouldBlockSprintNetwork(original);
    }

    @Inject(method = "applyInput", at = @At("HEAD"), cancellable = true)
    private void freecam$blockWASD(CallbackInfo ci) { if (lovisual$inputHandler.shouldBlockWASD()) ci.cancel(); }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;tick()V", shift = At.Shift.AFTER))
    private void freecam$afterInputTick(CallbackInfo ci) { lovisual$inputHandler.handleFreecamAfterInputTick((LocalPlayer) (Object) this); }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;tick()V", shift = At.Shift.AFTER))
    private void lovisual$afterInputTickSprint(CallbackInfo ci) { lovisual$inputHandler.handleAfterInputTickSprint((LocalPlayer) (Object) this); }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Input;shift()Z", ordinal = 0))
    private boolean freecam$noFlyDown0(Input in) { return lovisual$inputHandler.redirectShift(in, (LocalPlayer) (Object) this); }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Input;shift()Z", ordinal = 1), require = 0)
    private boolean freecam$noFlyDown1(Input in) { return lovisual$inputHandler.redirectShift(in, (LocalPlayer) (Object) this); }

    @Inject(method = "hurtTo", at = @At("HEAD"))
    private void freecam$onUpdateHealth(float newHealth, CallbackInfo ci) { lovisual$movementHandler.onHurt(newHealth); }

    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void lovisual$eventSync(CallbackInfo ci) { lovisual$networkHandler.onSendPositionHead((LocalPlayer) (Object) this, ci); }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void lovisual$eventPostSync(CallbackInfo ci) { lovisual$networkHandler.onSendPositionReturn(); }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float lovisual$eventSyncYaw(float original) { return lovisual$rotationHandler.resolveSendYaw((LocalPlayer) (Object) this, original, lovisual$networkHandler.getActiveSyncEvent()); }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float lovisual$eventSyncPitch(float original) { return lovisual$rotationHandler.resolveSendPitch(original, lovisual$networkHandler.getActiveSyncEvent()); }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float lovisual$visualTickYaw(float original) { return lovisual$rotationHandler.resolveTickYaw((LocalPlayer) (Object) this, original); }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float lovisual$visualTickPitch(float original) { return lovisual$rotationHandler.resolveTickPitch(original); }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;sendPosition()V", ordinal = 0, shift = At.Shift.AFTER), cancellable = true)
    private void lovisual$postPlayerUpdate(CallbackInfo ci) { lovisual$networkHandler.onPostPlayerUpdate((LocalPlayer) (Object) this, ci, this::sendPosition); }

    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void lovisual$freezeTickMovement(CallbackInfo ci) { lovisual$movementHandler.onFreezeTick((LocalPlayer) (Object) this, ci); }

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void lovisual$noPushOutOfBlocks(double x, double z, CallbackInfo ci) { lovisual$movementHandler.onPushOutOfBlocks(x, z, ci); }
}
