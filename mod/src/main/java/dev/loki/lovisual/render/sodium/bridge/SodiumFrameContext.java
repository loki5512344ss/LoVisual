package dev.loki.lovisual.render.sodium.bridge;

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
