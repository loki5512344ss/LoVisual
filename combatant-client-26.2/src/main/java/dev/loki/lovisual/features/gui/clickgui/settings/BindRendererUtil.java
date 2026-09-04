/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings;

import dev.loki.lovisual.features.gui.clickgui.ClickGuiRenderer;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.render.helpers.ScissorFunction;

final class BindRendererUtil {
    private BindRendererUtil() {
    }

    static float bindHeight(float lastW, String label, String display, String modeText) {
        float w = lastW > 0f ? lastW : SettingSkinUtil.m(160f, 115f);
        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        float size = SettingSkinUtil.m(17f, 7.0f);
        float small = SettingSkinUtil.m(11.5f, 5.7f);
        float pad = SettingSkinUtil.m(6f, 5f);
        float gap = SettingSkinUtil.m(4f, 4f);
        float chipPad = SettingSkinUtil.m(7f, 5f);
        float minLabelW = SettingSkinUtil.m(72f, 38f);
        float modeW = modeText == null ? 0f : Math.max(SettingSkinUtil.m(36f, 25f), UnifiedSettingsSkin.textWidth(font, modeText, small) + SettingSkinUtil.m(10f, 8f));
        float modeBlockW = modeW > 0f ? modeW + gap : 0f;
        String displayText = display == null || display.isBlank() ? "NONE" : display;
        float wantedChipW = UnifiedSettingsSkin.textWidth(font, displayText, size) + chipPad * 2f;
        float sameLineMaxChipW = Math.max(SettingSkinUtil.m(48f, 30f), w - pad * 3f - modeBlockW - minLabelW);
        boolean nextLine = sameLineMaxChipW < SettingSkinUtil.m(48f, 30f);
        if (!nextLine) {
            float chipW = Math.max(SettingSkinUtil.m(48f, 30f), Math.min(wantedChipW, sameLineMaxChipW));
            float labelMax = w - pad * 3f - modeBlockW - chipW;
            nextLine = labelMax < minLabelW;
        }
        return nextLine ? SettingSkinUtil.m(48f, 31f) : SettingSkinUtil.m(38f, 22f);
    }

    static float bindHoverAnim(Object uiObj, boolean hover) {
        float dt = AnimationUtility.deltaTime();
        if (uiObj instanceof KeyBindSetting.UiState ui) {
            ui.hoverAnim = AnimationUtility.approach(ui.hoverAnim, hover ? 1f : 0f, dt, 12f);
            return ui.hoverAnim;
        }
        if (uiObj instanceof FunctionBindSetting.UiState ui) {
            ui.hoverAnim = AnimationUtility.approach(ui.hoverAnim, hover ? 1f : 0f, dt, 12f);
            return ui.hoverAnim;
        }
        return hover ? 1f : 0f;
    }

    static float bindWaitAnim(Object uiObj, boolean waiting) {
        float dt = AnimationUtility.deltaTime();
        if (uiObj instanceof KeyBindSetting.UiState ui) {
            ui.waitAnim = AnimationUtility.approach(ui.waitAnim, waiting ? 1f : 0f, dt, 14f);
            return ui.waitAnim;
        }
        if (uiObj instanceof FunctionBindSetting.UiState ui) {
            ui.waitAnim = AnimationUtility.approach(ui.waitAnim, waiting ? 1f : 0f, dt, 14f);
            return ui.waitAnim;
        }
        return waiting ? 1f : 0f;
    }

