package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module;

import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRenderSurface;

import java.util.List;

public final class SettingsPanelComponent {
    private final SettingsPanelState state;
    private final SettingsPanelLayout layout;
    private final SettingsPanelChrome chrome;
    private final SettingsPanelRenderer renderer;
    private final SettingsPanelInput input;

    public SettingsPanelComponent() {
        this(SettingRenderSurface.MODULES);
    }

    public SettingsPanelComponent(SettingRenderSurface surface) {
        this.state = new SettingsPanelState(surface);
        this.layout = new SettingsPanelLayout(state);
        this.chrome = new SettingsPanelChrome(state, layout);
        this.renderer = new SettingsPanelRenderer(state, layout, chrome);
        this.input = new SettingsPanelInput(state, layout);
    }

    public void open(String id, String title, List<Setting> source, boolean hudContext) {
        state.open(id, title, source, hudContext);
    }

    public void openEditor(String id, String title, List<Setting> source) {
        state.openEditor(id, title, source);
    }

    public void close() {
        state.close();
    }

    public boolean isActive() {
        return state.isActive();
    }

    public boolean shouldMaskMenu() {
        return state.shouldMaskMenu();
    }

    public boolean blocksMenuInput() {
        return state.blocksMenuInput();
    }

    public void render(float menuX, float menuY, float menuW, float menuH, float mx, float my, float scale) {
        renderer.render(menuX, menuY, menuW, menuH, mx, my, scale);
    }

    public boolean mouseClicked(float mx, float my, int button, float scale) {
        return input.mouseClicked(mx, my, button, scale);
    }

    public boolean mouseReleased(float mx, float my, int button) {
        return input.mouseReleased(mx, my, button);
    }

    public boolean mouseScrolled(float mx, float my, double amount) {
        return input.mouseScrolled(mx, my, amount);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return input.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        return input.charTyped(chr, modifiers);
    }
}
