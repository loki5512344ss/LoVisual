package dev.loki.lovisual.util.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.client.Minecraft;

/**
 * Samples the target's bounding box with rays back to the explosion origin to compute
 * how exposed the entity is (0..1). Handles both a step-fraction path and a capped
 * index path used when the entity is large enough that the naive loop would be too costly.
 * Package-private: called only from {@link ExplosionDamageUtil}.
 */
final class ExplosionExposure {
    private static final int MAX_EXPOSURE_TOTAL_SAMPLES = 384;
    private static final int MIN_CAPPED_SAMPLES_PER_AXIS = 2;
    private ExplosionExposure() {}
    /** Entry point: choose the raw or capped sampling path based on estimated sample count. */
    static ExposureDebug of(LivingEntity entity, Vec3 source, AABB box, boolean ignoreTerrain, BlockPos ghostObsidianPos) {
        return compute(entity, source, box, ignoreTerrain ? 600.0f : null, ghostObsidianPos);
    }
    private static ExposureDebug compute(LivingEntity entity, Vec3 source, AABB box, Float maxBlastResistance, BlockPos ghostObsidianPos) {
        double sizeX = box.maxX - box.minX;
        double sizeY = box.maxY - box.minY;
        double sizeZ = box.maxZ - box.minZ;
        double stepX = 1.0 / (sizeX * 2.0 + 1.0);
        double stepY = 1.0 / (sizeY * 2.0 + 1.0);
        double stepZ = 1.0 / (sizeZ * 2.0 + 1.0);
        double offsetX = (1.0 - Math.floor(1.0 / stepX) * stepX) / 2.0;
        double offsetZ = (1.0 - Math.floor(1.0 / stepZ) * stepZ) / 2.0;
        if (stepX < 0.0 || stepY < 0.0 || stepZ < 0.0) {
            return new ExposureDebug(0.0f, 0, 0, null, null, null, false, false);
        }
        int sampleCountX = estimateSampleCount(sizeX);
        int sampleCountY = estimateSampleCount(sizeY);
        int sampleCountZ = estimateSampleCount(sizeZ);
        long estimated = (long) sampleCountX * sampleCountY * sampleCountZ;
        if (estimated > MAX_EXPOSURE_TOTAL_SAMPLES) {
            return computeCapped(entity, source, box, maxBlastResistance, sampleCountX, sampleCountY, sampleCountZ, estimated, ghostObsidianPos);
        }
        CollisionContext shapeContext = CollisionContext.withPosition(entity, box.minY);
        Accumulator acc = new Accumulator();
        for (double x = 0.0; x <= 1.0; x += stepX) {
            for (double y = 0.0; y <= 1.0; y += stepY) {
                for (double z = 0.0; z <= 1.0; z += stepZ) {
                    Vec3 sample = new Vec3(Mth.lerp(x, box.minX, box.maxX) + offsetX, Mth.lerp(y, box.minY, box.maxY), Mth.lerp(z, box.minZ, box.maxZ) + offsetZ);
                    acc.record(sample, source, shootRay(sample, source, shapeContext, maxBlastResistance, ghostObsidianPos), ghostObsidianPos);
                }
            }
        }
        return acc.toDebug();
    }
    /** Down-samples each axis (never below {@link #MIN_CAPPED_SAMPLES_PER_AXIS}) until the total fits the budget. */
    private static ExposureDebug computeCapped(LivingEntity entity, Vec3 source, AABB box, Float maxBlastResistance,
                                               int baseX, int baseY, int baseZ, long estimated, BlockPos ghostObsidianPos) {
        double scale = Math.cbrt((double) MAX_EXPOSURE_TOTAL_SAMPLES / (double) estimated);
        int sampleCountX = clampSampleCount((int) Math.round(baseX * scale), baseX);
        int sampleCountY = clampSampleCount((int) Math.round(baseY * scale), baseY);
        int sampleCountZ = clampSampleCount((int) Math.round(baseZ * scale), baseZ);
        while ((long) sampleCountX * sampleCountY * sampleCountZ > MAX_EXPOSURE_TOTAL_SAMPLES) {
            if (sampleCountX >= sampleCountY && sampleCountX >= sampleCountZ && sampleCountX > MIN_CAPPED_SAMPLES_PER_AXIS) {
                sampleCountX--;
            } else if (sampleCountY >= sampleCountZ && sampleCountY > MIN_CAPPED_SAMPLES_PER_AXIS) {
                sampleCountY--;
            } else if (sampleCountZ > MIN_CAPPED_SAMPLES_PER_AXIS) {
                sampleCountZ--;
            } else {
                break;
            }
        }
        CollisionContext shapeContext = CollisionContext.withPosition(entity, box.minY);
        Accumulator acc = new Accumulator();
        for (int x = 0; x < sampleCountX; x++) {
            for (int y = 0; y < sampleCountY; y++) {
                for (int z = 0; z < sampleCountZ; z++) {
                    Vec3 sample = new Vec3(
                            sampleCoordinate(box.minX, box.maxX, x, sampleCountX),
                            sampleCoordinate(box.minY, box.maxY, y, sampleCountY),
                            sampleCoordinate(box.minZ, box.maxZ, z, sampleCountZ)
                    );
                    acc.record(sample, source, shootRay(sample, source, shapeContext, maxBlastResistance, ghostObsidianPos), ghostObsidianPos);
                }
            }
        }
        return acc.toDebug();
    }
    private static int estimateSampleCount(double size) {
        return Math.max(2, (int) Math.floor(size * 2.0 + 1.0) + 1);
    }
    private static int clampSampleCount(int proposed, int original) {
        return Math.max(MIN_CAPPED_SAMPLES_PER_AXIS, Math.min(original, proposed));
    }
    private static double sampleCoordinate(double min, double max, int index, int total) {
        if (total <= 1) return (min + max) * 0.5;
        return Mth.lerp((index + 0.5) / total, min, max);
    }
    private static BlockHitResult shootRay(Vec3 from, Vec3 to, CollisionContext ctx, Float maxBlastResistance, BlockPos ghostObsidianPos) {
        return raycast(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ctx), maxBlastResistance, ghostObsidianPos);
    }
    /**
     * Same traversal as vanilla {@code Level.clip} but honours two overlays:
     * a ghost obsidian block (for crystal predictions) and a max-blast-resistance filter
     * (so {@code ignoreTerrain} mode treats weak blocks as air).
     */
    private static BlockHitResult raycast(ClipContext context, Float maxBlastResistance, BlockPos ghostObsidianPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            return BlockHitResult.miss(context.getTo(), Direction.getApproximateNearest(0.0, 0.0, 1.0), BlockPos.containing(context.getTo()));
        }
        return BlockGetter.traverseBlocks(context.getFrom(), context.getTo(), context, (innerContext, blockPos) -> {
            boolean ghostBlock = ghostObsidianPos != null && blockPos.equals(ghostObsidianPos);
            var state = ghostBlock ? Blocks.OBSIDIAN.defaultBlockState() : mc.level.getBlockState(blockPos);
            var fluidState = ghostBlock ? Fluids.EMPTY.defaultFluidState() : mc.level.getFluidState(blockPos);
            if (maxBlastResistance != null && state.getBlock().getExplosionResistance() < maxBlastResistance) {
                state = Blocks.VOID_AIR.defaultBlockState();
            }
            if (maxBlastResistance != null && fluidState.getExplosionResistance() < maxBlastResistance) {
                fluidState = Fluids.EMPTY.defaultFluidState();
            }
            Vec3 start = innerContext.getFrom();
            Vec3 end = innerContext.getTo();
            var blockShape = innerContext.getBlockShape(state, mc.level, blockPos);
            var blockHit = mc.level.clipWithInteractionOverride(start, end, blockPos, blockShape, state);
            var fluidShape = innerContext.getFluidShape(fluidState, mc.level, blockPos);
            var fluidHit = fluidShape.clip(start, end, blockPos);
            double blockDistance = blockHit == null ? Double.MAX_VALUE : start.distanceToSqr(blockHit.getLocation());
            double fluidDistance = fluidHit == null ? Double.MAX_VALUE : start.distanceToSqr(fluidHit.getLocation());
            return blockDistance <= fluidDistance ? blockHit : fluidHit;
        }, innerContext -> {
            Vec3 diff = innerContext.getFrom().subtract(innerContext.getTo());
            return BlockHitResult.miss(innerContext.getTo(), Direction.getApproximateNearest(diff.x, diff.y, diff.z), BlockPos.containing(innerContext.getTo()));
        });
    }
    private static String getBlockDebugName(BlockPos pos, BlockPos ghostObsidianPos) {
        Minecraft mc = Minecraft.getInstance();
        if (ghostObsidianPos != null && ghostObsidianPos.equals(pos)) {
            return BuiltInRegistries.BLOCK.getKey(Blocks.OBSIDIAN).toString();
        }
        if (mc == null || mc.level == null || pos == null) return null;
        return BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(pos).getBlock()).toString();
    }
    /** Mutable hit counter shared by both sampling loops; records the first blocker for debug output. */
    private static final class Accumulator {
        int hits, total;
        Vec3 firstBlockedSample; BlockPos firstBlockedPos; String firstBlockedBlock;
        boolean atSampleCell, atExplosionCell;
        void record(Vec3 sample, Vec3 source, BlockHitResult hit, BlockPos ghost) {
            if (hit.getType() == HitResult.Type.MISS) {
                hits++;
            } else if (firstBlockedPos == null) {
                firstBlockedSample = sample;
                firstBlockedPos = hit.getBlockPos();
                firstBlockedBlock = getBlockDebugName(hit.getBlockPos(), ghost);
                atSampleCell = hit.getBlockPos().equals(BlockPos.containing(sample));
                atExplosionCell = hit.getBlockPos().equals(BlockPos.containing(source));
            }
            total++;
        }
        ExposureDebug toDebug() {
            return new ExposureDebug(total <= 0 ? 0.0f : (float) hits / (float) total, hits, total, firstBlockedSample, firstBlockedPos, firstBlockedBlock, atSampleCell, atExplosionCell);
        }
    }
    record ExposureDebug(float exposure, int clearRays, int totalRays, Vec3 blockerSample, BlockPos blockerPos,
                         String blockerBlock, boolean blockerAtSampleCell, boolean blockerAtExplosionCell) {}
}
