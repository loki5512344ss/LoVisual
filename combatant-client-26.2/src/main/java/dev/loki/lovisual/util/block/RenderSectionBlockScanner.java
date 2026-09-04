/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.ConcurrentHashMap;

public enum RenderSectionBlockScanner {
    ;
    private static final ConcurrentHashMap<Long, BlockState> BLOCKS = new ConcurrentHashMap<>();

    public static void recordBlock(BlockPos pos, BlockState state) {
        if (pos == null || state == null) {
            return;
        }

        long key = pos.asLong();
        if (state.isAir()) {
            BLOCKS.remove(key);
        } else {
            BLOCKS.put(key, state);
        }
    }

    public static void clear() {
        BLOCKS.clear();
    }

    public static BlockState getCachedState(BlockPos pos) {
        return pos == null ? null : BLOCKS.get(pos.asLong());
    }

    public record ScanResult(BlockPos pos, BlockState state, boolean visible) {
    }
}
