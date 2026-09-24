package dev.loki.lovisual.render.engine.rhi.pipeline.pipeline;

/**
 * High-level render domain used by pass compilers and profilers.
 */
public enum PipelineDomain {
    WORLD,
    UI,
    FULLSCREEN,
    TEXT,
    POSTPROCESS,
    UNKNOWN
}
