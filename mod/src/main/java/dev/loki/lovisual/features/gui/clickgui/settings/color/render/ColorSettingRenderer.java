package dev.loki.lovisual.features.gui.clickgui.settings.color.render;

import dev.loki.lovisual.config.values.color.ColorValue;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;
import dev.loki.lovisual.features.gui.clickgui.settings.color.ColorSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.color.hex.ColorSettingHexInput;
import dev.loki.lovisual.features.gui.clickgui.settings.color.hex.ColorSettingHexState;
import dev.loki.lovisual.features.gui.clickgui.settings.color.preset.ColorSettingPresetHandler;
import dev.loki.lovisual.features.gui.clickgui.settings.color.preset.ColorSettingPresetRenderer;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.SettingSkinUtil;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.UnifiedSettingsSkin;
import dev.loki.lovisual.features.gui.clickgui.settings.color.render.ColorSettingDrag;
import dev.loki.lovisual.features.gui.clickgui.settings.color.render.ColorSettingLayout;
import dev.loki.lovisual.features.gui.clickgui.settings.color.render.ColorSettingSquareRenderer;

public final class ColorSettingRenderer {
    private ColorSettingRenderer() {
    }

    public static void render(ColorSetting setting, float x, float y, float w, float mx, float my) {
        UnifiedSettingsSkin.syncTheme();
        ColorSetting.UiState ui = setting.ui();
        ColorValue value = setting.value();
        boolean hasAlpha = value.supportsAlpha();
        ui.lastX = x;
        ui.lastY = y;
        ui.lastW = w;
        if (!ui.hexFocused && !ColorSettingDrag.colorDragActive(ui)) ColorSettingDrag.syncColorFromValue(ui, value);
        ui.rainbow = value.isRainbow();
        if (ui.rainbow) ColorSettingDrag.clearColorFocus(ui);

        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        float labelSize = SettingSkinUtil.m(18f, 7f);
        float previewW = SettingSkinUtil.m(34f, 20f);
        float previewH = SettingSkinUtil.m(16f, 10f);
        float previewX = x + w - previewW - SettingSkinUtil.m(5f, 5f);
        float previewY = y + (ColorSettingLayout.colorHeaderH() - previewH) * 0.5f;
        String label = UnifiedSettingsSkin.fit(font, setting.getDisplayName(), labelSize, Math.max(1f, previewX - x - SettingSkinUtil.m(12f, 12f)));
        ClickGuiRenderer.drawText(font, label, x + SettingSkinUtil.m(7f, 7f), y + (ColorSettingLayout.colorHeaderH() - UnifiedSettingsSkin.textHeight(font, labelSize)) * 0.5f, labelSize, UnifiedSettingsSkin.TEXT_PRIMARY, false);
        renderColorPreview(previewX, previewY, previewW, previewH, value.getArgb(), SettingSkinUtil.m(3f, 3f));
        ui.previewGlowAnim = AnimationUtility.approach(ui.previewGlowAnim, ui.editing ? 1f : UnifiedSettingsSkin.inside(mx, my, previewX, previewY, previewW, previewH) ? 0.55f : 0f, 0.20f);

        if (ui.expandAnim <= 0.001f) return;
        ColorSettingLayout.layoutColor(ui, x, y, w, hasAlpha);
        ColorSettingDrag.updateColorDrag(ui, value, mx, my, hasAlpha);
        float reveal = ColorSettingLayout.colorReveal(ui);
        float slide = ui.editing ? (1f - reveal) * SettingSkinUtil.m(8f, 8f) : (1f - reveal) * SettingSkinUtil.m(6f, 6f);
        float clipY = y + ColorSettingLayout.colorHeaderH();
        float clipH = (ColorSettingLayout.colorExpandedHeight(ui, hasAlpha) + SettingSkinUtil.m(5f, 5f)) * reveal;
        boolean clipped = clipH > 0.5f && ScissorFunction.pushRaw(x, clipY, w, clipH);

        float cx = ui.squareX;
        float cy = ui.squareY + slide;
        float containerX = cx - SettingSkinUtil.m(5f, 5f);
        float containerY = cy - SettingSkinUtil.m(5f, 5f);
        float containerW = x + w - SettingSkinUtil.m(5f, 5f) - containerX;
        float containerH = ColorSettingLayout.colorExpandedHeight(ui, hasAlpha);
        UnifiedSettingsSkin.drawSurface(containerX, containerY, containerW, containerH, SettingSkinUtil.m(6f, 6f), 0.90f);

        ColorSettingSquareRenderer.renderColorSquare(ui, cy);
        ColorSettingSquareRenderer.renderHueBar(ui, slide);
        if (hasAlpha) {
            SettingSkinUtil.renderChecker(ui.alphaX, ui.alphaY + slide, ui.alphaW, ui.alphaH, SettingSkinUtil.m(3f, 3f), SettingSkinUtil.m(3f, 3f));
            ColorSettingSquareRenderer.renderAlphaBar(ui, slide);
        }
        if (ui.rgbExpandAnim > 0.001f) ColorSettingSquareRenderer.renderRgbBars(ui, hasAlpha, slide);
        ColorSettingSquareRenderer.renderColorMarkers(ui, hasAlpha, slide);
        ColorSettingPresetRenderer.renderColorPresets(ui, value, mx, my, slide);
        ColorSettingHexInput.renderHexInput(ui, value, mx, my, slide);
        ColorSettingPresetRenderer.renderColorActions(ui, value, mx, my, slide);
        if (clipped) ScissorFunction.pop();
    }

