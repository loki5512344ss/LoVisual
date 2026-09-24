package dev.loki.lovisual.features.module.modules.visuals.esp.overlay.crystal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.util.block.scan.BlockSearchUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Discovers safe {@link Hole}s around the player for {@code CrystalHoles}. Scans replaceable
 * cells within the configured range and, per seed position, resolves the largest matching shape
 * (single cell, two-block or 2x2 quad) whose surroundings are blast-safe (see {@link BlockSafety});
 * already-consumed positions and overlapping boxes are skipped so each hole is reported once.
 * Box height comes from the module's {@code height} setting. Extracted so the module class stays
 * under the file-size cap.
 */
public final class HoleScanner {

    private final NumberValue<Integer> rangeXZ;
    private final NumberValue<Integer> rangeY;
    private final NumberValue<Float> height;
    private final RGBAColorValue indestructibleColor;
    private final RGBAColorValue bedrockColor;

    public HoleScanner(NumberValue<Integer> rangeXZ, NumberValue<Integer> rangeY, NumberValue<Float> height,
                       RGBAColorValue indestructibleColor, RGBAColorValue bedrockColor) {
        this.rangeXZ = rangeXZ;
        this.rangeY = rangeY;
        this.height = height;
        this.indestructibleColor = indestructibleColor;
        this.bedrockColor = bedrockColor;
    }

    public List<Hole> scan(ClientLevel level, Vec3 center) {
        int xz = rangeXZ.get();
        int y = rangeY.get();
        List<Hole> found = new ArrayList<>();
        List<AABB> acceptedBoxes = new ArrayList<>();
        Set<BlockPos> consumed = new HashSet<>();

        for (BlockSearchUtil.BlockSearchEntry entry : BlockSearchUtil.searchBlocksInCuboid(
                level,
                center,
                xz,
                y,
                (pos, state) -> BlockSafety.isReplaceable(level, pos)
                        && Math.abs((pos.getX() + 0.5) - center.x) <= xz
                        && Math.abs((pos.getY() + 0.5) - center.y) <= y
                        && Math.abs((pos.getZ() + 0.5) - center.z) <= xz
        )) {
            BlockPos pos = entry.pos();
            if (consumed.contains(pos)) {
                continue;
            }

            Hole hole = resolveHole(level, pos);
            if (hole == null || intersectsAny(hole.box(), acceptedBoxes)) {
                continue;
            }

            found.add(hole);
            acceptedBoxes.add(hole.box());
            consumed.addAll(hole.positions());
        }

        return List.copyOf(found);
    }

    private Hole resolveHole(ClientLevel level, BlockPos pos) {
        if (BlockSafety.validIndestructible(level, pos)) {
            return new Hole(box(pos, 1, 1), indestructibleColor.getArgb(), List.of(pos));
        }
        if (BlockSafety.validBedrock(level, pos)) {
            return new Hole(box(pos, 1, 1), bedrockColor.getArgb(), List.of(pos));
        }

        List<BlockPos> two = twoBlockShape(level, pos);
        if (two != null) {
            Hole hole = shapeHole(level, two);
            if (hole != null) return hole;
        }

        List<BlockPos> quad = quadShape(level, pos);
        if (quad != null) {
            Hole hole = shapeHole(level, quad);
            if (hole != null) return hole;
        }

        return null;
    }

    /** Resolve a multi-cell shape's protection type: bedrock takes priority, else indestructible. */
    private Hole shapeHole(ClientLevel level, List<BlockPos> positions) {
        if (BlockSafety.validBedrockShape(level, positions)) {
            return new Hole(box(positions), bedrockColor.getArgb(), positions);
        }
        if (BlockSafety.validIndestructibleShape(level, positions)) {
            return new Hole(box(positions), indestructibleColor.getArgb(), positions);
        }
        return null;
    }

    private static List<BlockPos> twoBlockShape(ClientLevel level, BlockPos pos) {
        if (!BlockSafety.isReplaceable(level, pos)) {
            return null;
        }
        for (Direction direction : BlockSafety.HORIZONTAL) {
            BlockPos other = pos.relative(direction);
            if (BlockSafety.isReplaceable(level, other)) {
                return List.of(pos, other);
            }
        }
        return null;
    }

    private static List<BlockPos> quadShape(ClientLevel level, BlockPos pos) {
        if (!BlockSafety.isReplaceable(level, pos)) {
            return null;
        }

        List<BlockPos> eastSouth = quadIfReplaceable(level, pos, pos.east(), pos.south(), pos.east().south());
        if (eastSouth != null) return eastSouth;

        List<BlockPos> westNorth = quadIfReplaceable(level, pos, pos.west(), pos.north(), pos.west().north());
        if (westNorth != null) return westNorth;

        List<BlockPos> eastNorth = quadIfReplaceable(level, pos, pos.east(), pos.north(), pos.east().north());
        if (eastNorth != null) return eastNorth;

        return quadIfReplaceable(level, pos, pos.west(), pos.south(), pos.west().south());
    }

    private static List<BlockPos> quadIfReplaceable(ClientLevel level, BlockPos a, BlockPos b, BlockPos c, BlockPos d) {
        if (BlockSafety.isReplaceable(level, b) && BlockSafety.isReplaceable(level, c) && BlockSafety.isReplaceable(level, d)) {
            return List.of(a, b, c, d);
        }
        return null;
    }

    private AABB box(BlockPos pos, int widthX, int widthZ) {
        return new AABB(
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                pos.getX() + widthX,
                pos.getY() + height.get(),
                pos.getZ() + widthZ
        );
    }

    private AABB box(List<BlockPos> positions) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        return new AABB(minX, minY, minZ, maxX + 1, minY + height.get(), maxZ + 1);
    }

    private static boolean intersectsAny(AABB box, List<AABB> boxes) {
        for (AABB other : boxes) {
            if (other.intersects(box)) {
                return true;
            }
        }
        return false;
    }
}
