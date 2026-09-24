
package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.relations;

public final class RelationsComponent {
    private final RelationsState state;
    private final RelationsRenderer renderer;
    private final RelationsInput input;

    public RelationsComponent() {
        RelationsState createdState = new RelationsState();
        RelationsRenderer createdRenderer = new RelationsRenderer();
        RelationsInput createdInput = new RelationsInput();
        createdState.bindStatusSink();
        this.state = createdState;
        this.renderer = createdRenderer;
        this.input = createdInput;
    }

    public void resetScroll() {
        state.resetScroll();
    }

    public boolean isMovementInputBlocked() {
        return state.isMovementInputBlocked();
    }

    public void render(float menuX, float menuY, float menuW, float menuH, float mx, float my, float scale) {
        renderer.render(state, menuX, menuY, menuW, menuH, mx, my, scale);
    }

    public boolean mousePressedScrollbar(float mx, float my, int button) {
        return input.mousePressedScrollbar(state, mx, my, button);
    }

    public void mouseReleased(int button) {
        input.mouseReleased(state, button);
    }

    public void scroll(float mx, float my, double amount) {
        input.scroll(state, mx, my, amount);
    }

    public boolean click(float mx, float my, int button) {
        return input.click(state, mx, my, button);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return input.keyPressed(state, keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        return input.charTyped(state, chr, modifiers);
    }
}
