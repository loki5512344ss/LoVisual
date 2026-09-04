/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.core;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public record FramebufferContext(@Nullable RenderTarget mainFramebuffer) {
    public static FramebufferContext capture() {
        Minecraft mc = Minecraft.getInstance();
        return new FramebufferContext(mc != null ? mc.gameRenderer.mainRenderTarget() : null);
    }
}
