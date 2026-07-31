/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings;

enum UnifiedSettingRenderer {
    ;

    // ------------------------------------------------------------
    // Boolean
    // ------------------------------------------------------------

    static void render(BooleanSetting setting, float x, float y, float w, float mx, float my) {
        BooleanSettingRenderer.render(setting, x, y, w, mx, my);
    }

    static void mouseClicked(BooleanSetting setting, double mx, double my, int button) {
        BooleanSettingRenderer.mouseClicked(setting, mx, my, button);
    }

    static float getHeight(BooleanSetting setting) {
        return BooleanSettingRenderer.getHeight(setting);
    }

    // ------------------------------------------------------------
    // Slider
    // ------------------------------------------------------------

    static <N extends Number> void render(SliderSetting<N> setting, float x, float y, float w, float mx, float my) {
        SliderSettingRenderer.render(setting, x, y, w, mx, my);
    }

    static <N extends Number> void mouseClicked(SliderSetting<N> setting, double mx, double my, int button) {
        SliderSettingRenderer.mouseClicked(setting, mx, my, button);
    }

    static <N extends Number> void mouseReleased(SliderSetting<N> setting, double mx, double my, int button) {
        SliderSettingRenderer.mouseReleased(setting, mx, my, button);
    }

    static <N extends Number> void mouseClickedOutside(SliderSetting<N> setting, double mx, double my, int button) {
        SliderSettingRenderer.mouseClickedOutside(setting, mx, my, button);
    }

    static <N extends Number> boolean keyPressed(SliderSetting<N> setting, int keyCode, int scanCode, int modifiers) {
        return SliderTextInputHandler.keyPressed(setting, keyCode, scanCode, modifiers);
    }

    static <N extends Number> boolean charTyped(SliderSetting<N> setting, char chr, int modifiers) {
        return SliderTextInputHandler.charTyped(setting, chr, modifiers);
    }

    static <N extends Number> float getHeight(SliderSetting<N> setting) {
        return SliderSettingRenderer.getHeight(setting);
    }

    // ------------------------------------------------------------
    // Color
    // ------------------------------------------------------------

    static void render(ColorSetting setting, float x, float y, float w, float mx, float my) {
        ColorSettingRenderer.render(setting, x, y, w, mx, my);
    }

    static void mouseClicked(ColorSetting setting, double mx, double my, int button) {
        ColorSettingRenderer.mouseClicked(setting, mx, my, button);
    }

    static void mouseReleased(ColorSetting setting, double mx, double my, int button) {
        ColorSettingRenderer.mouseReleased(setting, mx, my, button);
    }

    static void mouseClickedOutside(ColorSetting setting, double mx, double my, int button) {
        ColorSettingRenderer.mouseClickedOutside(setting, mx, my, button);
    }

    static boolean keyPressed(ColorSetting setting, int keyCode, int scanCode, int modifiers) {
        return ColorSettingHexInput.keyPressed(setting, keyCode, scanCode, modifiers);
    }

    static boolean charTyped(ColorSetting setting, char chr, int modifiers) {
        return ColorSettingHexInput.charTyped(setting, chr, modifiers);
    }

    static boolean mouseScrolled(ColorSetting setting, double mx, double my, double amount) {
        return ColorSettingRenderer.mouseScrolled(setting, mx, my, amount);
    }

    static float getHeight(ColorSetting setting) {
        return ColorSettingRenderer.getHeight(setting);
    }

    // ------------------------------------------------------------
    // Text
    // ------------------------------------------------------------

    static void render(TextSetting setting, float x, float y, float w, float mx, float my) {
        TextSettingRenderer.render(setting, x, y, w, mx, my);
    }

    static void mouseClicked(TextSetting setting, double mx, double my, int button) {
        TextSettingRenderer.mouseClicked(setting, mx, my, button);
    }

    static void mouseClickedOutside(TextSetting setting, double mx, double my, int button) {
        TextSettingRenderer.mouseClickedOutside(setting, mx, my, button);
    }

    static boolean keyPressed(TextSetting setting, int keyCode, int scanCode, int modifiers) {
        return TextSettingTextInputHandler.keyPressed(setting, keyCode, scanCode, modifiers);
    }

    static boolean charTyped(TextSetting setting, char chr, int modifiers) {
        return TextSettingTextInputHandler.charTyped(setting, chr, modifiers);
    }

    static float getHeight(TextSetting setting) {
        return TextSettingRenderer.getHeight(setting);
    }

    // ------------------------------------------------------------
    // TextList
    // ------------------------------------------------------------

    static void render(TextListSetting setting, float x, float y, float w, float mx, float my) {
        TextListSettingRenderer.render(setting, x, y, w, mx, my);
    }

    static void mouseClicked(TextListSetting setting, double mx, double my, int button) {
        TextListSettingRenderer.mouseClicked(setting, mx, my, button);
    }

    static void mouseClickedOutside(TextListSetting setting, double mx, double my, int button) {
        TextListSettingRenderer.mouseClickedOutside(setting, mx, my, button);
    }

    static boolean keyPressed(TextListSetting setting, int keyCode, int scanCode, int modifiers) {
        return TextListTextEditHandler.keyPressed(setting, keyCode, scanCode, modifiers);
    }

    static boolean charTyped(TextListSetting setting, char chr, int modifiers) {
        return TextListTextEditHandler.charTyped(setting, chr, modifiers);
    }

    static float getHeight(TextListSetting setting) {
        return TextListSettingRenderer.getHeight(setting);
    }

    // ------------------------------------------------------------
    // Group / Mode chips
    // ------------------------------------------------------------

    static void render(GroupSetting setting, float x, float y, float w, float mx, float my) {
        GroupSettingRenderer.render(setting, x, y, w, mx, my);
    }

    static void mouseClicked(GroupSetting setting, double mx, double my, int button) {
        GroupSettingRenderer.mouseClicked(setting, mx, my, button);
    }

    static float getHeight(GroupSetting setting) {
        return GroupSettingRenderer.getHeight(setting);
    }

    static void render(ModeSetting setting, float x, float y, float w, float mx, float my) {
        ModeSettingRenderer.render(setting, x, y, w, mx, my);
    }

    static void mouseClicked(ModeSetting setting, double mx, double my, int button) {
        ModeSettingRenderer.mouseClicked(setting, mx, my, button);
    }

    static float getHeight(ModeSetting setting) {
        return ModeSettingRenderer.getHeight(setting);
    }

    // ------------------------------------------------------------
    // Key binds
    // ------------------------------------------------------------

    static void render(KeyBindSetting setting, float x, float y, float w, float mx, float my) {
        KeyBindSettingRenderer.render(setting, x, y, w, mx, my);
    }

    static void mouseClicked(KeyBindSetting setting, double mx, double my, int button) {
        KeyBindSettingRenderer.mouseClicked(setting, mx, my, button);
    }

    static float getHeight(KeyBindSetting setting) {
        return KeyBindSettingRenderer.getHeight(setting);
    }

    static void render(FunctionBindSetting setting, float x, float y, float w, float mx, float my) {
        FunctionBindSettingRenderer.render(setting, x, y, w, mx, my);
    }

    static void mouseClicked(FunctionBindSetting setting, double mx, double my, int button) {
        FunctionBindSettingRenderer.mouseClicked(setting, mx, my, button);
    }

    static float getHeight(FunctionBindSetting setting) {
        return FunctionBindSettingRenderer.getHeight(setting);
    }
}
