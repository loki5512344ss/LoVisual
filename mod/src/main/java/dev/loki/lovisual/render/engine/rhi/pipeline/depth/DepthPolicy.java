package dev.loki.lovisual.render.engine.rhi.pipeline.depth;

public enum DepthPolicy {
    NONE,
    MAIN_FRAMEBUFFER,
    PRE_TRANSLUCENT,
    CUSTOM;

    public boolean needsDepthAttachment() {
        return this == MAIN_FRAMEBUFFER || this == PRE_TRANSLUCENT || this == CUSTOM;
    }
}
