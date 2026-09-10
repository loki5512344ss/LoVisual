/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

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
