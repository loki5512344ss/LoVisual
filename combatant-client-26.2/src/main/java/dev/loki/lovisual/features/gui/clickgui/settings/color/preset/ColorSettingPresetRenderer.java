/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings.color.preset;

import dev.loki.lovisual.config.values.color.ColorValue;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;
import dev.loki.lovisual.features.gui.clickgui.settings.color.ColorSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.color.render.ColorSettingLayout;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.SettingSkinUtil;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.UnifiedSettingsSkin;
import dev.loki.lovisual.features.gui.clickgui.settings.color.preset.ColorPresetStore;
import dev.loki.lovisual.features.gui.clickgui.settings.color.preset.ColorSettingPresetHandler;

public final class ColorSettingPresetRenderer {
    private ColorSettingPresetRenderer() {
    }

    public static void renderColorPresets(ColorSetting.UiState ui, ColorValue value, float mx, float my, float slide) {
        float y = ui.presetY + slide;
        float swatch = SettingSkinUtil.m(12f, 8f);
        float gap = SettingSkinUtil.m(4f, 3f);
        int labelColor = UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_MUTED, 190);
        Renderer2D.COLOR.svg("palette", ui.presetX, y + (ui.presetH - swatch) * 0.5f, swatch, swatch,
                SvgRenderOptions.overrideColor(labelColor));
        float sx = ui.presetX + swatch + gap;
        float available = Math.max(1f, ui.presetToggleX - gap - sx);
        java.util.List<Integer> presets = ColorPresetStore.presets();
        int max = Math.max(1, (int) Math.floor(available / (swatch + gap)));
        for (int i = 0; i < Math.min(max, presets.size()); i++) {
            float px = sx + i * (swatch + gap);
            int color = presets.get(i);
            boolean hover = UnifiedSettingsSkin.inside(mx, my, px, y + (ui.presetH - swatch) * 0.5f, swatch, swatch);
            renderPresetSwatch(px, y + (ui.presetH - swatch) * 0.5f, swatch, presetDisplayColor(value, color), hover, value.supportsAlpha());
        }

        float dt = AnimationUtility.deltaTime();
        ui.presetsToggleHoverAnim = AnimationUtility.approach(ui.presetsToggleHoverAnim, UnifiedSettingsSkin.inside(mx, my, ui.presetToggleX, y, ui.presetToggleW, ui.presetToggleH) ? 1f : 0f, dt, 14f);
        ui.savePresetHoverAnim = AnimationUtility.approach(ui.savePresetHoverAnim, UnifiedSettingsSkin.inside(mx, my, ui.savePresetX, y, ui.savePresetW, ui.savePresetH) ? 1f : 0f, dt, 14f);
        ui.deletePresetHoverAnim = AnimationUtility.approach(ui.deletePresetHoverAnim, UnifiedSettingsSkin.inside(mx, my, ui.deletePresetX, y, ui.deletePresetW, ui.deletePresetH) ? 1f : 0f, dt, 14f);
        renderColorPresetActionGroup(ui, y);

        float reveal = ColorSettingLayout.colorPresetReveal(ui);
        if (reveal <= 0.035f) {
            ui.presetsMaxScroll = 0f;
            ui.presetsContentH = 0f;
            ui.presetsViewportH = 0f;
            ui.presetListH = 0f;
            ui.presetScrollbarX = ui.presetScrollbarY = ui.presetScrollbarW = ui.presetScrollbarH = 0f;
            return;
        }

