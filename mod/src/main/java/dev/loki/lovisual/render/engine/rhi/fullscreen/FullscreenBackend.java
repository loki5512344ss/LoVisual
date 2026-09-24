package dev.loki.lovisual.render.engine.rhi.fullscreen;

import com.mojang.blaze3d.buffers.GpuBuffer;
import dev.loki.lovisual.render.engine.rhi.resource.GpuMeshHandle;

public interface FullscreenBackend extends AutoCloseable {
    void ensureInitialized();

    GpuMeshHandle quad();

    GpuBuffer vertexBuffer();

    GpuBuffer indexBuffer();

    @Override
    void close();
}
