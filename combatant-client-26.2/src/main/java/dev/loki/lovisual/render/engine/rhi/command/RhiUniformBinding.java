/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.rhi.command;

import com.mojang.blaze3d.buffers.GpuBufferSlice;

public record RhiUniformBinding(String name, GpuBufferSlice slice) {
}
