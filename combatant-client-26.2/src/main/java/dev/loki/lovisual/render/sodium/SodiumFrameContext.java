/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.sodium;

/**
 * Captured Sodium world-renderer state for the current LoVisual frame.
 */
public record SodiumFrameContext(boolean available,
                                 boolean terrainRenderComplete,
                                 int visibleChunkCount,
                                 String debugString) {
    public static final SodiumFrameContext UNAVAILABLE = new SodiumFrameContext(false, false, 0, "unavailable");

    public static SodiumFrameContext available(boolean terrainRenderComplete, int visibleChunkCount, String debugString) {
        return new SodiumFrameContext(true, terrainRenderComplete, Math.max(0, visibleChunkCount), debugString != null ? debugString : "available");
    }
}
