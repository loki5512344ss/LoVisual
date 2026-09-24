package dev.loki.lovisual.mixininterface.device;

public interface IGpuDevice {
    void lovisual$pushScissor(int x, int y, int width, int height);

    void lovisual$popScissor();
}
