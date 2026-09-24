package dev.loki.lovisual.render.engine.postprocess.graph.resource;

import com.mojang.blaze3d.textures.GpuTextureView;
import org.jetbrains.annotations.Nullable;

/**
 * Owner for temporal/history resources used by motion blur and future temporal effects.
 */
public final class HistoryBufferManager {
    private @Nullable GpuTextureView previousColor;
    private long previousFrameId = -1L;

    public void update(long frameId, @Nullable GpuTextureView finalColor) {
        previousFrameId = frameId;
        previousColor = finalColor;
    }

    public @Nullable GpuTextureView previousColor() {
        return previousColor;
    }

    public long previousFrameId() {
        return previousFrameId;
    }

    public void invalidate() {
        previousColor = null;
        previousFrameId = -1L;
    }
}
