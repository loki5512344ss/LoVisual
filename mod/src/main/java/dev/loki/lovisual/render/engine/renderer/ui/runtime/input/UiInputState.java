package dev.loki.lovisual.render.engine.renderer.ui.runtime.input;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNode;

public final class UiInputState {
    private UiNode hoveredNode;
    private UiNode pressedNode;
    private UiNode focusedNode;
    private UiNode scrollOwner;

    public UiNode hoveredNode() {
        return hoveredNode;
    }

    public UiNode pressedNode() {
        return pressedNode;
    }

    public UiNode focusedNode() {
        return focusedNode;
    }

    public UiNode scrollOwner() {
        return scrollOwner;
    }

    public void setHoveredNode(UiNode hoveredNode) {
        this.hoveredNode = hoveredNode;
    }

    public void setPressedNode(UiNode pressedNode) {
        this.pressedNode = pressedNode;
    }

    public void setFocusedNode(UiNode focusedNode) {
        this.focusedNode = focusedNode;
    }

    public void setScrollOwner(UiNode scrollOwner) {
        this.scrollOwner = scrollOwner;
    }
}
