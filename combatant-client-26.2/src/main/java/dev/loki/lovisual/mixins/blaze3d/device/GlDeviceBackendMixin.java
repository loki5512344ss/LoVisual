/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.blaze3d.device;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.FrameBufferCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import dev.loki.lovisual.mixininterface.IGlBackendInfo;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlDevice")
public abstract class GlDeviceBackendMixin implements IGlBackendInfo {
    @Override
    @Invoker("directStateAccess")
    public abstract DirectStateAccess lovisual$directStateAccess();

    @Override
    @Invoker("frameBufferCache")
    public abstract FrameBufferCache lovisual$frameBufferCache();
}
