/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.picker;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.picker.catalog.PickerEntryData;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.text.font.FontFace;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.faces.FontFamily;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.glyph.CustomTextRenderer;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;
import dev.loki.lovisual.render.helpers.util.SystemCursor;
import java.util.List;
import java.util.Set;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

final class ClickGuiPickerGridRenderer {
    private ClickGuiPickerGridRenderer() {}

    static void drawGrid(ClickGuiPickerState s, float mx, float my, SettingsGuiPalette p, Themes.Theme th) {
        List<PickerEntryData> visible = ClickGuiPickerLogic.visibleEntries(s);
        Set<String> selected = s.owner.getValueSet();
        int columns = ClickGuiPickerLogic.columns(s);
        s.cardHits.clear();
        ClickGuiRenderer.beginPickerIconScissor(s.gridX, s.gridY, s.gridW, s.gridH);
        boolean clipped = ScissorFunction.pushRaw(s.gridX, s.gridY, s.gridW, s.gridH);
        float step = ClickGuiPickerState.CARD_SIZE + ClickGuiPickerState.CARD_GAP;
        float iconScale = 1.55f * ClickGuiPickerState.SCALE;
        float iconPx = 16f * iconScale;
        TextRenderer font = ClickGuiRenderer.getInterRegular();
        float nameSize = 5.9f * ClickGuiPickerState.SCALE;
        boolean hasHovered = false;
        for (int i = 0; i < visible.size(); i++) {
            int col = i % columns; int row = i / columns;
            float cardX = s.gridX + col * step; float cardY = s.gridY + row * step + s.smoothScroll;
            if (cardY + ClickGuiPickerState.CARD_SIZE < s.gridY - 0.5f * ClickGuiPickerState.SCALE) continue;
            if (cardY > s.gridY + s.gridH + 0.5f * ClickGuiPickerState.SCALE) continue;
            PickerEntryData entry = visible.get(i);
            boolean isSelected = selected.contains(entry.getId());
            boolean hover = ClickGuiPickerState.inside(mx, my, cardX, cardY, ClickGuiPickerState.CARD_SIZE, ClickGuiPickerState.CARD_SIZE);
            if (hover) { hasHovered = true; if (!entry.getId().equals(s.hoveredCardId)) { s.hoveredCardId = entry.getId(); s.hoveredCardStartMs = Util.getMillis(); } }
            int left = SettingsGuiPalette.withAlpha(SettingsGuiPalette.mix(th.windowBg(), th.surface(), 0.42f), 205);
            int right = SettingsGuiPalette.withAlpha(SettingsGuiPalette.darken(SettingsGuiPalette.mix(th.windowHeader(), th.windowBg(), 0.55f), 0.26f), 205);
            if (isSelected) { left = ClickGuiRenderer.mixColor(left, th.accentSoft(), 0.14f); right = ClickGuiRenderer.mixColor(right, th.accentSoft(), 0.26f); }
            if (hover) { left = ClickGuiRenderer.mixColor(left, p.moduleCardTopStrong(), 0.40f); right = ClickGuiRenderer.mixColor(right, p.moduleCardBottomStrong(), 0.42f); }
            LayoutRender2D.roundedQuad(cardX, cardY, ClickGuiPickerState.CARD_SIZE, ClickGuiPickerState.CARD_SIZE, 4f * ClickGuiPickerState.SCALE, left, right, right, left);
            int stroke = isSelected ? th.accentSoft() : p.moduleDividerStart();
            float strokeW = isSelected ? 0.8f * ClickGuiPickerState.SCALE : 0.45f * ClickGuiPickerState.SCALE;
            LayoutRender2D.roundedStroke(cardX, cardY, ClickGuiPickerState.CARD_SIZE, ClickGuiPickerState.CARD_SIZE, 4f * ClickGuiPickerState.SCALE, strokeW, stroke);
            float lineY = cardY + ClickGuiPickerState.CARD_SIZE - 11f * ClickGuiPickerState.SCALE;
            LayoutRender2D.rectQuad(cardX + 3f * ClickGuiPickerState.SCALE, lineY, ClickGuiPickerState.CARD_SIZE - 6f * ClickGuiPickerState.SCALE, 0.45f * ClickGuiPickerState.SCALE, p.moduleDividerStart(), p.moduleDividerEnd(), p.moduleDividerEnd(), p.moduleDividerStart());
            ItemStack stack = entry.stack();
            if (stack != null && !stack.isEmpty()) {
                float iconX = cardX + (ClickGuiPickerState.CARD_SIZE - iconPx) * 0.5f; float iconY = cardY + 4f * ClickGuiPickerState.SCALE;
                float clipX = cardX + ClickGuiPickerState.SCALE; float clipY = cardY + ClickGuiPickerState.SCALE;
                float clipW = ClickGuiPickerState.CARD_SIZE - 2f * ClickGuiPickerState.SCALE;
                float clipH = Math.max(1f, lineY - clipY - 0.75f * ClickGuiPickerState.SCALE);
                ClickGuiRenderer.queuePickerIcon(stack, iconX, iconY, iconScale, clipX, clipY, clipW, clipH);
            } else {
                TextRenderer bold = interBold(s);
                float qSize = 13f * ClickGuiPickerState.SCALE; String q = "?";
                float qW = ClickGuiRenderer.textWidth(bold, q, qSize); float qH = ClickGuiRenderer.textHeight(bold, qSize);
                float iconAreaTop = cardY + 3.5f * ClickGuiPickerState.SCALE; float iconAreaBottom = lineY - ClickGuiPickerState.SCALE;
                float qX = cardX + (ClickGuiPickerState.CARD_SIZE - qW) * 0.5f; float qY = iconAreaTop + ((iconAreaBottom - iconAreaTop) - qH) * 0.5f;
                int qColor = isSelected ? SettingsGuiPalette.withAlpha(p.moduleTitleText(), 210) : SettingsGuiPalette.withAlpha(p.panelMuted(), 190);
                ClickGuiRenderer.drawText(bold, q, qX, qY, qSize, qColor, false);
            }
            float textMaxW = ClickGuiPickerState.CARD_SIZE - 6f * ClickGuiPickerState.SCALE;
            float textY = lineY + 1.5f * ClickGuiPickerState.SCALE;
            int textColor = isSelected ? p.moduleTitleText() : p.moduleDescriptionText();
            ClickGuiPickerFilterRenderer.drawCardLabel(s, font, entry.label(), cardX, textY, textMaxW, nameSize, textColor, hover);
            s.cardHits.add(new ClickGuiPickerState.CardHit(entry.getId(), cardX, cardY, ClickGuiPickerState.CARD_SIZE, ClickGuiPickerState.CARD_SIZE));
        }
        if (!hasHovered) { s.hoveredCardId = null; s.hoveredCardStartMs = 0L; }
        if (clipped) ScissorFunction.pop();
        ClickGuiRenderer.endPickerIconScissor();
        if (visible.isEmpty()) {
            ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), s.showAll ? "No entries" : "Nothing selected", s.gridX + 4f * ClickGuiPickerState.SCALE, s.gridY + 4f * ClickGuiPickerState.SCALE, 7.2f * ClickGuiPickerState.SCALE, p.panelMuted(), false);
        }
    }

    static void drawScrollbar(ClickGuiPickerState s, float mx, float my, SettingsGuiPalette p) {
        if (!s.scrollbarVisible || s.contentHeight <= s.gridH + 0.5f) { s.scrollbarVisible = false; return; }
        float baseW = 2.25f * ClickGuiPickerState.SCALE;
        boolean hover = ClickGuiPickerLogic.isScrollbarHovered(s, mx, my) || s.draggingScrollbar;
        if (hover) SystemCursor.set(SystemCursor.CursorType.SCROLL);
        float trackW = hover ? 3.2f * ClickGuiPickerState.SCALE : baseW;
        float trackX = s.gridX + s.gridW - trackW - ClickGuiPickerState.SCALE;
        float trackY = s.gridY + ClickGuiPickerState.SCALE;
        float trackH = Math.max(12f * ClickGuiPickerState.SCALE, s.gridH - 2f * ClickGuiPickerState.SCALE);
        s.sbTrackX = trackX; s.sbTrackY = trackY; s.sbTrackW = trackW; s.sbTrackH = trackH;
        LayoutRender2D.roundedQuad(trackX, trackY, trackW, trackH, 2f * ClickGuiPickerState.SCALE, p.moduleScrollTrackA(), p.moduleScrollTrackB(), p.moduleScrollTrackB(), p.moduleScrollTrackA());
        float thumbH = Math.max(16f * ClickGuiPickerState.SCALE, trackH * (s.gridH / s.contentHeight));
        s.sbThumbH = thumbH;
        float ratio = s.contentHeight <= s.gridH ? 0f : (-s.smoothScroll / (s.contentHeight - s.gridH));
        float thumbY = trackY + (trackH - thumbH) * ClickGuiPickerLogic.clamp(ratio, 0f, 1f);
        LayoutRender2D.roundedQuad(trackX, thumbY, trackW, thumbH, 2f * ClickGuiPickerState.SCALE, p.moduleScrollHandleA(), p.moduleScrollHandleB(), p.moduleScrollHandleB(), p.moduleScrollHandleA());
    }

    private static TextRenderer interBold(ClickGuiPickerState s) {
        if (s.interBold != null) return s.interBold;
        FontFamily family = Fonts.getFamily("Inter");
        if (family == null) { s.interBold = ClickGuiRenderer.getInterRegular(); return s.interBold; }
        FontFace face = family.get(FontInfo.Type.Bold);
        if (face == null) { s.interBold = ClickGuiRenderer.getInterRegular(); return s.interBold; }
        s.interBold = new CustomTextRenderer(face);
        return s.interBold;
    }
}
