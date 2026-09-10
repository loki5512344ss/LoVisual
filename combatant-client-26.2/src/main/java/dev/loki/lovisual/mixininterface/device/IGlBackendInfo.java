/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixininterface.device;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.FrameBufferCache;

public interface IGlBackendInfo {
    DirectStateAccess lovisual$directStateAccess();

    FrameBufferCache lovisual$frameBufferCache();
}
