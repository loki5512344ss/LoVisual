/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.rhi.blit;

import com.mojang.blaze3d.textures.GpuTextureView;

public interface TextureBlitter {
    /**
     * Returns true if the copy was handled by a backend texture-copy command.
     */
    boolean copyFast(GpuTextureView src, GpuTextureView dst);
}
