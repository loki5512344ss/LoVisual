/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.text.command;

public record TextCommandStatsSnapshot(int recorded,
                                       int submitted,
                                       int vanilla,
                                       int bitmap,
                                       int msdf,
                                       int worldPlacements,
                                       int effect,
                                       int clipped,
                                       int glyphs,
                                       int meshUploads,
                                       int routerCacheHits,
                                       int routerCacheMisses,
                                       int routerFallbackMisses,
                                       int adjacentBatchedCommands,
                                       int directAdjacentBatchedCommands,
                                       long uploadedVertexBytes,
                                       long uploadedIndexBytes) {
    /**
     * Compatibility alias for old profiler labels.
     */
    @Deprecated
    public int world() {
        return worldPlacements;
    }
}
