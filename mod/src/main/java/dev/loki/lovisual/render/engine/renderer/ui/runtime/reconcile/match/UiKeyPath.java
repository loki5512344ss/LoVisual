package dev.loki.lovisual.render.engine.renderer.ui.runtime.reconcile.match;

public record UiKeyPath(String value) {
    public UiKeyPath child(String keyOrIndex) {
        String child = keyOrIndex == null || keyOrIndex.isBlank() ? "?" : keyOrIndex;
        return new UiKeyPath(value == null || value.isBlank() ? child : value + "/" + child);
    }
}
