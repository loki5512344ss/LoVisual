/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings.slider;

import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.util.text.edit.TextSelection;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRendererBridge;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.runtime.UiState;

public class SliderSetting<N extends Number> extends Setting {

    private final NumberValue<N> value;
    private final UiState ui = new UiState();

    public SliderSetting(String name, NumberValue<N> value) {
        super(name, value);
        this.value = value;
    }

    public NumberValue<N> value() {
        return value;
    }

    public UiState ui() {
        return ui;
    }

    // ==================== CONFIG ==================== //

    @Override
    public Object save() {
        return value == null ? null : value.toJson();
    }

    @Override
    public void load(Object json) {
        if (value != null) value.fromJson(json);
    }

    // ========================= UI ========================= //

    @Override
    public void render(float x, float y, float w, float mx, float my) {
        SettingRendererBridge.render(this, x, y, w, mx, my);
    }

    @Override
    public void mouseClicked(double mx, double my, int button) {
        SettingRendererBridge.mouseClicked(this, mx, my, button);
    }

    @Override
    public void mouseReleased(double mx, double my, int button) {
        SettingRendererBridge.mouseReleased(this, mx, my, button);
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

    public static final class UiState {
        public final TextSelection editSelection = new TextSelection();
        public boolean dragging;
        public float lastX, lastY, lastW;
        public float anim = 0f;
        public float hoverAnim = 0f;
        public float valueHoverAnim = 0f;
        public float editAnim = 0f;
        public float errorAnim = 0f;
        public float cursorBlink = 0f;
        public boolean editing = false;
        public String editBuffer = "";
        public int editCursor = 0;
        public float inputHoverAnim = 0f;
        public float inputFocusGlowAnim = 0f;
        public float inputPressAnim = 0f;
        public float valueX, valueY, valueW, valueH;
        public float valueTextX, valueTextY, valueTextSize, valueTextPad;
    }
}
