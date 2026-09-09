/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings.bool;

import dev.loki.lovisual.features.gui.clickgui.ClickGuiRenderer;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.util.wav.ClickGuiSounds;
import dev.loki.lovisual.features.gui.clickgui.settings.bool.BooleanSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.SettingSkinUtil;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.UnifiedSettingsSkin;

public final class BooleanSettingRenderer {
    private BooleanSettingRenderer() {
    }

    public record LayoutBoolean(String[] lines, float lineH, float labelY, float controlY, float rowH) {
    }

    public static void render(BooleanSetting setting, float x, float y, float w, float mx, float my) {
        UnifiedSettingsSkin.syncTheme();
        BooleanSetting.UiState ui = setting.ui();
        LayoutBoolean layout = booleanLayout(setting, w);
        ui.lastX = x;
        ui.lastY = y;
        ui.lastW = w;
        ui.lastH = layout.rowH;

        boolean enabled = setting.get();
        float dt = AnimationUtility.deltaTime();
        ui.anim = AnimationUtility.approach(ui.anim, enabled ? 1f : 0f, dt, 13f);
        boolean hover = UnifiedSettingsSkin.inside(mx, my, x, y, w, layout.rowH);
        ui.hoverAnim = AnimationUtility.approach(ui.hoverAnim, hover ? 1f : 0f, dt, 12f);

        TextRenderer font = SettingSkinUtil.modules() ? UnifiedSettingsSkin.fontRegular() : ClickGuiRenderer.getIosevkaRegular();
        float labelSize = SettingSkinUtil.m(18f, 6.8f);

        float cardX = x + SettingSkinUtil.m(2f, 2f);
        float cardY = y + SettingSkinUtil.m(3f, 2f);
        float cardW = Math.max(1f, w - SettingSkinUtil.m(4f, 4f));
        float cardH = Math.max(SettingSkinUtil.m(28f, 17f), layout.rowH - SettingSkinUtil.m(6f, 4f));
        float radius = SettingSkinUtil.m(8f, 5f);
        UnifiedSettingsSkin.drawCard(cardX, cardY, cardW, cardH, radius, 0.42f + ui.hoverAnim * 0.24f);
        float cardStrokeAnim = Math.max(ui.hoverAnim, ui.anim);
        if (cardStrokeAnim > 0.001f) {
            int strokeA = UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_FAINT, Math.round(ui.hoverAnim * 82f));
            int strokeB = UnifiedSettingsSkin.accentGradientStart(ui.anim * 0.50f);
            ClickGuiRenderer.drawRoundedRectStroke(cardX, cardY, cardW, cardH, radius, SettingSkinUtil.m(0.65f, 0.42f), UnifiedSettingsSkin.mix(strokeA, strokeB, Math.max(ui.anim, ui.hoverAnim * 0.28f)));
        }

        float sw = SettingSkinUtil.m(39f, 16f);
        float sh = SettingSkinUtil.m(18f, 8f);
        float sx = cardX + SettingSkinUtil.m(8f, 5f);
        float sy = cardY + (cardH - sh) * 0.5f;
        ui.lastControlX = sx;
        ui.lastControlY = sy;
        ui.lastControlW = sw;
        ui.lastControlH = sh;

