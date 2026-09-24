package dev.loki.lovisual.api.v0.render.callback;

@FunctionalInterface
public interface LoVisualPostProcessCallback {
    public boolean render(LoVisualPostProcessContext context);
}