    static void renderSmallModeChip(float x, float y, float w, float h, String text, float size, float hoverAnim, float waitAnim) {
        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        float inset = SettingSkinUtil.m(0.8f, 0.45f);
        float dx = x + inset;
        float dy = y + inset;
        float dw = Math.max(1f, w - inset * 2f);
        float dh = Math.max(1f, h - inset * 2f);
        float radius = Math.max(0f, SettingSkinUtil.m(6f, 4f) - inset);
        int bg = UnifiedSettingsSkin.mix(UnifiedSettingsSkin.SURFACE_SOFT, UnifiedSettingsSkin.SURFACE_HOVER, 0.45f + hoverAnim * 0.25f + waitAnim * 0.20f);
        ClickGuiRenderer.drawRoundedRect(dx, dy, dw, dh, radius, bg);
        ClickGuiRenderer.drawRoundedRectStroke(dx, dy, dw, dh, radius, SettingSkinUtil.m(0.55f, 0.35f), UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_FAINT, Math.round(34f + hoverAnim * 24f + waitAnim * 30f)));
        String fitted = UnifiedSettingsSkin.fit(font, text, size, Math.max(1f, w - SettingSkinUtil.m(6f, 6f)));
        float tw = UnifiedSettingsSkin.textWidth(font, fitted, size);
        int color = UnifiedSettingsSkin.mix(UnifiedSettingsSkin.TEXT_MUTED, UnifiedSettingsSkin.TEXT_PRIMARY, waitAnim * 0.35f + hoverAnim * 0.15f);
        ClickGuiRenderer.drawText(font, fitted, x + (w - tw) * 0.5f, y + (h - UnifiedSettingsSkin.textHeight(font, size)) * 0.5f, size, color, false);
    }

    static void renderBindBase(String label, String display, String modeText, boolean waiting, float x, float y, float w, float mx, float my, Object uiObj) {
        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        float size = SettingSkinUtil.m(17f, 7.0f);
        float small = SettingSkinUtil.m(11.5f, 5.7f);
        String displayText = display == null || display.isBlank() ? "NONE" : display;
        boolean none = displayText.equalsIgnoreCase("NONE");
        float pad = SettingSkinUtil.m(6f, 5f);
        float gap = SettingSkinUtil.m(4f, 4f);
        float chipPad = SettingSkinUtil.m(7f, 5f);
        float chipH = SettingSkinUtil.m(22f, 13.5f);
        float minLabelW = SettingSkinUtil.m(72f, 38f);
        float modeW = modeText == null ? 0f : Math.max(SettingSkinUtil.m(36f, 25f), UnifiedSettingsSkin.textWidth(font, modeText, small) + SettingSkinUtil.m(10f, 8f));
        float modeBlockW = modeW > 0f ? modeW + gap : 0f;
        float rawTextW = UnifiedSettingsSkin.textWidth(font, displayText, size);
        float wantedChipW = rawTextW + chipPad * 2f;

        float sameLineMaxChipW = Math.max(SettingSkinUtil.m(48f, 30f), w - pad * 3f - modeBlockW - minLabelW);
        boolean nextLine = sameLineMaxChipW < SettingSkinUtil.m(48f, 30f);
        float maxChipW = nextLine
                ? Math.max(SettingSkinUtil.m(48f, 30f), w - pad * 2f - modeBlockW)
                : sameLineMaxChipW;
        float chipW = Math.max(SettingSkinUtil.m(48f, 30f), Math.min(wantedChipW, maxChipW));
        float labelMax = nextLine ? w - pad * 2f : Math.max(1f, w - pad * 3f - modeBlockW - chipW);
        if (!nextLine && labelMax < minLabelW) {
            nextLine = true;
            maxChipW = Math.max(SettingSkinUtil.m(48f, 30f), w - pad * 2f - modeBlockW);
            chipW = Math.max(SettingSkinUtil.m(48f, 30f), Math.min(wantedChipW, maxChipW));
            labelMax = w - pad * 2f;
        }

        float rowH = nextLine ? SettingSkinUtil.m(48f, 31f) : SettingSkinUtil.m(38f, 22f);
        float cardX = x + SettingSkinUtil.m(2f, 2f);
        float cardY = y + SettingSkinUtil.m(3f, 2f);
        float cardW = Math.max(1f, w - SettingSkinUtil.m(4f, 4f));
        float cardH = Math.max(SettingSkinUtil.m(30f, 18f), rowH - SettingSkinUtil.m(6f, 4f));
        float cardRadius = SettingSkinUtil.m(8f, 5f);
        boolean hover = UnifiedSettingsSkin.inside(mx, my, cardX, cardY, cardW, cardH);
        float hoverAnim = bindHoverAnim(uiObj, hover);
        float waitAnim = bindWaitAnim(uiObj, waiting);

        UnifiedSettingsSkin.drawCard(cardX, cardY, cardW, cardH, cardRadius, 0.38f + hoverAnim * 0.24f + waitAnim * 0.10f);
        if (hoverAnim > 0.001f || waitAnim > 0.001f) {
            int strokeA = UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_FAINT, Math.round(hoverAnim * 72f));
            int strokeB = UnifiedSettingsSkin.accentGradientStart(waitAnim * 0.70f);
            ClickGuiRenderer.drawRoundedRectStroke(cardX, cardY, cardW, cardH, cardRadius, SettingSkinUtil.m(0.65f, 0.42f), UnifiedSettingsSkin.mix(strokeA, strokeB, Math.max(waitAnim, hoverAnim * 0.28f)));
        }

        float labelY = y + (nextLine ? SettingSkinUtil.m(7f, 4f) : (rowH - UnifiedSettingsSkin.textHeight(font, size)) * 0.5f);
        float chipY = nextLine ? y + SettingSkinUtil.m(27f, 16f) : y + (rowH - chipH) * 0.5f;
        float chipX;
        float modeX = 0f;
        float modeY = chipY;
        if (nextLine) {
            chipX = cardX + pad;
            if (modeW > 0f) modeX = x + w - pad - modeW;
        } else {
            chipX = x + w - pad - chipW;
            if (modeW > 0f) modeX = chipX - gap - modeW;
        }

        int labelColor = UnifiedSettingsSkin.mix(UnifiedSettingsSkin.TEXT_MUTED, UnifiedSettingsSkin.TEXT_PRIMARY, 0.72f + hoverAnim * 0.18f + waitAnim * 0.10f);
        ClickGuiRenderer.drawText(font, UnifiedSettingsSkin.fit(font, label, size, Math.max(1f, labelMax)), cardX + pad, labelY, size, labelColor, false);
        if (modeW > 0f) renderSmallModeChip(modeX, modeY, modeW, chipH, modeText, small, hoverAnim, waitAnim);

        float chipInset = SettingSkinUtil.m(0.8f, 0.45f);
        float chipDrawX = chipX + chipInset;
        float chipDrawY = chipY + chipInset;
        float chipDrawW = Math.max(1f, chipW - chipInset * 2f);
        float chipDrawH = Math.max(1f, chipH - chipInset * 2f);
        float chipRadius = Math.max(0f, SettingSkinUtil.m(6f, 4f) - chipInset);
        if (waiting) {
            UnifiedSettingsSkin.drawAccent(chipDrawX, chipDrawY, chipDrawW, chipDrawH, chipRadius, 0.62f + hoverAnim * 0.18f);
            ClickGuiRenderer.drawRoundedRectStrokeGradient(chipDrawX, chipDrawY, chipDrawW, chipDrawH, chipRadius, SettingSkinUtil.m(0.75f, 0.45f), UnifiedSettingsSkin.accentGradientStart(0.92f), UnifiedSettingsSkin.accentGradientEnd(0.92f), UnifiedSettingsSkin.ACCENT_GRADIENT_ANGLE);
        } else {
            UnifiedSettingsSkin.drawSurface(chipDrawX, chipDrawY, chipDrawW, chipDrawH, chipRadius, none ? 0.30f + hoverAnim * 0.12f : 0.46f + hoverAnim * 0.16f);
            int chipStroke = none
                    ? UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_FAINT, Math.round(34f + hoverAnim * 36f))
                    : UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.ACCENT, Math.round(48f + hoverAnim * 54f));
            ClickGuiRenderer.drawRoundedRectStroke(chipDrawX, chipDrawY, chipDrawW, chipDrawH, chipRadius, SettingSkinUtil.m(0.65f, 0.42f), chipStroke);
        }

        float contentX = chipX + chipPad;
        float contentW = Math.max(1f, chipW - chipPad * 2f);
        float textX = contentX;
        if (rawTextW <= contentW) {
            textX = chipX + (chipW - rawTextW) * 0.5f;
        }
        float textY = chipY + (chipH - UnifiedSettingsSkin.textHeight(font, size)) * 0.5f;
        boolean textClip = ScissorFunction.pushRaw(contentX, chipY, contentW, chipH);
        int valueColor = waiting
                ? 0xFFFFFFFF
                : UnifiedSettingsSkin.mix(UnifiedSettingsSkin.TEXT_MUTED, UnifiedSettingsSkin.TEXT_PRIMARY, none ? 0.20f + hoverAnim * 0.12f : 0.82f + hoverAnim * 0.12f);
        if (rawTextW > contentW + 0.5f) {
            SettingSkinUtil.drawBindMarquee(font, displayText, contentX, textY, rawTextW, contentW, size, valueColor);
        } else {
            ClickGuiRenderer.drawText(font, displayText, textX, textY, size, valueColor, false);
        }
        if (textClip) ScissorFunction.pop();

        if (uiObj instanceof KeyBindSetting.UiState ui) {
            ui.lastX = x; ui.lastY = y; ui.lastW = w; ui.lastH = rowH;
            ui.lastBx = chipX; ui.lastBy = chipY; ui.lastBw = chipW; ui.lastBh = chipH;
        } else if (uiObj instanceof FunctionBindSetting.UiState ui) {
            ui.lastX = x; ui.lastY = y; ui.lastW = w; ui.lastH = rowH;
            ui.lastBx = chipX; ui.lastBy = chipY; ui.lastBw = chipW; ui.lastBh = chipH;
        }
    }
}