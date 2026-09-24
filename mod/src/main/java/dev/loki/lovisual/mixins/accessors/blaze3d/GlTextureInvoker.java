package dev.loki.lovisual.mixins.accessors.blaze3d;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.opengl.FrameBufferCache;
import com.mojang.blaze3d.opengl.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GlTexture.class)
public interface GlTextureInvoker {
    @Invoker("<init>")
    static GlTexture lovisual$create(int usage,
                                      String label,
                                      GpuFormat format,
                                      int width,
                                      int height,
                                      int depthOrLayers,
                                      int mipLevels,
                                      int glId,
                                      FrameBufferCache frameBufferCache) {
        throw new AssertionError();
    }
}

