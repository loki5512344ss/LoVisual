package dev.loki.lovisual.render.engine.renderer.ui.runtime.asset;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.asset.registry.UiAssetKind;


public record UiAssetRef(UiAssetKind kind, String id, float intrinsicWidth, float intrinsicHeight) {
    public static UiAssetRef texture(String id, float width, float height) {
        return new UiAssetRef(UiAssetKind.TEXTURE, id, width, height);
    }

    public String getId() {
        return id;
    }
}
