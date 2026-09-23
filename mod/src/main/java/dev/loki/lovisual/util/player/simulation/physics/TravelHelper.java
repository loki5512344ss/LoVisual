/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.player.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.mixins.accessors.entity.EntityInvoker;
import dev.loki.lovisual.util.player.effect.tracker.StatusEffectAccess;

final class TravelHelper {

    private TravelHelper() {
    }

    static void travel(PlayerMovementSimulation sim, Vec3 movementInput) {
        if (sim.swimming && !sim.player.isPassenger()) {
            double lookY = getRotationVector(sim).y;
            double climb = lookY < -0.2 ? 0.085 : 0.06;
            BlockPos fluidPos = BlockPos.containing(sim.pos.x, sim.pos.y + 0.9, sim.pos.z);
            if (lookY <= 0.0 || sim.jumping || !sim.player.level().getFluidState(fluidPos).isEmpty()) {
                sim.velocity = sim.velocity.add(0.0, (lookY - sim.velocity.y) * climb, 0.0);
            }
        }

        if ((sim.touchingWater || FluidPhysicsHelper.isInLava(sim)) && FluidPhysicsHelper.shouldSwimInFluids(sim)) {
            FluidPhysicsHelper.travelInFluid(sim, movementInput);
            return;
        }

        if (sim.shouldUseRemoteFlightPhysics()) {
            travelRemoteFlight(sim, movementInput);
            return;
        }

        if (sim.gliding) {
            travelGliding(sim, movementInput);
            return;
        }

        travelMidAir(sim, movementInput);
    }

    static void travelRemoteFlight(PlayerMovementSimulation sim, Vec3 movementInput) {
        updateVelocity(sim, 0.045f, movementInput);

        if (sim.shouldHoldVerticalMotion() && Math.abs(sim.velocity.y) < 0.12) {
            sim.velocity = new Vec3(sim.velocity.x, 0.0, sim.velocity.z);
        }

        SneakAndClimbHelper.move(sim, sim.velocity);

        double yDrag = sim.shouldHoldVerticalMotion() ? 0.35 : 0.91;
        sim.velocity = sim.velocity.multiply(0.91, yDrag, 0.91);

        if (sim.shouldHoldVerticalMotion() && Math.abs(sim.velocity.y) < 0.015) {
            sim.velocity = new Vec3(sim.velocity.x, 0.0, sim.velocity.z);
        }

        sim.fallDistance = 0.0;
        sim.onGround = false;
    }

    static void travelMidAir(PlayerMovementSimulation sim, Vec3 movementInput) {
        BlockPos blockPos = sim.getVelocityAffectingPos();
        float slipperiness = sim.player.level().getBlockState(blockPos).getBlock().getFriction();
        float friction = sim.onGround ? slipperiness * 0.91f : 0.91f;

        Vec3 movedVelocity = applyMovementInput(sim, movementInput, slipperiness);
        double y = movedVelocity.y;
        MobEffectInstance levitation = StatusEffectAccess.get(sim.player, MobEffects.LEVITATION);
        if (levitation != null) {
            y += (0.05 * (levitation.getAmplifier() + 1) - movedVelocity.y) * 0.2;
        } else if (!sim.player.level().isClientSide() || sim.player.level().hasChunkAt(blockPos)) {
            y -= getEffectiveGravity(sim);
        } else if (sim.pos.y > sim.player.level().getMinY()) {
            y = -0.1;
        } else {
            y = 0.0;
        }

        if (sim.player.shouldDiscardFriction()) {
            sim.velocity = new Vec3(movedVelocity.x, y, movedVelocity.z);
        } else {
            sim.velocity = new Vec3(movedVelocity.x * friction, y * 0.9800000190734863, movedVelocity.z * friction);
        }
    }

