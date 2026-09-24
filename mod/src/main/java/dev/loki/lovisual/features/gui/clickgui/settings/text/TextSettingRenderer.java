package dev.loki.lovisual.features.gui.clickgui.settings.text;

import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.SettingSkinUtil;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.UnifiedSettingsSkin;
import dev.loki.lovisual.features.gui.clickgui.settings.util.SettingTextInputUtil;
import dev.loki.lovisual.features.gui.clickgui.settings.text.TextSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.text.TextSettingTextInputHandler;

public final class TextSettingRenderer {
    private TextSettingRenderer() {
    }

    public static void render(TextSetting setting, float x, float y, float w, float mx, float my) {
        UnifiedSettingsSkin.syncTheme();
        TextSetting.UiState ui = setting.ui();
        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        float labelSize = SettingSkinUtil.m(15.5f, 6.3f);
        float fieldSize = SettingSkinUtil.m(14f, 6.1f);
        float fx = x + SettingSkinUtil.m(5f, 5f);
        float fy = y + SettingSkinUtil.m(23f, 11.5f);
        float fw = Math.max(1f, w - SettingSkinUtil.m(10f, 10f));
        float fh = SettingSkinUtil.m(20f, 13f);
        String value = setting.getEditorText();
        ui.fieldX = fx;
        ui.fieldY = fy;
        ui.fieldW = fw;
        ui.fieldH = fh;

        boolean hover = UnifiedSettingsSkin.inside(mx, my, fx, fy, fw, fh);
        float dt = AnimationUtility.deltaTime();
        ui.hoverAnim = AnimationUtility.approach(ui.hoverAnim, hover ? 1f : 0f, dt, 13f);
        ui.focusAnim = AnimationUtility.approach(ui.focusAnim, ui.editing ? 1f : 0f, dt, 12f);
        ui.errorAnim = AnimationUtility.approach(ui.errorAnim, 0f, dt, 7f);
        if (ui.editing) ui.cursorBlink += dt * 2.2f;
        else ui.cursorBlink = 0f;

        ClickGuiRenderer.drawText(font, UnifiedSettingsSkin.fit(font, setting.getDisplayName(), labelSize, fw), fx, y + SettingSkinUtil.m(5f, 2.5f), labelSize, UnifiedSettingsSkin.TEXT_PRIMARY, false);
        float fieldAnim = Math.max(ui.hoverAnim * 0.65f, ui.focusAnim);
        int bg = UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.mix(UnifiedSettingsSkin.SURFACE_SOFT, UnifiedSettingsSkin.SURFACE_HOVER, fieldAnim), 56 + Math.round(fieldAnim * 42f));
        int stroke = UnifiedSettingsSkin.mix(UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_FAINT, 58), UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.ACCENT, 155), ui.focusAnim);
        stroke = UnifiedSettingsSkin.mix(stroke, 0xFFFF5F66, ui.errorAnim);
        if (UnifiedSettingsSkin.SURFACE_GRADIENT_ENABLED) {
            UnifiedSettingsSkin.drawSurface(fx, fy, fw, fh, SettingSkinUtil.m(5f, 3.5f), 0.55f + fieldAnim * 0.32f);
        } else {
            ClickGuiRenderer.drawRoundedRect(fx, fy, fw, fh, SettingSkinUtil.m(5f, 3.5f), bg);
        }
        ClickGuiRenderer.drawRoundedRectStroke(fx, fy, fw, fh, SettingSkinUtil.m(5f, 3.5f), SettingSkinUtil.m(0.7f, 0.45f), stroke);

        String text = ui.editing ? ui.editBuffer : (value == null ? "" : value);
        String placeholder = "text...";
        float pad = SettingSkinUtil.m(6f, 4f);
        float textX = fx + pad;
        float textY = fy + (fh - UnifiedSettingsSkin.textHeight(font, fieldSize)) * 0.5f - SettingSkinUtil.m(0.2f, 0.15f);
        float clipW = Math.max(1f, fw - pad * 2f);
        if (ui.editing) TextSettingTextInputHandler.ensureTextCursorVisible(ui, font, ui.editBuffer, fieldSize, clipW);
        else ui.textScroll = 0f;
        float drawX = textX - ui.textScroll;
        ui.textX = drawX;
        ui.textY = textY;
        ui.textSize = fieldSize;
        boolean clip = ScissorFunction.pushRaw(textX, fy, clipW, fh);
        if (ui.editing) {
            SettingTextInputUtil.renderInlineSelection(font, ui.editBuffer, TextSettingTextInputHandler.textSelStart(ui), TextSettingTextInputHandler.textSelEnd(ui), drawX, textY, fieldSize, fh, fy);
            ClickGuiRenderer.drawText(font, ui.editBuffer, drawX, textY, fieldSize, UnifiedSettingsSkin.TEXT_PRIMARY, false);
            if (((int) (ui.cursorBlink * 2f) & 1) == 0) {
                int cursor = Math.max(0, Math.min(ui.editCursor, ui.editBuffer.length()));
                float cx = drawX + UnifiedSettingsSkin.textWidth(font, ui.editBuffer.substring(0, cursor), fieldSize) + SettingSkinUtil.m(0.5f, 0.3f);
                ClickGuiRenderer.drawRect(cx, fy + SettingSkinUtil.m(3f, 2f), SettingSkinUtil.m(0.65f, 0.4f), fh - SettingSkinUtil.m(6f, 4f), UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_PRIMARY, 205));
            }
        } else {
            boolean empty = text.isBlank();
            String draw = empty ? placeholder : UnifiedSettingsSkin.fit(font, text, fieldSize, fw - pad * 2f);
            ClickGuiRenderer.drawText(font, draw, textX, textY, fieldSize, empty ? UnifiedSettingsSkin.TEXT_FAINT : UnifiedSettingsSkin.TEXT_PRIMARY, false);
        }
        if (clip) ScissorFunction.pop();
    }

    public static void mouseClicked(TextSetting setting, double mx, double my, int button) {
        if (button != 0) return;
        TextSetting.UiState ui = setting.ui();
        if (UnifiedSettingsSkin.inside(mx, my, ui.fieldX, ui.fieldY, ui.fieldW, ui.fieldH)) {
            TextSettingTextInputHandler.beginTextEdit(setting);
            TextSettingTextInputHandler.setTextCursorFromMouse(setting, (float) mx, false);
        } else if (ui.editing) {
            TextSettingTextInputHandler.commitTextEdit(setting, true);
        }
    }

    public static void mouseClickedOutside(TextSetting setting, double mx, double my, int button) {
        if (button != 0) return;
        if (setting.ui().editing) TextSettingTextInputHandler.commitTextEdit(setting, true);
    }

    public static float getHeight(TextSetting setting) {
        return SettingSkinUtil.m(50f, 28f);
    }
}
