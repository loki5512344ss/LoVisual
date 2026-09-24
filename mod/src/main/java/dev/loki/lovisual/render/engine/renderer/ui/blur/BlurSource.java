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
