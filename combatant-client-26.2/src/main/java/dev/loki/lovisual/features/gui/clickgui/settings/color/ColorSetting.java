/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings.color;

import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRendererBridge;
import dev.loki.lovisual.config.values.color.ColorValue;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.util.text.edit.TextSelection;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.runtime.UiState;

public class ColorSetting extends Setting {

    private final ColorValue value;
    private final UiState ui = new UiState();

    public ColorSetting(String name, ColorValue value) {
        super(name, asConfigValue(value));
        this.value = value;
    }

    private static ConfigValue<?> asConfigValue(ColorValue value) {
        return value instanceof ConfigValue<?> configValue ? configValue : null;
    }

    public ColorValue value() {
        return value;
    }

    public UiState ui() {
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
    public boolean mouseScrolled(double mx, double my, double amount) {
        return SettingRendererBridge.mouseScrolled(this, mx, my, amount);
    }

    @Override
    public float getHeight() {
        return SettingRendererBridge.getHeight(this);
    }

    public static final class UiState {
        public final TextSelection hexSelection = new TextSelection();
        public boolean editing = false;
        public boolean showRgbBars = false;
        public boolean rainbow = false;
        public boolean sbFocused = false;
        public boolean hFocused = false;
        public boolean aFocused = false;
        public boolean rFocused = false;
        public boolean gFocused = false;
        public boolean bFocused = false;
        public boolean aRgbFocused = false;
        public float hue = 0f;
        public float saturation = 1f;
        public float brightness = 1f;
        public int alpha = 255;
        public int red = 255;
        public int green = 255;
        public int blue = 255;
        public int lastArgb = 0;
        public float previewGlowAnim = 0f;
        public float expandAnim = 0f;
        public float sliderHoverAnim = 0f;
        public float rnbHoverAnim = 0f;
        public float rnbOffAnim = 0f;
        public float rgbExpandAnim = 0f;
        public float lastX, lastY, lastW;
        public float popupX, popupY, popupW, popupH;
        public float moduleHoverAnim;
        public float squareX, squareY, squareW, squareH;
        public float hueX, hueY, hueW, hueH;
        public float alphaX, alphaY, alphaW, alphaH;
        public float rX, rY, rW, rH;
        public float gX, gY, gW, gH;
        public float bX, bY, bW, bH;
        public float aX, aY, aW, aH;
        public float rgbBottom;
        public float sliderX, sliderY, sliderW, sliderH;
        public float rnbX, rnbY, rnbW, rnbH;
        public boolean hexFocused = false;
        public String hexBuffer = "";
        public int hexCursor = 0;
        public float hexAnim = 0f;
        public float hexErrorAnim = 0f;
        public float hexCursorBlink = 0f;
        public float presetHoverAnim = 0f;
        public float savePresetHoverAnim = 0f;
        public float deletePresetHoverAnim = 0f;
        public boolean presetsExpanded = false;
        public float presetsExpandAnim = 0f;
        public float presetsScroll = 0f;
        public float presetsMaxScroll = 0f;
        public float presetsContentH = 0f;
        public float presetsViewportH = 0f;
        public float presetsToggleHoverAnim = 0f;
        public float presetsScrollbarHoverAnim = 0f;
        public boolean presetsScrollbarDragging = false;
        public float presetsScrollbarDragOffset = 0f;
        public float hexX, hexY, hexW, hexH;
        public float presetX, presetY, presetW, presetH;
        public float presetListX, presetListY, presetListW, presetListH;
        public float presetToggleX, presetToggleY, presetToggleW, presetToggleH;
        public float presetScrollbarX, presetScrollbarY, presetScrollbarW, presetScrollbarH;
        public float presetScrollbarHandleY, presetScrollbarHandleH;
        public float savePresetX, savePresetY, savePresetW, savePresetH;
        public float deletePresetX, deletePresetY, deletePresetW, deletePresetH;
    }
}
