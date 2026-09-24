package dev.loki.lovisual.render.engine.core.policy.fog;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.jetbrains.annotations.Nullable;

public interface FogProvider {
    public FogProvider NONE = () -> null;

    @Nullable GpuBufferSlice fogUniformSlice();
}
