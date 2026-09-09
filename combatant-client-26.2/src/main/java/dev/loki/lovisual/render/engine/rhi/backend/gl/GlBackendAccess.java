/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.rhi.backend.gl;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;
import dev.loki.lovisual.mixininterface.IGlBackendInfo;
import dev.loki.lovisual.mixins.accessors.blaze3d.GpuDeviceAccessor;

public enum GlBackendAccess {
    ;

    public static @Nullable IGlBackendInfo current() {
        GpuDevice device = RenderSystem.tryGetDevice();
        if (!(device instanceof GpuDeviceAccessor accessor)) {
            return null;
        }
        GpuDeviceBackend backend = accessor.lovisual$getBackend();
        return backend instanceof IGlBackendInfo glBackend ? glBackend : null;
    }
}
