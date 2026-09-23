/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.picker;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;
import net.minecraft.util.Util;

final class ClickGuiPickerFilterRenderer {
    private ClickGuiPickerFilterRenderer() {}

    static void drawFilterPill(ClickGuiPickerState s, float mx, float my, SettingsGuiPalette p, Themes.Theme th) {
        boolean hover = ClickGuiPickerState.inside(mx, my, s.filterX, s.filterY, ClickGuiPickerState.FILTER_W, ClickGuiPickerState.FILTER_H);
        boolean allHover = hover && mx < s.filterX + ClickGuiPickerState.FILTER_W * 0.5f;
        boolean selHover = hover && !allHover;
        float dt = AnimationUtility.deltaTime();
        s.allFilterHoverAnim = AnimationUtility.snap(AnimationUtility.approach(s.allFilterHoverAnim, allHover ? 1f : 0f, dt, 12f), allHover ? 1f : 0f, 0.01f);
        s.selectedFilterHoverAnim = AnimationUtility.snap(AnimationUtility.approach(s.selectedFilterHoverAnim, selHover ? 1f : 0f, dt, 12f), selHover ? 1f : 0f, 0.01f);
        int baseLeft = SettingsGuiPalette.mix(p.panelPillBase(), p.panelBgLeft(), 0.34f);
        int baseRight = SettingsGuiPalette.mix(p.panelPillBase(), p.panelBgRight(), 0.46f);
        if (hover) {
            float ha = Math.max(s.allFilterHoverAnim, s.selectedFilterHoverAnim);
            baseLeft = SettingsGuiPalette.mix(baseLeft, p.menuCategoryHoverLeft(), 0.18f * ha);
            baseRight = SettingsGuiPalette.mix(baseRight, p.menuCategoryHoverRight(), 0.18f * ha);
        }
        ClickGuiRenderer.drawBlur(s.filterX, s.filterY, ClickGuiPickerState.FILTER_W, ClickGuiPickerState.FILTER_H, 4f * ClickGuiPickerState.SCALE, p.panelBlurTint(), 150f / 255f);
        LayoutRender2D.roundedQuad(s.filterX, s.filterY, ClickGuiPickerState.FILTER_W, ClickGuiPickerState.FILTER_H, 3.5f * ClickGuiPickerState.SCALE, baseLeft, baseRight, baseRight, baseLeft);
        LayoutRender2D.roundedStrokeQuad(s.filterX, s.filterY, ClickGuiPickerState.FILTER_W, ClickGuiPickerState.FILTER_H, 3.5f * ClickGuiPickerState.SCALE, 0.5f * ClickGuiPickerState.SCALE,
                SettingsGuiPalette.mix(p.panelStroke(), p.moduleDividerStart(), 0.20f), SettingsGuiPalette.mix(p.panelStroke(), p.moduleDividerEnd(), 0.20f),
                SettingsGuiPalette.mix(p.panelStroke(), p.moduleDividerEnd(), 0.20f), SettingsGuiPalette.mix(p.panelStroke(), p.moduleDividerStart(), 0.20f));
        float pad = 1.2f * ClickGuiPickerState.SCALE;
        float half = (ClickGuiPickerState.FILTER_W - pad * 2f) * 0.5f;
        float activeX = s.filterX + pad + half * s.modeAnim;
        float activeHover = s.showAll ? s.allFilterHoverAnim : s.selectedFilterHoverAnim;
        int activeA = SettingsGuiPalette.mix(p.panelPillActive(), SettingsGuiPalette.withAlpha(th.accentSoft(), 230), 0.18f + activeHover * 0.10f);
        int activeB = SettingsGuiPalette.mix(p.panelPillActive(), SettingsGuiPalette.withAlpha(th.accent(), 235), 0.12f + activeHover * 0.14f);
        int activeC = SettingsGuiPalette.mix(SettingsGuiPalette.darken(p.panelPillActive(), 0.18f), SettingsGuiPalette.withAlpha(th.accentSoft(), 215), 0.14f + activeHover * 0.10f);
        LayoutRender2D.roundedQuad(activeX, s.filterY + pad, half, ClickGuiPickerState.FILTER_H - pad * 2f, 3f * ClickGuiPickerState.SCALE, activeA, activeB, activeC, activeA);
        LayoutRender2D.roundedStrokeQuad(activeX, s.filterY + pad, half, ClickGuiPickerState.FILTER_H - pad * 2f, 3f * ClickGuiPickerState.SCALE, 0.35f * ClickGuiPickerState.SCALE,
                SettingsGuiPalette.withAlpha(th.accentSoft(), 155), SettingsGuiPalette.withAlpha(th.accent(), 165),
                SettingsGuiPalette.withAlpha(th.accentSoft(), 135), SettingsGuiPalette.withAlpha(th.accentSoft(), 155));
        drawFilterDivider(s, s.filterX + pad + half, p, th);
        TextRenderer font = ClickGuiRenderer.getInterRegular();
        float textSize = 7.5f * ClickGuiPickerState.SCALE;
        float allW = ClickGuiRenderer.textWidth(font, "All", textSize); float allH = ClickGuiRenderer.textHeight(font, textSize);
        float selW = ClickGuiRenderer.textWidth(font, "Selected", textSize); float selH = ClickGuiRenderer.textHeight(font, textSize);
        float allCx = s.filterX + half * 0.5f + pad; float selCx = s.filterX + half + half * 0.5f + pad;
        float cy = s.filterY + (ClickGuiPickerState.FILTER_H - allH) * 0.5f;
        int allColor = s.showAll ? p.panelText() : SettingsGuiPalette.mix(p.panelMuted(), p.panelText(), 0.22f * s.allFilterHoverAnim);
        int selColor = !s.showAll ? p.panelText() : SettingsGuiPalette.mix(p.panelMuted(), p.panelText(), 0.22f * s.selectedFilterHoverAnim);
        ClickGuiRenderer.drawText(font, "All", allCx - allW * 0.5f, cy, textSize, allColor, false);
        ClickGuiRenderer.drawText(font, "Selected", selCx - selW * 0.5f, s.filterY + (ClickGuiPickerState.FILTER_H - selH) * 0.5f, textSize, selColor, false);
    }

