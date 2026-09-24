package dev.loki.lovisual.render.engine.renderer.ui.draw.edge;

/**
 * Per-edge modifier for flexible UI boxes.
 */
public enum UiEdgeKind {
    STRAIGHT(0.0f),
    NOTCHED(1.0f),
    INSET(2.0f),
    CUT(3.0f),
    CUSTOM(8.0f);

    private final float shaderCode;

    UiEdgeKind(float shaderCode) {
        this.shaderCode = shaderCode;
    }

    public float shaderCode() {
        return shaderCode;
    }
}
