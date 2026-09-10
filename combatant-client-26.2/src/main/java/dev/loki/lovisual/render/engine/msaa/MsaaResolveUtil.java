/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

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
