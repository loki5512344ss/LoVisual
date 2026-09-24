package dev.loki.lovisual.api.v0.clickgui;

public interface LoVisualClickGuiSection {
    default void layout(float x, float y, float w, float h) {
    }

    default void render(LoVisualClickGuiRenderContext context, float mouseX, float mouseY) {
    }

    default void renderGlassPass(float alphaFactor) {
    }

    default boolean mousePressed(float mouseX, float mouseY, int button) {
        return false;
    }

    default void mouseReleased(float mouseX, float mouseY, int button) {
    }

    default boolean mouseScrolled(float mouseX, float mouseY, double amount) {
        return false;
    }

    default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    default boolean charTyped(char chr, int modifiers) {
        return false;
    }

    default void onSelected() {
    }

    default void onDeselected() {
    }

    default boolean isVisible() {
        return true;
    }
}
