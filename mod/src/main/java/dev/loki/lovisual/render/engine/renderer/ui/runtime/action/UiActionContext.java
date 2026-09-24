package dev.loki.lovisual.render.engine.renderer.ui.runtime.action;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNode;

public record UiActionContext(UiNode node, UiActionRef ref, Object event) {
}