    static void travelGliding(PlayerMovementSimulation sim, Vec3 movementInput) {
        if (SneakAndClimbHelper.isClimbing(sim)) {
            travelMidAir(sim, movementInput);
            sim.gliding = false;
            return;
        }

        Vec3 oldVelocity = sim.velocity;
        double horizontalSpeed = oldVelocity.horizontalDistance();
        sim.velocity = calcGlidingVelocity(sim, oldVelocity);
        SneakAndClimbHelper.move(sim, sim.velocity);

        if (!sim.player.level().isClientSide()) {
            double newHorizontalSpeed = sim.velocity.horizontalDistance();
            if (sim.horizontalCollision && oldVelocity.horizontalDistance() - newHorizontalSpeed > 0.0) {
                sim.gliding = false;
            }
        }
    }

    static Vec3 calcGlidingVelocity(PlayerMovementSimulation sim, Vec3 oldVelocity) {
        Vec3 rotation = getRotationVector(sim);
        float pitchRadians = sim.pitch * Mth.DEG_TO_RAD;
        double horizontalLook = Math.sqrt(rotation.x * rotation.x + rotation.z * rotation.z);
        double horizontalVelocity = oldVelocity.horizontalDistance();
        double gravity = getEffectiveGravity(sim);
        double lift = Mth.square(Math.cos(pitchRadians));

        oldVelocity = oldVelocity.add(0.0, gravity * (-1.0 + lift * 0.75), 0.0);
        if (oldVelocity.y < 0.0 && horizontalLook > 0.0) {
            double dive = oldVelocity.y * -0.1 * lift;
            oldVelocity = oldVelocity.add(rotation.x * dive / horizontalLook, dive, rotation.z * dive / horizontalLook);
        }

        if (pitchRadians < 0.0f && horizontalLook > 0.0) {
            double climb = horizontalVelocity * -Mth.sin(pitchRadians) * 0.04;
            oldVelocity = oldVelocity.add(-rotation.x * climb / horizontalLook, climb * 3.2, -rotation.z * climb / horizontalLook);
        }

        if (horizontalLook > 0.0) {
            oldVelocity = oldVelocity.add(
                    (rotation.x / horizontalLook * horizontalVelocity - oldVelocity.x) * 0.1,
                    0.0,
                    (rotation.z / horizontalLook * horizontalVelocity - oldVelocity.z) * 0.1
            );
        }

        return oldVelocity.multiply(0.99f, 0.98f, 0.99f);
    }

    static float getMovementSpeed(PlayerMovementSimulation sim, float slipperiness) {
        if (sim.onGround) {
            return sim.player.getSpeed() * (0.21600002f / (slipperiness * slipperiness * slipperiness));
        }

        return sim.sprinting ? 0.025999999f : 0.02f;
    }

    static void updateVelocity(PlayerMovementSimulation sim, float speed, Vec3 movementInput) {
        Vec3 added = EntityInvoker.lovisual$movementInputToVelocity(movementInput, speed, sim.yaw);
        sim.velocity = sim.velocity.add(added);
    }

    static Vec3 applyMovementInput(PlayerMovementSimulation sim, Vec3 movementInput, float slipperiness) {
        updateVelocity(sim, getMovementSpeed(sim, slipperiness), movementInput);
        sim.velocity = SneakAndClimbHelper.applyClimbingSpeed(sim, sim.velocity);
        sim.velocity = FluidPhysicsHelper.applyWebSpeed(sim, sim.velocity);
        SneakAndClimbHelper.move(sim, SneakAndClimbHelper.adjustMovementForSneaking(sim, sim.velocity));
        Vec3 movedVelocity = sim.velocity;
        if ((sim.horizontalCollision || sim.jumping) && SneakAndClimbHelper.isClimbing(sim)) {
            movedVelocity = new Vec3(movedVelocity.x, 0.2, movedVelocity.z);
        }
        return movedVelocity;
    }

    static double getEffectiveGravity(PlayerMovementSimulation sim) {
        double gravity = sim.player.getGravity();
        if (sim.velocity.y <= 0.0 && StatusEffectAccess.has(sim.player, MobEffects.SLOW_FALLING)) {
            return Math.min(gravity, 0.01);
        }
        return gravity;
    }

    static Vec3 getRotationVector(PlayerMovementSimulation sim) {
        return Vec3.directionFromRotation(sim.pitch, sim.yaw);
    }
}
