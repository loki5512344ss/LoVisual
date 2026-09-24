package dev.loki.lovisual.render.engine.renderer.ui.runtime.action;

@FunctionalInterface
public interface UiAction {
    boolean run(UiActionContext context);
}
