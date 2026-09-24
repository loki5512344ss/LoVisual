package dev.loki.lovisual.render.sodium.visibility;

public record SodiumVisibilityStatsSnapshot(long queries,
                                            long accepts,
                                            long rejects,
                                            long bypasses,
                                            long unavailableBypasses,
                                            long optOutBypasses,
                                            long errors) {
    public static final SodiumVisibilityStatsSnapshot EMPTY = new SodiumVisibilityStatsSnapshot(0, 0, 0, 0, 0, 0, 0);
}
