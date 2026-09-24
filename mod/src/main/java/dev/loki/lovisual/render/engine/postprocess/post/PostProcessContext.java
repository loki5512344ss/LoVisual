package dev.loki.lovisual.render.engine.postprocess.post;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.jetbrains.annotations.Nullable;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessPass;

public record PostProcessContext(
        PostProcessPass.Phase phase,
        float tickDelta,
        RenderTarget mainFramebuffer,
        GpuTextureView mainColor,
        @Nullable GpuTextureView mainDepth,
        @Nullable GpuTextureView preTranslucentDepth,
        @Nullable GpuTextureView staticWorldDepth,
        int width,
        int height
) {
}
