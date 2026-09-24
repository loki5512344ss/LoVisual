package dev.loki.lovisual.features.gui.clickgui.settings.text;

import dev.loki.lovisual.config.values.primitive.StringValue;
import dev.loki.lovisual.util.text.edit.TextSelection;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.settings.owner.TextEditorOwner;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRendererBridge;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.runtime.UiState;

public class TextSetting extends Setting implements TextEditorOwner {

    private final StringValue value;
    private final UiState ui = new UiState();

    public TextSetting(String name, StringValue value) {
        super(name, value);
        this.value = value;
    }

    private static String normalizeSingleLine(String text) {
        if (text == null) return "";
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        return normalized.replace('\n', ' ');
    }

    UiState ui() {
        return ui;
    }

    @Override
    public void render(float x, float y, float w, float mx, float my) {
        SettingRendererBridge.render(this, x, y, w, mx, my);
    }

    @Override
    public void mouseClicked(double mx, double my, int button) {
        SettingRendererBridge.mouseClicked(this, mx, my, button);
    }

    @Override
    public void mouseClickedOutside(double mx, double my, int button) {
        SettingRendererBridge.mouseClickedOutside(this, mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return SettingRendererBridge.keyPressed(this, keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return SettingRendererBridge.charTyped(this, chr, modifiers);
    }

    @Override
    public float getHeight() {
        return SettingRendererBridge.getHeight(this);
    }

    @Override
    public void applyEditorText(String rawText) {
        String next = normalizeSingleLine(rawText);
        value.set(next);
        if (getParent() != null) getParent().saveConfig();
    }

    @Override
    public String getEditorText() {
        String current = value.get();
        return current == null ? "" : current;
    }

    @Override
    public boolean isSingleLine() {
        return true;
    }

    public static final class UiState {
        public final TextSelection editSelection = new TextSelection();
        public float fieldX, fieldY, fieldW, fieldH;
        public float textX, textY, textSize;
        public float textScroll;
        public float hoverAnim;
        public float focusAnim;
        public float errorAnim;
        public float cursorBlink;
        public boolean editing;
        public String editBuffer = "";
        public int editCursor;
    }
}
