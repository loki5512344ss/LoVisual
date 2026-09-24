package dev.loki.lovisual.render.engine.core.policy;

import com.mojang.blaze3d.textures.GpuTextureView;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface DepthProvider {
    DepthProvider NONE = () -> null;

    @Nullable GpuTextureView depthAttachment();
}
