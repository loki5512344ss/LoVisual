/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.blur;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;

public final class BlurSource {
    public final GpuTextureView view;
    public final GpuSampler sampler;

    public BlurSource(GpuTextureView view, GpuSampler sampler) {
        this.view = view;
        this.sampler = sampler;
    }
}
