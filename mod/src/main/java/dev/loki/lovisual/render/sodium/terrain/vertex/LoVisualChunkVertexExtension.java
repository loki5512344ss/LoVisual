package dev.loki.lovisual.render.sodium.terrain.vertex;

public interface LoVisualChunkVertexExtension {
    public void lovisual$setSurfaceFlags(int surfaceFlags);

    public int lovisual$getSurfaceFlags();

    public void lovisual$copyData(LoVisualChunkVertexExtension dest);
}
