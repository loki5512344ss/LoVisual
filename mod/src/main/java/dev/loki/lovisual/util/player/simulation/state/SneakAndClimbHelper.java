package dev.loki.lovisual.util.player.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

final class SneakAndClimbHelper {

    private SneakAndClimbHelper() {
    }

    static Vec3 adjustMovementForSneaking(PlayerMovementSimulation sim, Vec3 movement) {
        if (movement.y > 0.0 || !shouldConsiderLedgeClipping(sim)) {
            return movement;
        }

        double x = movement.x;
        double z = movement.z;
        while (x != 0.0 && sim.player.level().noCollision(sim.player, sim.boundingBox.move(x, -PlayerMovementSimulation.STEP_HEIGHT, 0.0))) {
            if (x < 0.05 && x >= -0.05) {
                x = 0.0;
            } else if (x > 0.0) {
                x -= 0.05;
            } else {
                x += 0.05;
            }
        }

        while (z != 0.0 && sim.player.level().noCollision(sim.player, sim.boundingBox.move(0.0, -PlayerMovementSimulation.STEP_HEIGHT, z))) {
            if (z < 0.05 && z >= -0.05) {
                z = 0.0;
            } else if (z > 0.0) {
                z -= 0.05;
            } else {
                z += 0.05;
            }
        }

        while (x != 0.0 && z != 0.0 && sim.player.level().noCollision(sim.player, sim.boundingBox.move(x, -PlayerMovementSimulation.STEP_HEIGHT, z))) {
            if (x < 0.05 && x >= -0.05) {
                x = 0.0;
            } else if (x > 0.0) {
                x -= 0.05;
            } else {
                x += 0.05;
            }

            if (z < 0.05 && z >= -0.05) {
                z = 0.0;
            } else if (z > 0.0) {
                z -= 0.05;
            } else {
                z += 0.05;
            }
        }

        if (movement.x != x || movement.z != z) {
            sim.clipLedged = true;
        }

        if (shouldClipAtLedge(sim)) {
            return new Vec3(x, movement.y, z);
        }

        return movement;
    }

    static boolean shouldClipAtLedge(PlayerMovementSimulation sim) {
        return !sim.input.ignoreClippingAtLedge && (sim.input.sneak || sim.input.forceSafeWalk);
    }

    static boolean shouldConsiderLedgeClipping(PlayerMovementSimulation sim) {
        return sim.onGround || sim.fallDistance < PlayerMovementSimulation.STEP_HEIGHT && !sim.player.level().noCollision(
                sim.player,
                sim.boundingBox.move(0.0, sim.fallDistance - PlayerMovementSimulation.STEP_HEIGHT, 0.0)
        );
    }

    static Vec3 applyClimbingSpeed(PlayerMovementSimulation sim, Vec3 motion) {
        if (!isClimbing(sim)) {
            return motion;
        }

        double x = Mth.clamp(motion.x, -0.15f, 0.15f);
        double z = Mth.clamp(motion.z, -0.15f, 0.15f);
        double y = Math.max(motion.y, -0.15f);
        if (y < 0.0 && sim.player.isShiftKeyDown()) {
            y = 0.0;
        }
        return new Vec3(x, y, z);
    }

    static boolean isClimbing(PlayerMovementSimulation sim) {
        if (sim.player.getAbilities().flying || sim.player.isSpectator()) {
            return false;
        }

        BlockPos blockPos = BlockPos.containing(sim.pos);
        BlockState blockState = sim.player.level().getBlockState(blockPos);
        if (sim.gliding && blockState.is(BlockTags.CAN_GLIDE_THROUGH)) {
            return false;
        }
        if (blockState.is(BlockTags.CLIMBABLE)) {
            return true;
        }
        return blockState.getBlock() instanceof TrapDoorBlock && canEnterTrapdoor(sim, blockPos, blockState);
    }

    static float getVelocityMultiplier(PlayerMovementSimulation sim) {
        if (sim.player.getAbilities().flying || sim.gliding) {
            return 1.0f;
        }

        BlockState state = sim.player.level().getBlockState(BlockPos.containing(sim.pos));
        float multiplier = state.getBlock().getSpeedFactor();
        if (!state.is(Blocks.WATER) && !state.is(Blocks.BUBBLE_COLUMN)) {
            if (multiplier == 1.0f) {
                multiplier = sim.player.level().getBlockState(sim.getVelocityAffectingPos()).getBlock().getSpeedFactor();
            }
        }
        return multiplier;
    }

    private static boolean canEnterTrapdoor(PlayerMovementSimulation sim, BlockPos pos, BlockState state) {
        if (!(Boolean) state.getValue(TrapDoorBlock.OPEN)) {
            return false;
        }

        BlockState belowState = sim.player.level().getBlockState(pos.below());
        return belowState.is(Blocks.LADDER) && belowState.getValue(LadderBlock.FACING) == state.getValue(TrapDoorBlock.FACING);
    }

    static void move(PlayerMovementSimulation sim, Vec3 movement) {
        Vec3 adjusted = movement.lengthSqr() == 0.0
                ? movement
                : Entity.collideBoundingBox(sim.player, movement, sim.boundingBox, sim.player.level(), List.of());
        if (adjusted.lengthSqr() > 1.0E-7 || movement.lengthSqr() - adjusted.lengthSqr() < 1.0E-7) {
            sim.pos = sim.pos.add(adjusted);
            EntityDimensions dimensions = sim.player.getDimensions(sim.player.getPose());
            sim.boundingBox = dimensions.makeBoundingBox(sim.pos);
        }
        boolean xCollision = movement.x != adjusted.x;
        boolean yCollision = movement.y != adjusted.y;
        boolean zCollision = movement.z != adjusted.z;
        sim.horizontalCollision = xCollision || zCollision;
        sim.onGround = yCollision && movement.y < 0.0;
        if (sim.onGround) {
            sim.fallDistance = 0.0;
        } else if (adjusted.y < 0.0) {
            sim.fallDistance -= adjusted.y;
        }
        if (xCollision || zCollision || yCollision) {
            sim.velocity = new Vec3(xCollision ? 0.0 : sim.velocity.x, yCollision ? 0.0 : sim.velocity.y, zCollision ? 0.0 : sim.velocity.z);
        }
        float velocityMultiplier = getVelocityMultiplier(sim);
        sim.velocity = new Vec3(sim.velocity.x * velocityMultiplier, sim.velocity.y, sim.velocity.z * velocityMultiplier);
    }

    static void applyBubbleColumnEffects(PlayerMovementSimulation sim) {
        BlockPos pos = BlockPos.containing(sim.pos);
        BlockState state = sim.player.level().getBlockState(pos);
        if (!state.is(Blocks.BUBBLE_COLUMN)) {
            return;
        }
        BlockState above = sim.player.level().getBlockState(pos.above());
        boolean surface = above.getCollisionShape(sim.player.level(), pos).isEmpty() && above.getFluidState().isEmpty();
        boolean drag = state.getValue(BubbleColumnBlock.DRAG_DOWN);
        double y;
        if (surface) {
            y = drag ? Math.max(-0.9, sim.velocity.y - 0.03) : Math.min(1.8, sim.velocity.y + 0.1);
        } else {
            y = drag ? Math.max(-0.3, sim.velocity.y - 0.03) : Math.min(0.7, sim.velocity.y + 0.06);
            sim.onGround = true;
        }
        sim.velocity = new Vec3(sim.velocity.x, y, sim.velocity.z);
    }
}
