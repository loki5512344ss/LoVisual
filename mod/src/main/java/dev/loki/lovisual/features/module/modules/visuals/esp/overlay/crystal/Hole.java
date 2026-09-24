package dev.loki.lovisual.features.module.modules.visuals.esp.overlay.crystal;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/**
 * A detected safe hole: its world-space bounding {@code box}, the resolved ARGB color for the
 * protection type and the set of {@code positions} it consumes (used to avoid overlapping
 * duplicates during scanning). Produced by {@link HoleScanner}, drawn by {@link HoleRenderer}.
 */
public record Hole(AABB box, int argb, List<BlockPos> positions) {
}
