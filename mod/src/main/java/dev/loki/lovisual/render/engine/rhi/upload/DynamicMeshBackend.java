package dev.loki.lovisual.render.engine.rhi.upload;

import dev.loki.lovisual.render.engine.rhi.resource.GpuMeshHandle;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

public interface DynamicMeshBackend extends AutoCloseable {
    GpuMeshHandle upload(MeshBuilder mesh);

    void beginFrame(long frameId);

    void endSubmission();

    void framePresented();

    @Override
    void close();
}
