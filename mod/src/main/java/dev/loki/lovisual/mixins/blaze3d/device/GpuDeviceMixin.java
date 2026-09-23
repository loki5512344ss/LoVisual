/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.blaze3d.device;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import org.spongepowered.asm.mixin.Mixin;
import dev.loki.lovisual.mixininterface.device.IGpuDevice;
import dev.loki.lovisual.mixininterface.device.IMsaaDevice;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.rhi.scissor.GlobalScissorState;

@Mixin(GpuDevice.class)
public abstract class GpuDeviceMixin implements IGpuDevice, IMsaaDevice {
    @Override
    public void lovisual$pushScissor(int x, int y, int width, int height) {
        GlobalScissorState.push(x, y, width, height);
    }

    @Override
    public void lovisual$popScissor() {
        GlobalScissorState.pop();
    }

    @Override
    public GpuTexture lovisual$createMsaaTexture(String label, int usage, GpuFormat format, int width, int height, int samples) {
        return LoVisualRenderSystem.rhi().msaa().createTexture(label, usage, format, width, height, samples);
    }
}
