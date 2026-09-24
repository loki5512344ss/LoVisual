package dev.loki.lovisual.util.aiming.data;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.util.aiming.rotation.RotationManager;
import dev.loki.lovisual.util.aiming.rotation.RotationUtil;

public record Rotation(float yaw, float pitch, boolean isNormalized) {

    public static final Rotation ZERO = new Rotation(0.0f, 0.0f, false);

    public Rotation(float yaw, float pitch) {
        this(yaw, pitch, false);
    }

    public static Rotation lookingAt(Vec3 point, Vec3 from) {
        return fromRotationVec(point.subtract(from));
    }

    public static Rotation fromRotationVec(Vec3 lookVec) {
        return fromRotationVec(lookVec.x, lookVec.y, lookVec.z);
    }

    public static Rotation fromRotationVec(double diffX, double diffY, double diffZ) {
        float yaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f);
        float pitch = Mth.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, Math.hypot(diffX, diffZ))));
        return new Rotation(yaw, pitch, false);
    }

    private static float clamp(float v, float low, float high) {
        if (v < low) return low;
        if (v > high) return high;
        return v;
    }

    private static float quantizeDelta(float rawDelta, float gcd) {
        if (gcd <= 0.0f) {
            return rawDelta;
        }

        return Math.round(rawDelta / gcd) * gcd;
    }

    public Vec3 directionVector() {
        return Vec3.directionFromRotation(pitch, yaw);
    }

    public float xRot() {
        return pitch;
    }

    public float yRot() {
        return yaw;
    }

    /**
     * Fixes GCD and modulo 360 at yaw.
     */
    public Rotation normalize() {
        if (isNormalized) return this;

        double gcd = RotationUtil.gcd();
        if (gcd <= 0.0) {
            float ny = yaw;
            float np = pitch;
            return new Rotation(ny, Mth.clamp(np, -90.0f, 90.0f), true);
        }

        Rotation base = RotationManager.INSTANCE.getServerRotation();
        if (base == null || (!base.isNormalized() && base == Rotation.ZERO)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) {
                base = new Rotation(mc.player.getYRot(), mc.player.getXRot(), true);
            } else {
                base = Rotation.ZERO;
            }
        }

        RotationDelta diff = base.rotationDeltaTo(this);
        float g1 = quantizeDelta(diff.deltaYaw(), (float) gcd);
        float g2 = quantizeDelta(diff.deltaPitch(), (float) gcd);

        float ny = base.yaw + g1;
        float np = base.pitch + g2;
        return new Rotation(ny, Mth.clamp(np, -90.0f, 90.0f), true);
    }

    public float angleTo(Rotation other) {
        return Math.min(rotationDeltaTo(other).length(), 180.0f);
    }

    public RotationDelta rotationDeltaTo(Rotation other) {
        return new RotationDelta(
                RotationUtil.angleDifference(other.yaw, this.yaw),
                RotationUtil.angleDifference(other.pitch, this.pitch)
        );
    }

    public Rotation towardsLinear(Rotation other, float horizontalFactor, float verticalFactor) {
        RotationDelta diff = rotationDeltaTo(other);
        float rotationDifference = diff.length();
        if (rotationDifference <= 0.0f) {
            return this;
        }

        float straightLineYaw = Math.abs(diff.deltaYaw() / rotationDifference) * horizontalFactor;
        float straightLinePitch = Math.abs(diff.deltaPitch() / rotationDifference) * verticalFactor;

        return new Rotation(
                this.yaw + clamp(diff.deltaYaw(), -straightLineYaw, straightLineYaw),
                this.pitch + clamp(diff.deltaPitch(), -straightLinePitch, straightLinePitch)
        );
    }

    public boolean approximatelyEquals(Rotation other) {
        return approximatelyEquals(other, 2.0f);
    }

    public boolean approximatelyEquals(Rotation other, float tolerance) {
        if (other == null) return false;
        return angleTo(other) <= tolerance;
    }
}
