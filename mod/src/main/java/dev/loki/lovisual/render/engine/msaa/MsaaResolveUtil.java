package dev.loki.lovisual.render.engine.msaa;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;

@Deprecated
public enum MsaaResolveUtil {
    ;

    public static boolean resolve(RenderTarget src, RenderTarget dst, boolean color, boolean depth) {
        RenderSystem.assertOnRenderThread();
        return LoVisualRenderSystem.rhi().msaa().resolve(src, dst, color, depth);
    }
}