        float listY = ui.presetListY + slide;
        float listH = ui.presetListH;
        if (listH <= SettingSkinUtil.m(2f, 1.2f)) {
            ui.presetsMaxScroll = 0f;
            return;
        }
        float pad = SettingSkinUtil.m(5f, 4f);
        UnifiedSettingsSkin.drawSurface(ui.presetListX, listY, ui.presetListW, listH, SettingSkinUtil.m(6f, 4f), 0.68f * reveal);
        if (UnifiedSettingsSkin.STROKE_GRADIENT_ENABLED) {
            UnifiedSettingsSkin.drawStroke(ui.presetListX, listY, ui.presetListW, listH, SettingSkinUtil.m(6f, 4f), SettingSkinUtil.m(0.65f, 0.42f), 0.32f * reveal);
        } else {
            ClickGuiRenderer.drawRoundedRectStroke(ui.presetListX, listY, ui.presetListW, listH, SettingSkinUtil.m(6f, 4f), SettingSkinUtil.m(0.65f, 0.42f), UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_FAINT, Math.round(58f * reveal)));
        }

        int cols = Math.max(1, (int) Math.floor((ui.presetListW - pad * 2f - SettingSkinUtil.m(7f, 5f)) / (swatch + gap)));
        int rows = (int) Math.ceil(presets.size() / (double) cols);
        float rowH = swatch + gap;
        ui.presetsContentH = Math.max(0f, rows * rowH - gap);
        ui.presetsViewportH = Math.max(1f, listH - pad * 2f);
        ui.presetsMaxScroll = Math.max(0f, ui.presetsContentH - ui.presetsViewportH);
        ui.presetsScroll = AnimationUtility.clamp(ui.presetsScroll, 0f, ui.presetsMaxScroll);

        boolean clip = ScissorFunction.pushRaw(ui.presetListX + pad, listY + pad, Math.max(1f, ui.presetListW - pad * 2f - (ui.presetsMaxScroll > 0.5f ? SettingSkinUtil.m(5f, 4f) : 0f)), ui.presetsViewportH);
        for (int i = 0; i < presets.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            float px = ui.presetListX + pad + col * (swatch + gap);
            float py = listY + pad + row * rowH - ui.presetsScroll;
            if (py + swatch < listY + pad - 1f || py > listY + pad + ui.presetsViewportH + 1f) continue;
            boolean hover = UnifiedSettingsSkin.inside(mx, my, px, py, swatch, swatch);
            renderPresetSwatch(px, py, swatch, presetDisplayColor(value, presets.get(i)), hover, value.supportsAlpha());
        }
        if (clip) ScissorFunction.pop();

        if (ui.presetsMaxScroll > 0.5f) {
            ui.presetScrollbarW = SettingSkinUtil.m(2.5f, 1.7f);
            ui.presetScrollbarX = ui.presetListX + ui.presetListW - pad - ui.presetScrollbarW;
            ui.presetScrollbarY = listY + pad;
            ui.presetScrollbarH = ui.presetsViewportH;
            ui.presetScrollbarHandleH = Math.max(SettingSkinUtil.m(12f, 8f), ui.presetScrollbarH * (ui.presetScrollbarH / (ui.presetScrollbarH + ui.presetsMaxScroll)));
            ui.presetScrollbarHandleY = ui.presetScrollbarY + (ui.presetScrollbarH - ui.presetScrollbarHandleH) * (ui.presetsScroll / Math.max(1f, ui.presetsMaxScroll));
            boolean barHover = UnifiedSettingsSkin.inside(mx, my, ui.presetScrollbarX - SettingSkinUtil.m(3f, 2f), ui.presetScrollbarY, ui.presetScrollbarW + SettingSkinUtil.m(6f, 4f), ui.presetScrollbarH) || ui.presetsScrollbarDragging;
            ui.presetsScrollbarHoverAnim = AnimationUtility.approach(ui.presetsScrollbarHoverAnim, barHover ? 1f : 0f, dt, 14f);
            ClickGuiRenderer.drawRoundedRect(ui.presetScrollbarX, ui.presetScrollbarY, ui.presetScrollbarW, ui.presetScrollbarH, ui.presetScrollbarW * 0.5f, UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_FAINT, 42));
            ClickGuiRenderer.drawRoundedRect(ui.presetScrollbarX, ui.presetScrollbarHandleY, ui.presetScrollbarW, ui.presetScrollbarHandleH, ui.presetScrollbarW * 0.5f, UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_MUTED, 95 + Math.round(65f * ui.presetsScrollbarHoverAnim)));
            if (ui.presetsScrollbarDragging) {
                ColorSettingPresetHandler.updatePresetScrollbarDrag(ui, my);
            }
        } else {
            ui.presetScrollbarX = ui.presetScrollbarY = ui.presetScrollbarW = ui.presetScrollbarH = 0f;
        }
    }

    private static void renderPresetSwatch(float x, float y, float size, int color, boolean hover, boolean alphaContext) {
        int stroke = UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_FAINT, hover ? 130 : 78);
        if (alphaContext || ((color >>> 24) & 0xFF) < 255) {
            SettingSkinUtil.renderChecker(x, y, size, size, SettingSkinUtil.m(3f, 2f), SettingSkinUtil.m(3f, 2f));
        }
        ClickGuiRenderer.drawRoundedRect(x, y, size, size, SettingSkinUtil.m(3f, 2f), color);
        ClickGuiRenderer.drawRoundedRectStroke(x, y, size, size, SettingSkinUtil.m(3f, 2f), SettingSkinUtil.m(0.7f, 0.45f), stroke);
    }

    private static int presetDisplayColor(ColorValue value, int argb) {
        return value.supportsAlpha() ? argb : (0xFF000000 | (argb & 0x00FFFFFF));
    }

    private static void renderColorPresetActionGroup(ColorSetting.UiState ui, float y) {
        float x = ui.presetToggleX;
        float w = ui.presetToggleW + ui.savePresetW + ui.deletePresetW;
        float h = ui.presetToggleH;
        float hover = Math.max(ui.presetsToggleHoverAnim, Math.max(ui.savePresetHoverAnim, ui.deletePresetHoverAnim));
        float active = ui.presetsExpanded ? 1f : 0f;
        if (ui.presetsExpanded) {
            UnifiedSettingsSkin.drawAccent(x, y, w, h, SettingSkinUtil.m(5f, 4f), 0.32f + hover * 0.16f);
        } else {
            UnifiedSettingsSkin.drawSurface(x, y, w, h, SettingSkinUtil.m(5f, 4f), 0.58f + hover * 0.24f);
        }
        int stroke = UnifiedSettingsSkin.mix(UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_FAINT, 62), UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.ACCENT, 150), active);
        stroke = UnifiedSettingsSkin.mix(stroke, UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_MUTED, 120), hover * 0.35f);
        if (UnifiedSettingsSkin.STROKE_GRADIENT_ENABLED || ui.presetsExpanded) {
            ClickGuiRenderer.drawRoundedRectStrokeGradient(x, y, w, h, SettingSkinUtil.m(5f, 4f), SettingSkinUtil.m(0.7f, 0.45f), UnifiedSettingsSkin.accentGradientStart(0.58f + active * 0.20f), UnifiedSettingsSkin.accentGradientEnd(0.58f + active * 0.20f), UnifiedSettingsSkin.ACCENT_GRADIENT_ANGLE);
        } else {
            ClickGuiRenderer.drawRoundedRectStroke(x, y, w, h, SettingSkinUtil.m(5f, 4f), SettingSkinUtil.m(0.7f, 0.45f), stroke);
        }

        float sepW = SettingSkinUtil.m(0.7f, 0.45f);
        int sep = UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_FAINT, 55 + Math.round(35f * hover));
        ClickGuiRenderer.drawRect(ui.savePresetX, y + SettingSkinUtil.m(3f, 2f), sepW, h - SettingSkinUtil.m(6f, 4f), sep);
        ClickGuiRenderer.drawRect(ui.deletePresetX, y + SettingSkinUtil.m(3f, 2f), sepW, h - SettingSkinUtil.m(6f, 4f), sep);

        renderPresetActionIcon("folder-closed", ui.presetToggleX, y, ui.presetToggleW, h, ui.presetsToggleHoverAnim, ui.presetsExpanded);
        renderPresetActionIcon("heart-plus", ui.savePresetX, y, ui.savePresetW, h, ui.savePresetHoverAnim, false);
        renderPresetActionIcon("minus", ui.deletePresetX, y, ui.deletePresetW, h, ui.deletePresetHoverAnim, false);
    }

    private static void renderPresetActionIcon(String icon, float x, float y, float w, float h, float hoverAnim, boolean active) {
        float size = Math.min(SettingSkinUtil.m(11f, 7f), Math.max(SettingSkinUtil.m(8f, 5f), Math.min(w, h) - SettingSkinUtil.m(5f, 3f)));
        int color = active
                ? UnifiedSettingsSkin.ACCENT
                : UnifiedSettingsSkin.mix(UnifiedSettingsSkin.TEXT_MUTED, UnifiedSettingsSkin.TEXT_PRIMARY, 0.55f + hoverAnim * 0.35f);
        Renderer2D.COLOR.svg(icon, x + (w - size) * 0.5f, y + (h - size) * 0.5f, size, size,
                SvgRenderOptions.overrideColor(color));
    }

    public static void renderColorActions(ColorSetting.UiState ui, ColorValue value, float mx, float my, float slide) {
        float dt = AnimationUtility.deltaTime();
        boolean rgbHover = UnifiedSettingsSkin.inside(mx, my, ui.sliderX, ui.sliderY + slide, ui.sliderW, ui.sliderH);
        boolean rnbHover = UnifiedSettingsSkin.inside(mx, my, ui.rnbX, ui.rnbY + slide, ui.rnbW, ui.rnbH);
        ui.sliderHoverAnim = AnimationUtility.approach(ui.sliderHoverAnim, rgbHover ? 1f : 0f, dt, 14f);
        ui.rnbHoverAnim = AnimationUtility.approach(ui.rnbHoverAnim, rnbHover ? 1f : 0f, dt, 14f);
        ui.rnbOffAnim = AnimationUtility.approach(ui.rnbOffAnim, value.isRainbow() ? 0f : 1f, dt, 8f);
        SettingSkinUtil.renderChipButton(ui.sliderX, ui.sliderY + slide, ui.sliderW, ui.sliderH, "RGB", ui.sliderHoverAnim, ui.showRgbBars);
        SettingSkinUtil.renderRainbowChipButton(ui.rnbX, ui.rnbY + slide, ui.rnbW, ui.rnbH, ui.rnbHoverAnim, value.isRainbow());
    }
}
