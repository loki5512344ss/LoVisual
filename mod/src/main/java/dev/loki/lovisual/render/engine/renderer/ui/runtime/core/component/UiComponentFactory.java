package dev.loki.lovisual.render.engine.renderer.ui.runtime.core.component;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNodeSpec;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiProps;


@FunctionalInterface
public interface UiComponentFactory {
    UiNodeSpec create(UiProps props);
}
