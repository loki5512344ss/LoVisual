/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings.text.list;

import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;

import java.util.List;
import java.util.Set;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.SettingSkinUtil;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.UnifiedSettingsSkin;
import dev.loki.lovisual.features.gui.clickgui.settings.util.SettingTextInputUtil;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListTextEditHandler;

public final class TextListSettingRenderer {
    private TextListSettingRenderer() {
    }

    public static void render(TextListSetting setting, float x, float y, float w, float mx, float my) {
        UnifiedSettingsSkin.syncTheme();
        TextListSetting.UiState ui = setting.ui();
        ui.lastX = x;
        ui.lastY = y;
        ui.lastW = w;

        if (isInlineTextList(setting)) {
            renderInlineTextList(setting, x, y, w, mx, my);
            return;
        }

        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        float labelSize = SettingSkinUtil.m(15.5f, 6.4f);
        float small = SettingSkinUtil.m(11.5f, 5.7f);
        float bh = SettingSkinUtil.m(18f, 13f);
        float editW = Math.max(SettingSkinUtil.m(36f, 28f), UnifiedSettingsSkin.textWidth(font, "Edit", small) + SettingSkinUtil.m(12f, 12f));
        float labelX = x + SettingSkinUtil.m(5f, 5f);
        float labelMax = Math.max(SettingSkinUtil.m(50f, 34f), w - editW - SettingSkinUtil.m(22f, 16f));
        List<String> labelLines = ClickGuiRenderer.wrapText(font, setting.getDisplayName(), labelSize, labelMax, 2);
        if (labelLines.isEmpty()) labelLines = java.util.List.of(setting.getDisplayName());
        float lineH = UnifiedSettingsSkin.textHeight(font, labelSize);
        float lineGap = SettingSkinUtil.m(2f, 0.7f);
        float textBlockH = labelLines.size() * lineH + Math.max(0, labelLines.size() - 1) * lineGap;
        float rowH = Math.max(SettingSkinUtil.m(44f, 24f), textBlockH + SettingSkinUtil.m(14f, 8f));
        float bx = x + w - SettingSkinUtil.m(9f, 9f) - editW;
        float by = y + (rowH - bh) * 0.5f;
        ui.lastButtonX = bx;
        ui.lastButtonY = by;
        ui.lastButtonW = editW;
        ui.lastButtonH = bh;
        boolean hover = UnifiedSettingsSkin.inside(mx, my, bx, by, editW, bh);
        ui.moduleHoverAnim = AnimationUtility.approach(ui.moduleHoverAnim, hover ? 1f : 0f, AnimationUtility.deltaTime(), 13f);
        float labelY = y + (rowH - textBlockH) * 0.5f;
        for (int i = 0; i < labelLines.size(); i++) {
            ClickGuiRenderer.drawText(font, labelLines.get(i), labelX, labelY + i * (lineH + lineGap), labelSize, UnifiedSettingsSkin.TEXT_PRIMARY, false);
        }
        SettingSkinUtil.renderChipButton(bx, by, editW, bh, "Edit", ui.moduleHoverAnim, false);
    }

