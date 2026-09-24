package dev.loki.lovisual.util.aiming.data;

public record RotationDelta(float deltaYaw, float deltaPitch) {

    public float length() {
        return (float) Math.hypot(deltaYaw, deltaPitch);
    }

    public net.minecraft.world.phys.Vec2 toVec2f() {
        return new net.minecraft.world.phys.Vec2(deltaYaw, deltaPitch);
    }
}
