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
