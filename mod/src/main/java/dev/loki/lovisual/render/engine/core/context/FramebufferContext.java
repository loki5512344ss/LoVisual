package dev.loki.lovisual.render.engine.core.context;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public record FramebufferContext(@Nullable RenderTarget mainFramebuffer) {
    public static FramebufferContext capture() {
        Minecraft mc = Minecraft.getInstance();
        return new FramebufferContext(mc != null ? mc.gameRenderer.mainRenderTarget() : null);
    }
}
