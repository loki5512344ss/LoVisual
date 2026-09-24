package dev.loki.lovisual.mixininterface.device;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.textures.GpuTexture;

@Deprecated
public interface IMsaaDevice {
    GpuTexture lovisual$createMsaaTexture(String label,
                                           int usage,
                                           GpuFormat format,
                                           int width,
                                           int height,
                                           int samples);
}
