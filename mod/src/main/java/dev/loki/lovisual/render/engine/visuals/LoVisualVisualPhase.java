package dev.loki.lovisual.render.engine.visuals;

/**
 * Render graph ordering for the custom world visual stack.
 */
public enum LoVisualVisualPhase {
    PREPARE,
    SHADOWS,
    VOLUMETRICS,
    REFLECTIONS,
    COMPOSITE
}
