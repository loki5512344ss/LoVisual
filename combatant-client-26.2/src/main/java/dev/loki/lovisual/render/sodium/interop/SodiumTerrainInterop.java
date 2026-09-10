/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.sodium.interop;
import dev.loki.lovisual.render.sodium.bridge.SodiumRenderBridge;


/**
 * Central terrain interop state for Sodium mixins and LoVisual render code.
 */
public final class SodiumTerrainInterop {
    private final SodiumRenderBridge bridge;

    private long terrainUpdatesScheduled;
    private long rebuildsScheduled;
    private long interopErrors;

    public SodiumTerrainInterop(SodiumRenderBridge bridge) {
        this.bridge = bridge;
    }

    public void beginFrame() {
        terrainUpdatesScheduled = 0L;
        rebuildsScheduled = 0L;
        interopErrors = 0L;
    }

    public void scheduleTerrainUpdate() {
        bridge.scheduleTerrainUpdate();
    }

    public void scheduleRebuildForBlockArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean important) {
        bridge.scheduleRebuildForBlockArea(minX, minY, minZ, maxX, maxY, maxZ, important);
    }

    public void recordTerrainUpdateScheduled() {
        terrainUpdatesScheduled++;
    }

    public void recordRebuildScheduled() {
        rebuildsScheduled++;
    }

    public void recordInteropError() {
        interopErrors++;
    }

    public SodiumTerrainInteropStatsSnapshot statsSnapshot() {
        return new SodiumTerrainInteropStatsSnapshot(
                terrainUpdatesScheduled,
                rebuildsScheduled,
                interopErrors
        );
    }
}
