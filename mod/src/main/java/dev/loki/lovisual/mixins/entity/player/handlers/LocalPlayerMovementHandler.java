package dev.loki.lovisual.mixins.entity.player;

import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.events.impl.world.EventPushOutOfBlocks;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.camera.Freecam;
import dev.loki.lovisual.mixins.accessors.entity.EntityAccessor;
import dev.loki.lovisual.mixins.accessors.entity.LocalPlayerAccessor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

final class LocalPlayerMovementHandler {

    private float lastHealth = Float.NaN;
    private boolean frozen;
    private Vec3 savedVelocity = Vec3.ZERO;
    private double savedX, savedY, savedZ;
    private float savedBodyYaw, savedHeadYaw;

    void onPortalNausea(LocalPlayer self, CallbackInfo ci) {
        PortalProcessor portal = null;
        if (self instanceof EntityAccessor accessor) portal = accessor.lovisual$getPortalManager();
        if (portal != null && portal.isInsidePortalThisTick()) {
            if (self instanceof LocalPlayerAccessor acc) {
                acc.lovisual$setNauseaIntensity(0.0f);
                acc.lovisual$setLastNauseaIntensity(0.0f);
            }
            ci.cancel();
        }
    }

    void onHurt(float newHealth) {
        Freecam fc = Modules.get(Freecam.class);
        if (fc == null || !fc.isEnabled() || !fc.disableOnDamage()) { lastHealth = newHealth; return; }
        if (!Float.isNaN(lastHealth) && newHealth < lastHealth) fc.toggle();
        lastHealth = newHealth;
    }

    void onPushOutOfBlocks(double x, double z, CallbackInfo ci) {
        if (!Events.BUS.hasListeners(EventPushOutOfBlocks.class)) return;
        EventPushOutOfBlocks event = new EventPushOutOfBlocks(x, z);
        Events.BUS.post(event);
        if (event.isCancelled()) ci.cancel();
    }

    boolean shouldFreezeNow() {
        Freecam freecam = Modules.get(Freecam.class);
        return freecam != null && freecam.freezePlayer() && freecam.isCameraInput();
    }

    void onFreezeTick(LocalPlayer self, CallbackInfo ci) {
        if (!shouldFreezeNow()) {
            if (frozen) { self.setDeltaMovement(savedVelocity); frozen = false; }
            return;
        }
        if (!frozen) {
            frozen = true;
            savedX = self.getX(); savedY = self.getY(); savedZ = self.getZ();
            savedVelocity = self.getDeltaMovement();
            if (Modules.get(Freecam.class) == null || !Modules.get(Freecam.class).isEnabled()) {
                savedBodyYaw = self.yBodyRot; savedHeadYaw = self.yHeadRot;
            }
        }
        self.setDeltaMovement(Vec3.ZERO);
        self.absSnapTo(savedX, savedY, savedZ);
        if (Modules.get(Freecam.class) == null || !Modules.get(Freecam.class).isEnabled()) {
            self.yBodyRot = savedBodyYaw; self.yHeadRot = savedHeadYaw;
        }
        ci.cancel();
    }
}