    public static void mouseClicked(ColorSetting setting, double mx, double my, int button) {
        if (button != 0 && button != 1 && button != 2) return;
        ColorSetting.UiState ui = setting.ui();
        ColorValue value = setting.value();
        boolean hasAlpha = value.supportsAlpha();
        float pw = SettingSkinUtil.m(34f, 20f);
        float ph = SettingSkinUtil.m(16f, 10f);
        float px = ui.lastX + ui.lastW - pw - SettingSkinUtil.m(5f, 5f);
        float py = ui.lastY + (ColorSettingLayout.colorHeaderH() - ph) * 0.5f;
        boolean headerHit = UnifiedSettingsSkin.inside(mx, my, ui.lastX, ui.lastY, ui.lastW, ColorSettingLayout.colorHeaderH());
        if (button == 0 && headerHit) {
            ui.editing = !ui.editing;
            ColorSettingDrag.clearColorFocus(ui);
            return;
        }
        if (!ui.editing) return;
        ColorSettingLayout.layoutColor(ui, ui.lastX, ui.lastY, ui.lastW, hasAlpha);
        if (ColorSettingPresetHandler.handleColorPresetClick(setting, value, ui, mx, my, button)) return;
        if (button != 0) return;
        if (UnifiedSettingsSkin.inside(mx, my, ui.hexX, ui.hexY, ui.hexW, ui.hexH)) {
            ColorSettingHexInput.beginHexEdit(ui, value);
            ColorSettingHexState.setHexCursorFromMouse(ui, (float) mx, false);
            return;
        }
        if (ui.hexFocused) {
            if (!ColorSettingHexInput.commitHexEdit(setting, value, ui, true)) return;
        }
        if (UnifiedSettingsSkin.inside(mx, my, ui.sliderX, ui.sliderY, ui.sliderW, ui.sliderH)) {
            ui.showRgbBars = !ui.showRgbBars;
            ColorSettingDrag.clearColorFocus(ui);
            return;
        }
        if (UnifiedSettingsSkin.inside(mx, my, ui.rnbX, ui.rnbY, ui.rnbW, ui.rnbH)) {
            value.set(value.isRainbow() ? value.rainbowFallbackHex() : value.toRainbowValue());
            ColorSettingDrag.syncColorFromValue(ui, value);
            ColorSettingDrag.clearColorFocus(ui);
            if (setting.getParent() != null) setting.getParent().saveConfig();
            return;
        }
        if (value.isRainbow()) return;
        ColorSettingDrag.clearColorFocus(ui);
        if (UnifiedSettingsSkin.inside(mx, my, ui.squareX, ui.squareY, ui.squareW, ui.squareH)) {
            ui.sbFocused = true;
            return;
        }
        if (UnifiedSettingsSkin.inside(mx, my, ui.hueX - SettingSkinUtil.m(2f, 2f), ui.hueY, ui.hueW + SettingSkinUtil.m(4f, 4f), ui.hueH)) {
            ui.hFocused = true;
            return;
        }
        if (hasAlpha && UnifiedSettingsSkin.inside(mx, my, ui.alphaX - SettingSkinUtil.m(2f, 2f), ui.alphaY, ui.alphaW + SettingSkinUtil.m(4f, 4f), ui.alphaH)) {
            ui.aFocused = true;
            return;
        }
        if (ui.showRgbBars && ui.rgbExpandAnim > 0.20f) {
            if (UnifiedSettingsSkin.inside(mx, my, ui.rX, ui.rY, ui.rW, ui.rH)) {
                ui.rFocused = true;
                return;
            }
            if (UnifiedSettingsSkin.inside(mx, my, ui.gX, ui.gY, ui.gW, ui.gH)) {
                ui.gFocused = true;
                return;
            }
            if (UnifiedSettingsSkin.inside(mx, my, ui.bX, ui.bY, ui.bW, ui.bH)) {
                ui.bFocused = true;
                return;
            }
            if (hasAlpha && UnifiedSettingsSkin.inside(mx, my, ui.aX, ui.aY, ui.aW, ui.aH)) ui.aRgbFocused = true;
        }
    }

