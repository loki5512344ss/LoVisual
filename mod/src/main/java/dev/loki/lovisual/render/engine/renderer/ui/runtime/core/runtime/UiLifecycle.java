package dev.loki.lovisual.render.engine.renderer.ui.runtime.core.runtime;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNode;


public interface UiLifecycle {
    default void onMount(UiNode node) {
    }

    default void onUpdate(UiNode node) {
    }

    default void onUnmount(UiNode node) {
    }
}
