package dev.loki.lovisual.api.v0.render.callback;

@FunctionalInterface
public interface LoVisualRenderCallback {
    public void render(LoVisualRenderContext context);
}
