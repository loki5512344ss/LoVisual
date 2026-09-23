/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.player.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class FluidStateHelper {

    private FluidStateHelper() {
    }

    static void updateWaterState(PlayerMovementSimulation sim) {
        sim.waterHeight = 0.0;
        sim.lavaHeight = 0.0;
        sim.touchingWater = updateMovementInFluid(sim, true, 0.014);
        updateMovementInFluid(sim, false, 0.0023333333333333335);
    }

    static void updateSubmergedInWaterState(PlayerMovementSimulation sim) {
        double eyeY = sim.pos.y + sim.player.getEyeHeight(sim.player.getPose());
        BlockPos blockPos = BlockPos.containing(sim.pos.x, eyeY, sim.pos.z);
        FluidState fluidState = sim.player.level().getFluidState(blockPos);
        double fluidY = blockPos.getY() + fluidState.getHeight(sim.player.level(), blockPos);
        sim.submergedInWater = fluidState.is(FluidTags.WATER) && fluidY > eyeY;
    }

    static void updateSwimming(PlayerMovementSimulation sim) {
        if (sim.player.getAbilities().flying) {
            sim.swimming = false;
            return;
        }

        if (sim.swimming) {
            sim.swimming = sim.sprinting && sim.touchingWater && !sim.player.isPassenger();
        } else {
            FluidState state = sim.player.level().getFluidState(BlockPos.containing(sim.pos));
            sim.swimming = sim.sprinting && sim.submergedInWater && !sim.player.isPassenger() && state.is(FluidTags.WATER);
        }
    }

    static boolean updateMovementInFluid(PlayerMovementSimulation sim, boolean water, double speed) {
        Level world = sim.player.level();
        if (isRegionUnloaded(sim, world)) {
            return false;
        }

        AABB box = sim.boundingBox.deflate(0.001);
        int minX = Mth.floor(box.minX);
        int maxX = Mth.ceil(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.ceil(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.ceil(box.maxZ);

        double maxHeight = 0.0;
        boolean found = false;
        Vec3 addedVelocity = Vec3.ZERO;
        int samples = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    mutable.set(x, y, z);
                    FluidState fluidState = world.getFluidState(mutable);
                    if (!(water ? fluidState.is(FluidTags.WATER) : fluidState.is(FluidTags.LAVA))) {
                        continue;
                    }

                    double height = y + fluidState.getHeight(world, mutable);
                    if (height < box.minY) {
                        continue;
                    }

                    found = true;
                    maxHeight = Math.max(height - box.minY, maxHeight);
                    Vec3 fluidVelocity = fluidState.getFlow(world, mutable);
                    if (maxHeight < 0.4) {
                        fluidVelocity = fluidVelocity.scale(maxHeight);
                    }
                    addedVelocity = addedVelocity.add(fluidVelocity);
                    samples++;
                }
            }
        }

        if (samples > 0) {
            addedVelocity = addedVelocity.scale(1.0 / samples).scale(speed);
            if (Math.abs(sim.velocity.x) < 0.003 && Math.abs(sim.velocity.z) < 0.003 && addedVelocity.length() < 0.0045) {
                addedVelocity = addedVelocity.normalize().scale(0.0045);
            }
            if (addedVelocity.lengthSqr() > 0.0) {
                sim.velocity = sim.velocity.add(addedVelocity);
            }
        }

        if (water) {
            sim.waterHeight = maxHeight;
        } else {
            sim.lavaHeight = maxHeight;
        }
        return found;
    }

    private static boolean isRegionUnloaded(PlayerMovementSimulation sim, Level world) {
        AABB box = sim.boundingBox.inflate(1.0);
        int minX = Mth.floor(box.minX);
        int maxX = Mth.ceil(box.maxX);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.ceil(box.maxZ);

        for (int x = minX >> 4; x <= (maxX - 1) >> 4; x++) {
            for (int z = minZ >> 4; z <= (maxZ - 1) >> 4; z++) {
                LevelChunk chunk = world.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    return true;
                }
            }
        }
        return false;
    }
}
