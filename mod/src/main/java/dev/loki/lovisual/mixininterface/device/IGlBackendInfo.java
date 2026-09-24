package dev.loki.lovisual.mixininterface.device;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.FrameBufferCache;

public interface IGlBackendInfo {
    DirectStateAccess lovisual$directStateAccess();

    FrameBufferCache lovisual$frameBufferCache();
}