    private static void renderInlineTextList(TextListSetting setting, float x, float y, float w, float mx, float my) {
        TextListSetting.UiState ui = setting.ui();
        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        float labelSize = SettingSkinUtil.m(15.5f, 6.3f);
        float fieldSize = SettingSkinUtil.m(14f, 6.1f);
        String label = setting.getDisplayName();
        float labelY = y + SettingSkinUtil.m(5f, 2.5f);
        float fieldY = y + SettingSkinUtil.m(23f, 11.5f);
        float fieldH = SettingSkinUtil.m(20f, 13f);
        float fieldX = x + SettingSkinUtil.m(5f, 5f);
        float fieldW = Math.max(1f, w - SettingSkinUtil.m(10f, 10f));
        ui.fieldX = fieldX;
        ui.fieldY = fieldY;
        ui.fieldW = fieldW;
        ui.fieldH = fieldH;
        boolean hover = UnifiedSettingsSkin.inside(mx, my, fieldX, fieldY, fieldW, fieldH);
        float dt = AnimationUtility.deltaTime();
        ui.fieldHoverAnim = AnimationUtility.approach(ui.fieldHoverAnim, hover ? 1f : 0f, dt, 13f);
        ui.fieldFocusAnim = AnimationUtility.approach(ui.fieldFocusAnim, ui.editing ? 1f : 0f, dt, 12f);
        ui.fieldErrorAnim = AnimationUtility.approach(ui.fieldErrorAnim, 0f, dt, 7f);
        if (ui.editing) ui.cursorBlink += dt * 2.2f;
        else ui.cursorBlink = 0f;

        ClickGuiRenderer.drawText(font, UnifiedSettingsSkin.fit(font, label, labelSize, fieldW), fieldX, labelY, labelSize, UnifiedSettingsSkin.TEXT_PRIMARY, false);
        float fieldAnim = Math.max(ui.fieldHoverAnim * 0.65f, ui.fieldFocusAnim);
        int bg = UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.mix(UnifiedSettingsSkin.SURFACE_SOFT, UnifiedSettingsSkin.SURFACE_HOVER, fieldAnim), 56 + Math.round(fieldAnim * 42f));
        int stroke = UnifiedSettingsSkin.mix(UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_FAINT, 58), UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.ACCENT, 155), ui.fieldFocusAnim);
        stroke = UnifiedSettingsSkin.mix(stroke, 0xFFFF5F66, ui.fieldErrorAnim);
        if (UnifiedSettingsSkin.SURFACE_GRADIENT_ENABLED) {
            UnifiedSettingsSkin.drawSurface(fieldX, fieldY, fieldW, fieldH, SettingSkinUtil.m(5f, 3.5f), 0.55f + fieldAnim * 0.32f);
        } else {
            ClickGuiRenderer.drawRoundedRect(fieldX, fieldY, fieldW, fieldH, SettingSkinUtil.m(5f, 3.5f), bg);
        }
        ClickGuiRenderer.drawRoundedRectStroke(fieldX, fieldY, fieldW, fieldH, SettingSkinUtil.m(5f, 3.5f), SettingSkinUtil.m(0.7f, 0.45f), stroke);

        String current = firstTextListEntry(setting);
        String text = ui.editing ? ui.editBuffer : current;
        if (text == null) text = "";
        String placeholder = "value...";
        float pad = SettingSkinUtil.m(6f, 4f);
        float textX = fieldX + pad;
        float textY = fieldY + (fieldH - UnifiedSettingsSkin.textHeight(font, fieldSize)) * 0.5f - SettingSkinUtil.m(0.2f, 0.15f);
        boolean clip = ScissorFunction.pushRaw(textX, fieldY, Math.max(1f, fieldW - pad * 2f), fieldH);
        if (ui.editing) {
            SettingTextInputUtil.renderInlineSelection(font, ui.editBuffer, TextListTextEditHandler.textListSelStart(ui), TextListTextEditHandler.textListSelEnd(ui), textX, textY, fieldSize, fieldH, fieldY);
            ClickGuiRenderer.drawText(font, ui.editBuffer, textX, textY, fieldSize, UnifiedSettingsSkin.TEXT_PRIMARY, false);
            if (((int) (ui.cursorBlink * 2f) & 1) == 0) {
                int cursor = Math.max(0, Math.min(ui.editCursor, ui.editBuffer.length()));
                float cx = textX + UnifiedSettingsSkin.textWidth(font, ui.editBuffer.substring(0, cursor), fieldSize) + SettingSkinUtil.m(0.5f, 0.3f);
                ClickGuiRenderer.drawRect(cx, fieldY + SettingSkinUtil.m(3f, 2f), SettingSkinUtil.m(0.65f, 0.4f), fieldH - SettingSkinUtil.m(6f, 4f), UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_PRIMARY, 205));
            }
        } else {
            boolean empty = text.isBlank();
            String draw = empty ? placeholder : UnifiedSettingsSkin.fit(font, text, fieldSize, fieldW - pad * 2f);
            ClickGuiRenderer.drawText(font, draw, textX, textY, fieldSize, empty ? UnifiedSettingsSkin.TEXT_FAINT : UnifiedSettingsSkin.TEXT_PRIMARY, false);
        }
        if (clip) ScissorFunction.pop();
    }

    public static void mouseClicked(TextListSetting setting, double mx, double my, int button) {
        if (button != 0) return;
        TextListSetting.UiState ui = setting.ui();
        if (isInlineTextList(setting)) {
            if (UnifiedSettingsSkin.inside(mx, my, ui.fieldX, ui.fieldY, ui.fieldW, ui.fieldH)) {
                TextListTextEditHandler.beginTextListEdit(setting);
                TextListTextEditHandler.setTextListCursorFromMouse(setting, (float) mx, false);
            } else if (ui.editing) {
                TextListTextEditHandler.commitTextListEdit(setting, true);
            }
            return;
        }
        if (!UnifiedSettingsSkin.inside(mx, my, ui.lastButtonX, ui.lastButtonY, ui.lastButtonW, ui.lastButtonH)) return;
        if (setting.getPickerMode() == TextListSetting.PickerMode.TEXT) {
            ClickGuiRenderer.openTextEditor(setting, setting.getDisplayName(), setting.getEditorText(), ui.lastButtonX, ui.lastButtonY, ui.lastButtonW);
        } else {
            ClickGuiRenderer.openPicker(setting, setting.getDisplayName(), setting.getPickerMode());
        }
    }

    public static void mouseClickedOutside(TextListSetting setting, double mx, double my, int button) {
        if (button != 0 || !isInlineTextList(setting)) return;
        if (setting.ui().editing) TextListTextEditHandler.commitTextListEdit(setting, true);
    }

    public static float getHeight(TextListSetting setting) {
        return isInlineTextList(setting) ? inlineTextListHeight() : textListButtonHeight(setting);
    }

    static boolean isInlineTextList(TextListSetting setting) {
        return setting.getPickerMode() == TextListSetting.PickerMode.TEXT && setting.isSingleLine();
    }

    private static float inlineTextListHeight() {
        return SettingSkinUtil.m(50f, 28f);
    }

    private static String firstTextListEntry(TextListSetting setting) {
        Set<String> set = setting.getValueSet();
        if (set == null || set.isEmpty()) return "";
        return set.iterator().next();
    }

    private static float textListButtonHeight(TextListSetting setting) {
        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        float width = setting.ui().lastW > 0f ? setting.ui().lastW : SettingSkinUtil.m(170f, 115f);
        float labelSize = SettingSkinUtil.m(15.5f, 6.4f);
        float small = SettingSkinUtil.m(11.5f, 5.7f);
        float editW = Math.max(SettingSkinUtil.m(36f, 28f), UnifiedSettingsSkin.textWidth(font, "Edit", small) + SettingSkinUtil.m(12f, 12f));
        float labelMax = Math.max(SettingSkinUtil.m(50f, 34f), width - editW - SettingSkinUtil.m(22f, 16f));
        List<String> labelLines = ClickGuiRenderer.wrapText(font, setting.getDisplayName(), labelSize, labelMax, 2);
        int lines = Math.max(1, labelLines.size());
        float lineH = UnifiedSettingsSkin.textHeight(font, labelSize);
        float textBlockH = lines * lineH + Math.max(0, lines - 1) * SettingSkinUtil.m(2f, 0.7f);
        return Math.max(SettingSkinUtil.m(44f, 24f), textBlockH + SettingSkinUtil.m(14f, 8f));
    }

    static String firstEntry(TextListSetting setting) {
        return firstTextListEntry(setting);
    }
}
