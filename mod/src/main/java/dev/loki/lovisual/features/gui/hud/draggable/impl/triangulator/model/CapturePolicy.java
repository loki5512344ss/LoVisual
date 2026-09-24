package dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.model;

/** Tunables the tracker needs for a single tick. */
public record CapturePolicy(int minObservationTicks,
                            double stabilityThreshold,
                            double minPairAngleDeg,
                            boolean autoReset,
                            double autoResetDistance,
                            int maxCachedThrows,
                            long cacheLifetimeMs) {
}
