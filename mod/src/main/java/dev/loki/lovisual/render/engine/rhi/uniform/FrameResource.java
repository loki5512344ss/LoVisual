package dev.loki.lovisual.render.engine.rhi.uniform;

public interface FrameResource {
    String name();

    boolean usedThisFrame();

    void onFramePresented();
}
