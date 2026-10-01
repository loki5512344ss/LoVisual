package dev.loki.lovisual.mixinhandlers.player;

import dev.loki.lovisual.events.impl.network.EventSync;
import dev.loki.lovisual.mixininterface.entity.ILocalPlayer;
import dev.loki.lovisual.util.aiming.data.Rotation;
import dev.loki.lovisual.util.aiming.rotation.RotationManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class LocalPlayerRotationHandler {

    private double rotationPrevX;
    private double rotationPrevZ;
    private float rotationPrevBodyYaw;
    private boolean rotationBodyYawInitialized;

    public void onTickCache(LocalPlayer self) {
        if (self instanceof ILocalPlayer access) {
            access.lovisual$setLastYaw(self.getYRot());
            access.lovisual$setLastPitch(self.getXRot());
        }
        RotationManager.INSTANCE.runTickLifecycle();
        if (!rotationBodyYawInitialized) {
            rotationPrevX = self.getX();
            rotationPrevZ = self.getZ();
            rotationPrevBodyYaw = self.getVisualRotationYInDegrees();
            rotationBodyYawInitialized = true;
        }
    }

    public float resolveSendYaw(LocalPlayer self, float original, EventSync active) {
        updateVisualBodyYaw(self);
        if (active != null && active.hasRotationOverride()) return active.getYaw();
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        return rotation != null ? rotation.yaw() : original;
    }

    public float resolveSendPitch(float original, EventSync active) {
        if (active != null && active.hasRotationOverride()) return Mth.clamp(active.getPitch(), -90.0f, 90.0f);
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        return rotation != null ? Mth.clamp(rotation.pitch(), -90.0f, 90.0f) : original;
    }

    public float resolveTickYaw(LocalPlayer self, float original) {
        updateVisualBodyYaw(self);
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        return rotation != null ? rotation.yaw() : original;
    }

    public float resolveTickPitch(float original) {
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        return rotation != null ? Mth.clamp(rotation.pitch(), -90.0f, 90.0f) : original;
    }

    public void updateVisualBodyYaw(LocalPlayer self) {
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        if (rotation == null) {
            rotationPrevX = self.getX();
            rotationPrevZ = self.getZ();
            rotationPrevBodyYaw = self.getVisualRotationYInDegrees();
            return;
        }
        float newBodyYaw = simulateBodyYaw(rotation.yaw(), rotationPrevBodyYaw, rotationPrevX, rotationPrevZ, self.getX(), self.getZ(), self.attackAnim);
        self.setYBodyRot(newBodyYaw);
        rotationPrevBodyYaw = newBodyYaw;
        rotationPrevX = self.getX();
        rotationPrevZ = self.getZ();
    }

    public float simulateBodyYaw(float yaw, float prevBodyYaw, double prevX, double prevZ, double currentX, double currentZ, float handSwing) {
        double motionX = currentX - prevX;
        double motionZ = currentZ - prevZ;
        float motionSquared = (float) (motionX * motionX + motionZ * motionZ);
        float bodyYaw = prevBodyYaw;
        if (motionSquared > 0.0025000002F) {
            float movementYaw = (float) Mth.atan2(motionZ, motionX) * Mth.RAD_TO_DEG - 90.0F;
            float yawDiff = Math.abs(Mth.wrapDegrees(yaw) - movementYaw);
            bodyYaw = (yawDiff > 95.0F && yawDiff < 265.0F) ? movementYaw - 180.0F : movementYaw;
        }
        if (handSwing - 0.2F > 0.0F) bodyYaw = yaw;
        float deltaYaw = Mth.wrapDegrees(bodyYaw - prevBodyYaw);
        bodyYaw = prevBodyYaw + deltaYaw * 0.3F;
        float yawOffsetDiff = Mth.wrapDegrees(yaw - bodyYaw);
        if (Math.abs(yawOffsetDiff) > 52.0F) bodyYaw += yawOffsetDiff - Math.signum(yawOffsetDiff) * 52.0F;
        return bodyYaw;
    }
}
