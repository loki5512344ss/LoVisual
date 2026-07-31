/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.api.v0.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.postprocess.PostProcessPass;
import net.minecraft.client.Minecraft;

public record LoVisualPostProcessContext(
        String addonId,
        String passId,
        PostProcessPass.Phase phase,
        GpuTextureView source,
        GpuTextureView destination,
        float tickDelta
) {
    public Minecraft client() {
        return Minecraft.getInstance();
    }
}
