/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.core.policy.fog;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.jetbrains.annotations.Nullable;

public interface FogProvider {
    public FogProvider NONE = () -> null;

    @Nullable GpuBufferSlice fogUniformSlice();
}
