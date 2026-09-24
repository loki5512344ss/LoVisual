package dev.loki.lovisual.features.gui.clickgui.settings.slider;

import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;
import dev.loki.lovisual.render.helpers.util.SystemCursor;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.SettingSkinUtil;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.UnifiedSettingsSkin;
import dev.loki.lovisual.features.gui.clickgui.settings.util.SettingTextInputUtil;
import dev.loki.lovisual.features.gui.clickgui.settings.slider.SliderSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.slider.SliderTextInputHandler;
import dev.loki.lovisual.features.gui.clickgui.settings.slider.SliderTextInputState;

public final class SliderSettingRenderer {
    private SliderSettingRenderer() {
    }

    public static <N extends Number> void render(SliderSetting<N> setting, float x, float y, float w, float mx, float my) {
        UnifiedSettingsSkin.syncTheme();
        SliderSetting.UiState ui = setting.ui();
        NumberValue<N> value = setting.value();
        double min = value.getMin().doubleValue();
        double max = value.getMax().doubleValue();
        double val = value.get().doubleValue();

        ui.lastX = x;
        ui.lastY = y;
        ui.lastW = w;

        float dt = AnimationUtility.deltaTime();
        if (ui.dragging && !ui.editing) {
            SliderTextInputHandler.setSliderValueFromMouse(setting, mx, x + SettingSkinUtil.m(6f, 5f), Math.max(1f, w - SettingSkinUtil.m(6f, 5f) * 2f), min, max);
            val = value.get().doubleValue();
        }

        float trackPad = SettingSkinUtil.m(6f, 5f);
        float trackX = x + trackPad;
        float trackW = Math.max(1f, w - trackPad * 2f);
        float trackY = y + SettingSkinUtil.m(38f, 15f);
        float trackH = SettingSkinUtil.m(2.4f, 1.2f);

        float target = max == min ? 0f : (float) ((val - min) / (max - min));
        target = UnifiedSettingsSkin.clamp01(target);
        ui.anim = AnimationUtility.approach(ui.anim, target, 0.40f);
        boolean trackHover = UnifiedSettingsSkin.inside(mx, my, trackX, trackY - SettingSkinUtil.m(6f, 2f), trackW, trackH + SettingSkinUtil.m(12f, 4f));
        if ((trackHover && !ui.editing) || ui.dragging) {
            SystemCursor.set(SystemCursor.CursorType.RESIZE_HORIZONTAL);
        }
        ui.hoverAnim = AnimationUtility.approach(ui.hoverAnim, trackHover ? 1f : 0f, dt, 12f);
        ui.editAnim = AnimationUtility.approach(ui.editAnim, ui.editing ? 1f : 0f, dt, 14f);
        ui.errorAnim = AnimationUtility.approach(ui.errorAnim, 0f, dt, 7.5f);
        if (ui.editing) ui.cursorBlink += dt * 2.3f;
        else ui.cursorBlink = 0f;

        TextRenderer font = SettingSkinUtil.modules() ? UnifiedSettingsSkin.fontRegular() : ClickGuiRenderer.getIosevkaRegular();
        float labelSize = SettingSkinUtil.m(17f, 6.8f);
        float valueSize = SettingSkinUtil.m(17f, 6.8f);
        float textY = y + SettingSkinUtil.m(6f, 1.9f);
        String display = ui.editing ? ui.editBuffer : value.toDisplay();
        if (display == null) display = "";
        float valuePad = SettingSkinUtil.m(6f, 4f);
        float rawValueW = UnifiedSettingsSkin.textWidth(font, display.isEmpty() ? "0" : display, valueSize) + valuePad * 2f;
        float valueW = Math.max(SettingSkinUtil.m(34f, 20f), rawValueW);
        valueW = Math.min(valueW, Math.max(SettingSkinUtil.m(46f, 28f), w * 0.48f));
        ui.valueX = x + w - valueW - trackPad;
        ui.valueY = textY - SettingSkinUtil.m(4f, 1.5f);
        ui.valueW = valueW;
        ui.valueH = UnifiedSettingsSkin.textHeight(font, valueSize) + SettingSkinUtil.m(8f, 4f);
        boolean valueHover = UnifiedSettingsSkin.inside(mx, my, ui.valueX, ui.valueY, ui.valueW, ui.valueH);
        ui.valueHoverAnim = AnimationUtility.approach(ui.valueHoverAnim, valueHover ? 1f : 0f, dt, 12f);
        ui.inputHoverAnim = AnimationUtility.approach(ui.inputHoverAnim, valueHover ? 1f : 0f, dt, 15f);
        ui.inputFocusGlowAnim = AnimationUtility.approach(ui.inputFocusGlowAnim, ui.editing ? 1f : 0f, dt, 12f);
        ui.inputPressAnim = AnimationUtility.approach(ui.inputPressAnim, ui.editing ? 1f : 0f, dt, 10f);

        int textCol = UnifiedSettingsSkin.mix(UnifiedSettingsSkin.TEXT_PRIMARY, 0xFFFF7777, ui.errorAnim * 0.85f);
        float fieldAnim = Math.max(ui.inputHoverAnim * 0.62f, ui.inputFocusGlowAnim);
        int fieldBg = UnifiedSettingsSkin.withAlpha(
                UnifiedSettingsSkin.mix(UnifiedSettingsSkin.SURFACE_SOFT, UnifiedSettingsSkin.SURFACE_HOVER, fieldAnim * 0.72f),
                Math.round(fieldAnim * 104f)
        );
        int fieldStrokeBase = UnifiedSettingsSkin.mix(
                UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_FAINT, Math.round(fieldAnim * 70f)),
                UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.ACCENT, Math.round(ui.inputFocusGlowAnim * 160f)),
                ui.inputFocusGlowAnim
        );
        int fieldStroke = UnifiedSettingsSkin.mix(fieldStrokeBase, 0xFFFF5F66, ui.errorAnim);

        ClickGuiRenderer.beginTextBatch();
        String label = setting.getDisplayName();
        float labelMax = Math.max(1f, ui.valueX - trackX - SettingSkinUtil.m(7f, 5f));
        ClickGuiRenderer.drawText(font, UnifiedSettingsSkin.fit(font, label, labelSize, labelMax), trackX, textY, labelSize, UnifiedSettingsSkin.TEXT_PRIMARY, false);
        ClickGuiRenderer.endTextBatch();

        if (fieldAnim > 0.001f || ui.errorAnim > 0.001f) {
            float lift = ui.inputFocusGlowAnim * SettingSkinUtil.m(1.0f, 0.45f);
            if (UnifiedSettingsSkin.SURFACE_GRADIENT_ENABLED) {
                UnifiedSettingsSkin.drawSurface(ui.valueX, ui.valueY - lift, ui.valueW, ui.valueH + lift * 2f, SettingSkinUtil.m(6f, 3.5f), fieldAnim);
            } else {
                ClickGuiRenderer.drawRoundedRect(ui.valueX, ui.valueY - lift, ui.valueW, ui.valueH + lift * 2f, SettingSkinUtil.m(6f, 3.5f), fieldBg);
            }
            if (ui.inputFocusGlowAnim > 0.02f) {
                ClickGuiRenderer.drawRoundedRectStrokeGradient(ui.valueX, ui.valueY - lift, ui.valueW, ui.valueH + lift * 2f, SettingSkinUtil.m(6f, 3.5f), SettingSkinUtil.m(0.8f, 0.45f), UnifiedSettingsSkin.accentGradientStart(0.72f), UnifiedSettingsSkin.accentGradientEnd(0.72f), UnifiedSettingsSkin.ACCENT_GRADIENT_ANGLE);
            } else {
                ClickGuiRenderer.drawRoundedRectStroke(ui.valueX, ui.valueY - lift, ui.valueW, ui.valueH + lift * 2f, SettingSkinUtil.m(6f, 3.5f), SettingSkinUtil.m(0.7f, 0.45f), fieldStroke);
            }
            if (ui.errorAnim > 0.01f) {
                ClickGuiRenderer.drawRoundedRectStroke(ui.valueX, ui.valueY - lift, ui.valueW, ui.valueH + lift * 2f, SettingSkinUtil.m(6f, 3.5f), SettingSkinUtil.m(1.0f, 0.55f), UnifiedSettingsSkin.withAlpha(0xFFFF5F66, Math.round(170f * ui.errorAnim)));
            }
        }

        float textBaseY = y + SettingSkinUtil.m(6f, 1.9f);
        ui.valueTextSize = valueSize;
        ui.valueTextPad = valuePad;
        ui.valueTextY = textBaseY;
        String drawValue = ui.editing ? display : UnifiedSettingsSkin.fit(font, display, valueSize, ui.valueW - valuePad * 2f);
        float drawW = UnifiedSettingsSkin.textWidth(font, drawValue, valueSize);
        float drawX = ui.editing ? ui.valueX + valuePad : ui.valueX + ui.valueW - drawW - valuePad;
        ui.valueTextX = drawX;
        boolean textClip = ScissorFunction.pushRaw(ui.valueX + valuePad * 0.65f, ui.valueY, Math.max(1f, ui.valueW - valuePad * 1.3f), ui.valueH);
        if (ui.editing) {
            SettingTextInputUtil.renderInlineSelection(font, ui.editBuffer, SliderTextInputState.sliderSelStart(ui), SliderTextInputState.sliderSelEnd(ui), drawX, textBaseY, valueSize, ui.valueH, 0f);
        }
        ClickGuiRenderer.drawText(font, drawValue, drawX, textBaseY, valueSize, textCol, false);
        if (ui.editing && ((int) (ui.cursorBlink * 2f) & 1) == 0) {
            int cursor = Math.max(0, Math.min(ui.editCursor, ui.editBuffer.length()));
            float cx = drawX + UnifiedSettingsSkin.textWidth(font, ui.editBuffer.substring(0, cursor), valueSize) + SettingSkinUtil.m(0.5f, 0.3f);
            ClickGuiRenderer.drawRect(cx, ui.valueY + SettingSkinUtil.m(3f, 1.5f), SettingSkinUtil.m(0.75f, 0.45f), ui.valueH - SettingSkinUtil.m(6f, 3f), UnifiedSettingsSkin.withAlpha(textCol, 205));
        }
        if (textClip) ScissorFunction.pop();

        int baseTrack = UnifiedSettingsSkin.mix(UnifiedSettingsSkin.SURFACE, UnifiedSettingsSkin.SURFACE_HOVER, 0.55f + ui.hoverAnim * 0.15f);
        ClickGuiRenderer.drawRoundedRect(trackX, trackY, trackW, trackH, 0.6f * SettingSkinUtil.s(), baseTrack);
        float fillW = trackW * ui.anim;
        if (fillW > 0.5f) {
            ClickGuiRenderer.drawGradientRect(trackX, trackY, fillW, trackH, UnifiedSettingsSkin.accentGradientStart(1f), UnifiedSettingsSkin.accentGradientEnd(1f), UnifiedSettingsSkin.accentGradientEnd(1f), UnifiedSettingsSkin.accentGradientStart(1f));
        }

        float knobW = SettingSkinUtil.m(2.4f, 1.2f);
        float knobH = SettingSkinUtil.m(7.2f, 4.2f);
        float knobX = trackX + fillW - knobW * 0.5f;
        float knobY = trackY - (knobH - trackH) * 0.5f;
        int knobCol = UnifiedSettingsSkin.mix(0xFFE1E1E1, UnifiedSettingsSkin.accentGradientStart(1f), 0.25f + ui.hoverAnim * 0.20f);
        ClickGuiRenderer.drawRoundedRect(knobX, knobY, knobW, knobH, knobW * 0.5f, knobCol);
    }

    public static <N extends Number> void mouseClicked(SliderSetting<N> setting, double mx, double my, int button) {
        if (button != 0) return;
        float trackPad = SettingSkinUtil.m(6f, 5f);
        SliderSetting.UiState ui = setting.ui();
        if (ui.editing) {
            if (UnifiedSettingsSkin.inside(mx, my, ui.valueX, ui.valueY, ui.valueW, ui.valueH)) {
                SliderTextInputHandler.setSliderEditCursorFromMouse(setting, (float) mx, false);
            } else {
                SliderTextInputHandler.commitSliderEdit(setting, true);
            }
            return;
        }
        if (UnifiedSettingsSkin.inside(mx, my, ui.valueX, ui.valueY, ui.valueW, ui.valueH)) {
            SliderTextInputHandler.beginSliderEdit(setting);
            SliderTextInputHandler.setSliderEditCursorFromMouse(setting, (float) mx, false);
            return;
        }
        float trackX = ui.lastX + trackPad;
        float trackW = Math.max(1f, ui.lastW - trackPad * 2f);
        float trackY = ui.lastY + SettingSkinUtil.m(38f, 15f);
        float trackH = SettingSkinUtil.m(2.4f, 1.2f);
        if (!UnifiedSettingsSkin.inside(mx, my, trackX, trackY - SettingSkinUtil.m(6f, 2f), trackW, trackH + SettingSkinUtil.m(12f, 4f))) return;
        ui.dragging = true;
        SliderTextInputHandler.setSliderValueFromMouse(setting, (float) mx, trackX, trackW, setting.value().getMin().doubleValue(), setting.value().getMax().doubleValue());
    }

    public static <N extends Number> void mouseReleased(SliderSetting<N> setting, double mx, double my, int button) {
        if (button != 0 || !setting.ui().dragging) return;
        setting.ui().dragging = false;
        if (setting.getParent() != null) setting.getParent().saveConfig();
    }

    public static <N extends Number> void mouseClickedOutside(SliderSetting<N> setting, double mx, double my, int button) {
        if (button != 0) return;
        SliderSetting.UiState ui = setting.ui();
        if (!ui.editing) return;
        SliderTextInputHandler.commitSliderEdit(setting, true);
    }

    public static float getHeight(SliderSetting<?> setting) {
        return SettingSkinUtil.m(58f, 24f);
    }
}