    static void drawFilterDivider(ClickGuiPickerState s, float x, SettingsGuiPalette p, Themes.Theme th) {
        float backingW = Math.max(1.25f * ClickGuiPickerState.SCALE, 1.05f);
        float backingH = ClickGuiPickerState.FILTER_H - 4.4f * ClickGuiPickerState.SCALE;
        float backingY = s.filterY + (ClickGuiPickerState.FILTER_H - backingH) * 0.5f;
        int backingTop = SettingsGuiPalette.withAlpha(p.panelBgLeft(), 104);
        int backingBottom = SettingsGuiPalette.withAlpha(p.panelBgRight(), 84);
        LayoutRender2D.roundedQuad(x - backingW * 0.5f, backingY, backingW, backingH, backingW * 0.5f, backingTop, backingTop, backingBottom, backingBottom);
        float lineW = Math.max(0.5f * ClickGuiPickerState.SCALE, 0.75f);
        float lineH = ClickGuiPickerState.FILTER_H - 6.0f * ClickGuiPickerState.SCALE;
        float lineY = s.filterY + (ClickGuiPickerState.FILTER_H - lineH) * 0.5f;
        int top = SettingsGuiPalette.withAlpha(SettingsGuiPalette.mix(p.moduleDividerStart(), th.accentSoft(), 0.30f), 154);
        int mid = SettingsGuiPalette.withAlpha(SettingsGuiPalette.mix(p.moduleDividerEnd(), th.accent(), 0.34f), 214);
        int bottom = SettingsGuiPalette.withAlpha(SettingsGuiPalette.mix(p.moduleDividerEnd(), p.panelText(), 0.22f), 142);
        LayoutRender2D.roundedQuad(x - lineW * 0.5f, lineY, lineW, lineH, lineW * 0.5f, top, mid, bottom, top);
    }

    static void drawCardLabel(ClickGuiPickerState s, TextRenderer font, String label, float cardX, float textY, float maxW, float size, int color, boolean hover) {
        String text = label == null ? "" : label;
        float fullW = ClickGuiRenderer.textWidth(font, text, size);
        if (!hover || fullW <= maxW + 0.5f * ClickGuiPickerState.SCALE) {
            String shown = ClickGuiRenderer.fitText(font, text, size, maxW);
            float shownW = ClickGuiRenderer.textWidth(font, shown, size);
            ClickGuiRenderer.drawText(font, shown, cardX + (ClickGuiPickerState.CARD_SIZE - shownW) * 0.5f, textY, size, color, false);
            return;
        }
        float clipX = cardX + (ClickGuiPickerState.CARD_SIZE - maxW) * 0.5f;
        float textH = Math.max(1f, ClickGuiRenderer.textHeight(font, size));
        boolean clipped = ScissorFunction.pushRaw(clipX, textY - ClickGuiPickerState.SCALE, maxW, textH + 2f * ClickGuiPickerState.SCALE);
        float elapsed = Math.max(0f, (Util.getMillis() - s.hoveredCardStartMs) / 1000f);
        float cycleDistance = fullW + ClickGuiPickerState.MARQUEE_GAP;
        float cycleTime = ClickGuiPickerState.MARQUEE_PAUSE_SEC + cycleDistance / Math.max(1f, ClickGuiPickerState.MARQUEE_SPEED);
        float t = elapsed % cycleTime;
        float offset = t <= ClickGuiPickerState.MARQUEE_PAUSE_SEC ? 0f : -(t - ClickGuiPickerState.MARQUEE_PAUSE_SEC) * ClickGuiPickerState.MARQUEE_SPEED;
        ClickGuiRenderer.drawText(font, text, clipX + offset, textY, size, color, false);
        if (cycleDistance > maxW * 0.5f) ClickGuiRenderer.drawText(font, text, clipX + offset + cycleDistance, textY, size, color, false);
        if (clipped) ScissorFunction.pop();
    }
}