        int bgOffA = UnifiedSettingsSkin.mix(UnifiedSettingsSkin.SURFACE, UnifiedSettingsSkin.SURFACE_HOVER, 0.45f + ui.hoverAnim * 0.20f);
        int bgOffB = UnifiedSettingsSkin.mix(bgOffA, 0xFF000000, 0.30f);
        int bgOnA = UnifiedSettingsSkin.accentGradientStart(1f);
        int bgOnB = UnifiedSettingsSkin.accentGradientEnd(1f);
        int bgA = UnifiedSettingsSkin.mix(bgOffA, bgOnA, ui.anim);
        int bgB = UnifiedSettingsSkin.mix(bgOffB, bgOnB, ui.anim);
        ClickGuiRenderer.drawRoundedRectGradient(sx, sy, sw, sh, sh * 0.5f, bgA, bgB, UnifiedSettingsSkin.ACCENT_GRADIENT_ANGLE);
        ClickGuiRenderer.drawRoundedRectStroke(sx, sy, sw, sh, sh * 0.5f, SettingSkinUtil.m(0.65f, 0.42f), UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_FAINT, 50 + Math.round(ui.hoverAnim * 44f)));

        float knob = sh - SettingSkinUtil.m(4f, 2.4f);
        float kx = sx + SettingSkinUtil.m(2f, 1.2f) + (sw - knob - SettingSkinUtil.m(4f, 2.4f)) * AnimationUtility.easeInOutCubic(ui.anim);
        float ky = sy + (sh - knob) * 0.5f;
        int knobCol = UnifiedSettingsSkin.mix(0xFFECEEF2, 0xFFFFFFFF, ui.anim * 0.55f + ui.hoverAnim * 0.18f);
        ClickGuiRenderer.drawRoundedRect(kx, ky, knob, knob, knob * 0.5f, knobCol);

        float labelX = sx + sw + SettingSkinUtil.m(9f, 6f);
        int labelColor = UnifiedSettingsSkin.mix(UnifiedSettingsSkin.TEXT_MUTED, UnifiedSettingsSkin.TEXT_PRIMARY, 0.72f + ui.hoverAnim * 0.16f + ui.anim * 0.12f);

        ClickGuiRenderer.beginTextBatch();
        for (int i = 0; i < layout.lines.length; i++) {
            ClickGuiRenderer.drawText(font, layout.lines[i], labelX, y + layout.labelY + i * (layout.lineH + SettingSkinUtil.m(2f, 0.7f)), labelSize, labelColor, false);
        }
        ClickGuiRenderer.endTextBatch();
    }

    public static void mouseClicked(BooleanSetting setting, double mx, double my, int button) {
        if (button != 0) return;
        BooleanSetting.UiState ui = setting.ui();
        if (!UnifiedSettingsSkin.inside(mx, my, ui.lastX, ui.lastY, ui.lastW, ui.lastH)) return;
        setting.set(!setting.get());
        ClickGuiSounds.changeMode();
        if (setting.getParent() != null) setting.getParent().saveConfig();
    }

    public static float getHeight(BooleanSetting setting) {
        return booleanLayout(setting, setting.ui().lastW > 0f ? setting.ui().lastW : SettingSkinUtil.m(170f, 115f)).rowH;
    }

    private static LayoutBoolean booleanLayout(BooleanSetting setting, float width) {
        BooleanSetting.UiState ui = setting.ui();
        TextRenderer font = SettingSkinUtil.modules() ? UnifiedSettingsSkin.fontRegular() : ClickGuiRenderer.getIosevkaRegular();
        float labelSize = SettingSkinUtil.m(18f, 6.8f);
        float switchW = SettingSkinUtil.m(39f, 16f);
        String labelText = setting.getDisplayName();
        boolean needsLayout = !Float.isFinite(ui.layoutW)
                || Math.abs(ui.layoutW - width) > 0.5f
                || !labelText.equals(ui.layoutLabel);
        if (needsLayout) {
            float lineH = UnifiedSettingsSkin.textHeight(font, labelSize);
            float labelMaxW = Math.max(SettingSkinUtil.m(72f, 36f), width - switchW - SettingSkinUtil.m(31f, 18f));
            var linesList = ClickGuiRenderer.wrapText(font, labelText, labelSize, labelMaxW, 2);
            String[] lines = linesList.toArray(new String[0]);
            float gap = SettingSkinUtil.m(2f, 0.7f);
            float textBlockH = lines.length * lineH + Math.max(0, lines.length - 1) * gap;
            float contentH = Math.max(textBlockH, SettingSkinUtil.m(18f, 8f));
            float minRow = SettingSkinUtil.m(44f, 24f);
            ui.layoutW = width;
            ui.layoutLabel = labelText;
            ui.layoutLines = lines;
            ui.layoutLineH = lineH;
            ui.layoutRowH = Math.max(minRow, contentH + SettingSkinUtil.m(14f, 8f));
            ui.layoutLabelY = (ui.layoutRowH - textBlockH) * 0.5f;
            ui.layoutControlY = (ui.layoutRowH - SettingSkinUtil.m(18f, 8f)) * 0.5f;
        }
        return new LayoutBoolean(ui.layoutLines, ui.layoutLineH, ui.layoutLabelY, ui.layoutControlY, ui.layoutRowH);
    }
}