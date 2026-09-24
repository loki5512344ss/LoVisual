package dev.loki.lovisual.util.player.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.util.player.effect.tracker.StatusEffectAccess;
import dev.loki.lovisual.util.player.simulation.remote.RemotePlayerObservation;

public abstract class PlayerMovementSimulation {
    static final double STEP_HEIGHT = 0.5;

    final Player player;
    final SimulatedInput input;
    final float yaw;
    final float pitch;
    final boolean sprinting;
    Vec3 pos;
    Vec3 velocity;
    AABB boundingBox;
    boolean onGround;
    boolean jumping;
    boolean gliding;
    double fallDistance;
    boolean horizontalCollision;
    boolean clipLedged;
    boolean touchingWater;
    boolean submergedInWater;
    boolean swimming;
    double waterHeight;
    double lavaHeight;
    int jumpingCooldown;

    protected PlayerMovementSimulation(Player player, SimulatedInput input) {
        this.player = player;
        this.pos = player.position();
        this.velocity = this.pos.subtract(player.xo, player.yo, player.zo);
        this.boundingBox = player.getBoundingBox();
        this.yaw = player.getYRot();
        this.pitch = player.getXRot();
        this.sprinting = player.isSprinting();
        this.onGround = player.onGround();
        this.fallDistance = player.fallDistance;
        this.jumping = !player.onGround();
        this.gliding = player.isFallFlying();
        this.input = input;
    }

    void tick() {
        clipLedged = false;
        input.update();

        FluidStateHelper.updateWaterState(this);
        FluidStateHelper.updateSubmergedInWaterState(this);
        FluidStateHelper.updateSwimming(this);
        SneakAndClimbHelper.applyBubbleColumnEffects(this);

        if (jumpingCooldown > 0) {
            jumpingCooldown--;
        }

        jumping = input.jump;

        double vx = Math.abs(velocity.x) < 0.003 ? 0.0 : velocity.x;
        double vy = Math.abs(velocity.y) < 0.003 ? 0.0 : velocity.y;
        double vz = Math.abs(velocity.z) < 0.003 ? 0.0 : velocity.z;
        velocity = new Vec3(vx, vy, vz);

        if (jumping && FluidPhysicsHelper.shouldSwimInFluids(this)) {
            double fluidHeight = FluidPhysicsHelper.isInLava(this) ? lavaHeight : waterHeight;
            boolean inWater = touchingWater && fluidHeight > 0.0;
            double swimHeight = FluidPhysicsHelper.getSwimHeight(this);

            if (!inWater || onGround && !(fluidHeight > swimHeight)) {
                if (!FluidPhysicsHelper.isInLava(this) || onGround && !(fluidHeight > swimHeight)) {
                    if ((onGround || inWater && fluidHeight <= swimHeight) && jumpingCooldown == 0) {
                        FluidPhysicsHelper.jump(this);
                        jumpingCooldown = 10;
                    }
                } else {
                    FluidPhysicsHelper.swimUpward(this);
                }
            } else {
                FluidPhysicsHelper.swimUpward(this);
            }
        } else {
            jumpingCooldown = 0;
        }

        if (StatusEffectAccess.has(player, MobEffects.SLOW_FALLING) || StatusEffectAccess.has(player, MobEffects.LEVITATION)) {
            onGround = false;
        }

        TravelHelper.travel(this, new Vec3(input.movementSideways * 0.98f, 0.0, input.movementForward * 0.98f));
    }

    protected boolean shouldUseRemoteFlightPhysics() {
        return false;
    }

    protected boolean shouldHoldVerticalMotion() {
        return false;
    }

    protected RemotePlayerObservation.MotionKind motionKind() {
        return RemotePlayerObservation.MotionKind.NORMAL;
    }

    BlockPos getVelocityAffectingPos() {
        return BlockPos.containing(pos.x, boundingBox.minY - 0.5000001, pos.z);
    }

    protected void setSimulatedVelocity(Vec3 velocity) {
        this.velocity = velocity;
    }

    protected void setSimulatedFallDistance(double fallDistance) {
        this.fallDistance = fallDistance;
    }

    protected void setSimulatedOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    protected void setSimulatedGliding(boolean gliding) {
        this.gliding = gliding;
    }

    Vec3 position() {
        return pos;
    }

    double simulatedFallDistance() {
        return fallDistance;
    }

    Vec3 simulatedVelocity() {
        return velocity;
    }

    boolean simulatedOnGround() {
        return onGround;
    }

    boolean simulatedClipLedged() {
        return clipLedged;
    }

}
