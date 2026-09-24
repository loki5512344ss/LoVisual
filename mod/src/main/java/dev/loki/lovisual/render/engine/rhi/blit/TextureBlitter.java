package dev.loki.lovisual.render.engine.rhi.blit;

import com.mojang.blaze3d.textures.GpuTextureView;

public interface TextureBlitter {
    /**
     * Returns true if the copy was handled by a backend texture-copy command.
     */
    boolean copyFast(GpuTextureView src, GpuTextureView dst);
}
