package dev.loki.lovisual.render.engine.core.policy;

/**
 * Placeholder for LoVisual lighting policy. Kept explicit so fullbright/ambient/shaderpack hooks do not leak into renderers.
 */
public interface LightPolicy {
    LightPolicy VANILLA = new LightPolicy() {
    };
}
