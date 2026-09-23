/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.player.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

final class FluidPhysicsHelper {

    private FluidPhysicsHelper() {
    }

    static void jump(PlayerMovementSimulation sim) {
        double jumpVelocity = sim.player.getAttributeValue(Attributes.JUMP_STRENGTH) * getJumpVelocityMultiplier(sim)
                + sim.player.getJumpBoostPower();
        if (jumpVelocity <= 1.0E-5) {
            return;
        }

        sim.velocity = new Vec3(sim.velocity.x, Math.max(jumpVelocity, sim.velocity.y), sim.velocity.z);

        if (sim.sprinting) {
            float yawRadians = sim.yaw * Mth.DEG_TO_RAD;
            sim.velocity = sim.velocity.add(-Mth.sin(yawRadians) * 0.2f, 0.0, Mth.cos(yawRadians) * 0.2f);
        }
    }

    static double getJumpVelocityMultiplier(PlayerMovementSimulation sim) {
        BlockPos feetPos = BlockPos.containing(sim.pos);
        float feet = sim.player.level().getBlockState(feetPos).getBlock().getJumpFactor();
        float floor = sim.player.level().getBlockState(sim.getVelocityAffectingPos()).getBlock().getJumpFactor();
        return feet == 1.0f ? floor : feet;
    }

    static void travelInFluid(PlayerMovementSimulation sim, Vec3 movementInput) {
        boolean falling = sim.velocity.y <= 0.0;
        double y = sim.pos.y;
        double gravity = TravelHelper.getEffectiveGravity(sim);

        if (sim.touchingWater) {
            travelInWater(sim, movementInput, gravity, falling, y);
        } else {
            travelInLava(sim, movementInput, gravity, falling, y);
        }
    }

    static void travelInWater(PlayerMovementSimulation sim, Vec3 movementInput, double gravity, boolean falling, double y) {
        float movementMultiplier = sim.sprinting ? 0.9f : 0.8f;
        float acceleration = 0.02f;
        float waterEfficiency = (float) sim.player.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
        if (!sim.onGround) {
            waterEfficiency *= 0.5f;
        }

        if (waterEfficiency > 0.0f) {
            movementMultiplier += (0.54600006f - movementMultiplier) * waterEfficiency;
            acceleration += (sim.player.getSpeed() - acceleration) * waterEfficiency;
        }

        if (dev.loki.lovisual.util.player.effect.tracker.StatusEffectAccess.has(sim.player, net.minecraft.world.effect.MobEffects.DOLPHINS_GRACE)) {
            movementMultiplier = 0.96f;
        }

        TravelHelper.updateVelocity(sim, acceleration, movementInput);
        SneakAndClimbHelper.move(sim, sim.velocity);

        Vec3 movedVelocity = sim.velocity;
        if (sim.horizontalCollision && SneakAndClimbHelper.isClimbing(sim)) {
            movedVelocity = new Vec3(movedVelocity.x, 0.2, movedVelocity.z);
        }

        movedVelocity = movedVelocity.multiply(movementMultiplier, 0.8f, movementMultiplier);
        sim.velocity = applyFluidMovingSpeed(sim, gravity, falling, movedVelocity);
        resetVerticalVelocityInFluid(sim, y);
    }

    static void travelInLava(PlayerMovementSimulation sim, Vec3 movementInput, double gravity, boolean falling, double y) {
        TravelHelper.updateVelocity(sim, 0.02f, movementInput);
        SneakAndClimbHelper.move(sim, sim.velocity);
        if (sim.lavaHeight <= getSwimHeight(sim)) {
            sim.velocity = sim.velocity.multiply(0.5, 0.8f, 0.5);
            sim.velocity = applyFluidMovingSpeed(sim, gravity, falling, sim.velocity);
        } else {
            sim.velocity = sim.velocity.scale(0.5);
        }

        if (gravity != 0.0) {
            sim.velocity = sim.velocity.add(0.0, -gravity / 4.0, 0.0);
        }

        resetVerticalVelocityInFluid(sim, y);
    }

    static Vec3 applyFluidMovingSpeed(PlayerMovementSimulation sim, double gravity, boolean falling, Vec3 motion) {
        if (gravity != 0.0 && !sim.sprinting) {
            double y = falling && Math.abs(motion.y - 0.005) >= 0.003 && Math.abs(motion.y - gravity / 16.0) < 0.003
                    ? -0.003
                    : motion.y - gravity / 16.0;
            return new Vec3(motion.x, y, motion.z);
        }

        return motion;
    }

    static void resetVerticalVelocityInFluid(PlayerMovementSimulation sim, double y) {
        if (sim.horizontalCollision && doesNotCollide(sim, sim.velocity.x, sim.velocity.y + 0.6f - sim.pos.y + y, sim.velocity.z)) {
            sim.velocity = new Vec3(sim.velocity.x, 0.3f, sim.velocity.z);
        }
    }

    static boolean doesNotCollide(PlayerMovementSimulation sim, double offsetX, double offsetY, double offsetZ) {
        net.minecraft.world.phys.AABB moved = sim.boundingBox.move(offsetX, offsetY, offsetZ);
        return sim.player.level().noCollision(sim.player, moved) && !sim.player.level().containsAnyLiquid(moved);
    }

    static boolean shouldSwimInFluids(PlayerMovementSimulation sim) {
        return !sim.player.getAbilities().flying;
    }

    static boolean isInLava(PlayerMovementSimulation sim) {
        return sim.lavaHeight > 0.0;
    }

    static double getSwimHeight(PlayerMovementSimulation sim) {
        return sim.player.getEyeHeight(sim.player.getPose()) < 0.4f ? 0.0 : 0.4;
    }

    static void swimUpward(PlayerMovementSimulation sim) {
        sim.velocity = sim.velocity.add(0.0, 0.04f, 0.0);
    }

    static Vec3 applyWebSpeed(PlayerMovementSimulation sim, Vec3 motion) {
        BlockState state = sim.player.level().getBlockState(BlockPos.containing(sim.pos));
        if (!state.is(Blocks.COBWEB)) {
            return motion;
        }
        if (dev.loki.lovisual.util.player.effect.tracker.StatusEffectAccess.has(sim.player, net.minecraft.world.effect.MobEffects.WEAVING)) {
            return motion.multiply(0.5, 0.25, 0.5);
        }
        return motion.multiply(0.25, 0.05, 0.25);
    }
}
