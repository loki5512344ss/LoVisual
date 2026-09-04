/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.rhi.resource;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;

/**
 * RHI-facing sampler/texture utility surface. Mojang still owns actual sampler cache for now.
 */
public final class SamplerCache {
    public GpuSampler get(AddressMode u, AddressMode v, FilterMode min, FilterMode mag, boolean mipmap) {
        return RenderSystem.getSamplerCache().getSampler(u, v, min, mag, mipmap);
    }

    public GpuTextureView createTextureView(com.mojang.blaze3d.textures.GpuTexture texture) {
        return RenderSystem.getDevice().createTextureView(texture);
    }
}
