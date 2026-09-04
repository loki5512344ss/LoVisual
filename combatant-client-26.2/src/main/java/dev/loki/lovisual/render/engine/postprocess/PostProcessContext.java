/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.postprocess;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.jetbrains.annotations.Nullable;

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
