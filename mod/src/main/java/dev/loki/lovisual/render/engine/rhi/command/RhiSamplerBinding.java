package dev.loki.lovisual.render.engine.rhi.command;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;

public record RhiSamplerBinding(String name, GpuTextureView view, GpuSampler sampler) {
}