    public static void mouseReleased(ColorSetting setting, double mx, double my, int button) {
        if (button != 0) return;
        ColorSetting.UiState ui = setting.ui();
        boolean changed = ui.sbFocused || ui.hFocused || ui.aFocused || ui.rFocused || ui.gFocused || ui.bFocused || ui.aRgbFocused;
        ui.presetsScrollbarDragging = false;
        ColorSettingDrag.clearColorFocus(ui);
        if (changed && setting.getParent() != null) setting.getParent().saveConfig();
    }

    public static void mouseClickedOutside(ColorSetting setting, double mx, double my, int button) {
        if (button != 0) return;
        ColorSetting.UiState ui = setting.ui();
        if (ui.hexFocused) {
            ColorSettingHexInput.commitHexEdit(setting, setting.value(), ui, true);
        }
        ui.presetsScrollbarDragging = false;
        ColorSettingDrag.clearColorFocus(ui);
    }

    public static boolean mouseScrolled(ColorSetting setting, double mx, double my, double amount) {
        ColorSetting.UiState ui = setting.ui();
        if (!ui.editing || !ui.presetsExpanded || ui.presetsMaxScroll <= 0.5f) return false;
        float slide = (1f - ColorSettingLayout.colorReveal(ui)) * SettingSkinUtil.m(8f, 8f);
        float listY = ui.presetListY + slide;
        if (!UnifiedSettingsSkin.inside(mx, my, ui.presetListX, listY, ui.presetListW, Math.max(1f, ui.presetListH)))
            return false;
        ui.presetsScroll = AnimationUtility.clamp(ui.presetsScroll - (float) amount * SettingSkinUtil.m(16f, 12f), 0f, ui.presetsMaxScroll);
        return true;
    }

    public static float getHeight(ColorSetting setting) {
        ColorSetting.UiState ui = setting.ui();
        boolean hasAlpha = setting.value().supportsAlpha();
        float dt = AnimationUtility.deltaTime();
        ui.expandAnim = AnimationUtility.approach(ui.expandAnim, ui.editing ? 1f : 0f, dt, ui.editing ? 5.2f : 4.0f);
        ui.expandAnim = AnimationUtility.snap(ui.expandAnim, ui.editing ? 1f : 0f, 0.001f);
        ui.rgbExpandAnim = AnimationUtility.approach(ui.rgbExpandAnim, ui.showRgbBars ? 1f : 0f, dt, ui.showRgbBars ? 10f : 9f);
        ui.rgbExpandAnim = AnimationUtility.snap(ui.rgbExpandAnim, ui.showRgbBars ? 1f : 0f, 0.002f);
        ui.presetsExpandAnim = AnimationUtility.approach(ui.presetsExpandAnim, ui.presetsExpanded ? 1f : 0f, dt, ui.presetsExpanded ? 9.5f : 8.5f);
        ui.presetsExpandAnim = AnimationUtility.snap(ui.presetsExpandAnim, ui.presetsExpanded ? 1f : 0f, 0.002f);
        return ColorSettingLayout.colorHeaderH() + ColorSettingLayout.colorExpandedHeight(ui, hasAlpha) * ColorSettingLayout.colorLayoutAnim(ui);
    }

    private static void renderColorPreview(float x, float y, float w, float h, int color, float radius) {
        ClickGuiRenderer.drawRoundedRect(x, y, w, h, radius, color);
    }
}
