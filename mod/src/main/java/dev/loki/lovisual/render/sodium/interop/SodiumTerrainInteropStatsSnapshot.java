package dev.loki.lovisual.render.sodium.interop;

public record SodiumTerrainInteropStatsSnapshot(long terrainUpdatesScheduled,
                                                long rebuildsScheduled,
                                                long interopErrors) {
    public static final SodiumTerrainInteropStatsSnapshot EMPTY = new SodiumTerrainInteropStatsSnapshot(0, 0, 0);
}
