package dev.loki.lovisual.features.module.modules.visuals.esp.overlay.crystal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blast-safety predicates for {@link HoleScanner}: which blocks are replaceable / bedrock /
 * indestructible, and whether a single cell or a multi-cell shape is a valid safe hole (fully
 * surrounded by bedrock, or by at least one indestructible with the rest no weaker). Stateless
 * static helpers over the client world. Extracted so the module stays under the file-size cap.
 */
final class BlockSafety {

    static final Direction[] HORIZONTAL = {
            Direction.NORTH,
            Direction.SOUTH,
            Direction.EAST,
            Direction.WEST
    };

    private BlockSafety() { }

    static boolean validBedrock(ClientLevel level, BlockPos pos) {
        return isBedrock(level, pos.below())
                && isBedrock(level, pos.east())
                && isBedrock(level, pos.west())
                && isBedrock(level, pos.south())
                && isBedrock(level, pos.north())
                && isReplaceable(level, pos)
                && isReplaceable(level, pos.above())
                && isReplaceable(level, pos.above(2));
    }

    static boolean validIndestructible(ClientLevel level, BlockPos pos) {
        return !validBedrock(level, pos)
                && isSafeBlock(level, pos.below())
                && isSafeBlock(level, pos.east())
                && isSafeBlock(level, pos.west())
                && isSafeBlock(level, pos.south())
                && isSafeBlock(level, pos.north())
                && isReplaceable(level, pos)
                && isReplaceable(level, pos.above())
                && isReplaceable(level, pos.above(2));
    }

    static boolean validBedrockShape(ClientLevel level, List<BlockPos> positions) {
        Set<BlockPos> inside = new HashSet<>(positions);
        for (BlockPos check : positions) {
            if (!isReplaceable(level, check) || !isReplaceable(level, check.above()) || !isReplaceable(level, check.above(2))) {
                return false;
            }
            if (!isBedrock(level, check.below())) {
                return false;
            }
            for (Direction direction : HORIZONTAL) {
                BlockPos surround = check.relative(direction);
                if (!inside.contains(surround) && !isBedrock(level, surround)) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean validIndestructibleShape(ClientLevel level, List<BlockPos> positions) {
        Set<BlockPos> inside = new HashSet<>(positions);
        boolean hasIndestructible = false;
        for (BlockPos check : positions) {
            if (!isReplaceable(level, check) || !isReplaceable(level, check.above()) || !isReplaceable(level, check.above(2))) {
                return false;
            }

            BlockPos below = check.below();
            if (isIndestructible(level, below)) {
                hasIndestructible = true;
            } else if (!isBedrock(level, below)) {
                return false;
            }

            for (Direction direction : HORIZONTAL) {
                BlockPos surround = check.relative(direction);
                if (inside.contains(surround)) {
                    continue;
                }
                if (isIndestructible(level, surround)) {
                    hasIndestructible = true;
                } else if (!isBedrock(level, surround)) {
                    return false;
                }
            }
        }
        return hasIndestructible;
    }

    static boolean isSafeBlock(ClientLevel level, BlockPos pos) {
        return isBedrock(level, pos) || isIndestructible(level, pos);
    }

    static boolean isBedrock(ClientLevel level, BlockPos pos) {
        return level != null && block(level, pos) == Blocks.BEDROCK;
    }

    static boolean isIndestructible(ClientLevel level, BlockPos pos) {
        Block block = block(level, pos);
        return block == Blocks.OBSIDIAN
                || block == Blocks.NETHERITE_BLOCK
                || block == Blocks.CRYING_OBSIDIAN
                || block == Blocks.RESPAWN_ANCHOR;
    }

    static boolean isReplaceable(ClientLevel level, BlockPos pos) {
        if (level == null || pos == null || pos.getY() < level.getMinY() || pos.getY() >= level.getMaxY()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced() || !state.getFluidState().isEmpty();
    }

    private static Block block(ClientLevel level, BlockPos pos) {
        if (level == null || pos == null || pos.getY() < level.getMinY() || pos.getY() >= level.getMaxY()) {
            return Blocks.AIR;
        }
        return level.getBlockState(pos).getBlock();
    }
}
