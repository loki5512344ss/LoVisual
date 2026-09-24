package dev.loki.lovisual.render.engine.renderer.ui.runtime.script.render;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiProps;

public record UiScriptRenderContext(
        long frame,
        double time,
        double delta,
        float width,
        float height,
        UiProps props
) {
    public UiScriptRenderContext {
        props = props != null ? props : UiProps.EMPTY;
    }
}
