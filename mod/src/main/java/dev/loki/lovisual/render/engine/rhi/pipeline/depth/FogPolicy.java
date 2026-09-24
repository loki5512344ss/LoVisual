package dev.loki.lovisual.render.engine.rhi.pipeline.depth;

public enum FogPolicy {
    NONE,
    VANILLA,
    LOVISUAL,
    SHADERPACK;

    public boolean requiresWorldFogUniform() {
        return this == VANILLA || this == LOVISUAL || this == SHADERPACK;
    }
}
