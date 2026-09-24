package dev.loki.lovisual.render.engine.renderer.ui.runtime.asset;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiProps;

@FunctionalInterface
public interface UiAssetProvider {
    UiAssetRef resolve(UiProps props, String type, String id, float intrinsicWidth, float intrinsicHeight);
}
