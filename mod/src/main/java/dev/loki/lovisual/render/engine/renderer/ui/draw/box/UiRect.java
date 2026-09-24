package dev.loki.lovisual.render.engine.renderer.ui.draw.box;

public record UiRect(float x, float y, float width, float height) {
    public static UiRect of(double x, double y, double width, double height) {
        return new UiRect((float) x, (float) y, (float) width, (float) height);
    }

    public boolean empty() {
        return width == 0.0f || height == 0.0f;
    }
}
