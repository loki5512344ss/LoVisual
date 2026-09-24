package dev.loki.lovisual.render.engine.renderer.ui.runtime.script.patch;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNodeSpec;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.render.UiScriptRenderContext;

public interface UiScriptComponent {
    UiNodeSpec render(UiScriptRenderContext context);

    default void dispose() {
    }
}
