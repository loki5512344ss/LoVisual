package dev.loki.lovisual.render.engine.renderer.ui.runtime.input.event;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNode;

public record UiHitResult(UiNode node, float localX, float localY) {
    public static final UiHitResult MISS = new UiHitResult(null, 0.0f, 0.0f);

    public boolean hit() {
        return node != null;
    }
}
