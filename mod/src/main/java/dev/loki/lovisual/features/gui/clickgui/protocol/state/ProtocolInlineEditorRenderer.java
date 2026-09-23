package dev.loki.lovisual.features.gui.clickgui.protocol;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;
import dev.loki.lovisual.render.helpers.util.SystemCursor;
import dev.loki.lovisual.util.combat.protocol.heuristics.CombatProtocolHeuristicsConfig;

import java.util.List;

final class ProtocolInlineEditorRenderer {
    private ProtocolInlineEditorRenderer() {}

    static void render(ProtocolInlineEditor e, float x, float y, float w, float h, TextRenderer regular, TextRenderer medium, float mx, float my, SettingsGuiPalette palette, Themes.Theme theme) {
        e.bounds = new CombatProtocolHeuristicsEditorState.Rect(x, y, w, h);
        boolean hovered = e.bounds.contains(mx, my);
        e.focusAnim = CombatProtocolRenderer.animate(e.focusAnim, e.focused, 13f);
        e.hoverAnim = CombatProtocolRenderer.animate(e.hoverAnim, hovered, 13f);
        e.blink += AnimationUtility.deltaTime();
        e.scrollY = AnimationUtility.approach(e.scrollY, e.targetScrollY, 0.28f);
        e.scrollX = AnimationUtility.approach(e.scrollX, e.targetScrollX, 0.28f);
        int baseA = SettingsGuiPalette.mix(palette.moduleCardTop(), palette.panelBgLeft(), 0.30f);
        int baseB = SettingsGuiPalette.mix(palette.moduleCardBottom(), palette.panelBgRight(), 0.28f);
        int bgA = SettingsGuiPalette.mix(baseA, theme.accentSoft(), e.focusAnim * 0.14f + e.hoverAnim * 0.04f);
        int bgB = SettingsGuiPalette.mix(baseB, theme.accent(), e.focusAnim * 0.08f + e.hoverAnim * 0.03f);
        ClickGuiRenderer.drawRoundedRectGradient(x, y, w, h, 5.5f * CombatProtocolHeuristicsEditorState.SCALE, bgA, bgB, 85f);
        ClickGuiRenderer.drawRoundedRectStrokeGradient(x, y, w, h, 5.5f * CombatProtocolHeuristicsEditorState.SCALE, (0.45f + e.focusAnim * 0.25f) * CombatProtocolHeuristicsEditorState.SCALE, SettingsGuiPalette.mix(palette.panelStroke(), theme.accentSoft(), 0.18f + e.focusAnim * 0.66f), SettingsGuiPalette.mix(palette.panelStroke(), theme.accent(), 0.10f + e.focusAnim * 0.54f), 0f);
        if (e.focused) ClickGuiRenderer.drawRoundedRectGlow(x, y, w, h, 5.5f * CombatProtocolHeuristicsEditorState.SCALE, 6f * CombatProtocolHeuristicsEditorState.SCALE, SettingsGuiPalette.withAlpha(theme.accentSoft(), Math.round(25f + 25f * e.focusAnim)));
        boolean legacy = e.family == CombatProtocolHeuristicsConfig.ProtocolFamily.LEGACY;
        String name = legacy ? CombatProtocolRenderer.tr("legacy", "Legacy (1.8)") : CombatProtocolRenderer.tr("modern", "Modern (1.9+)");
        int invalid = invalidCount(List.of(e.lines()));
        String meta = CombatProtocolRenderer.tr("pattern_count", "%s patterns", e.patternCount());
        if (invalid > 0) meta += " \u00b7 " + CombatProtocolRenderer.tr("invalid_count", "%s invalid", invalid);
        ClickGuiRenderer.drawText(medium, name, x + 7f * CombatProtocolHeuristicsEditorState.SCALE, y + 5.5f * CombatProtocolHeuristicsEditorState.SCALE, 6.7f * CombatProtocolHeuristicsEditorState.SCALE, palette.moduleTitleText(), false);
        float metaSize = 5.3f * CombatProtocolHeuristicsEditorState.SCALE;
        float metaW = ClickGuiRenderer.textWidth(regular, meta, metaSize);
        ClickGuiRenderer.drawText(regular, meta, x + w - 7f * CombatProtocolHeuristicsEditorState.SCALE - metaW, y + 7f * CombatProtocolHeuristicsEditorState.SCALE, metaSize, invalid > 0 ? 0xFFFF8A80 : palette.panelMuted(), false);
        float areaY = y + 19f * CombatProtocolHeuristicsEditorState.SCALE;
        e.textArea = new CombatProtocolHeuristicsEditorState.Rect(x + 5f * CombatProtocolHeuristicsEditorState.SCALE, areaY, w - 10f * CombatProtocolHeuristicsEditorState.SCALE, h - 24f * CombatProtocolHeuristicsEditorState.SCALE);
        int areaBg = SettingsGuiPalette.withAlpha(SettingsGuiPalette.darken(palette.panelBgRight(), 0.18f), 190);
        ClickGuiRenderer.drawRoundedRect(e.textArea.x(), e.textArea.y(), e.textArea.w(), e.textArea.h(), 3.5f * CombatProtocolHeuristicsEditorState.SCALE, areaBg);
        ClickGuiRenderer.drawRoundedRectStroke(e.textArea.x(), e.textArea.y(), e.textArea.w(), e.textArea.h(), 3.5f * CombatProtocolHeuristicsEditorState.SCALE, 0.35f * CombatProtocolHeuristicsEditorState.SCALE, SettingsGuiPalette.mix(palette.panelStroke(), theme.accentSoft(), e.focusAnim * 0.42f));
        renderText(e, regular, palette, theme);
        if (hovered) SystemCursor.set(SystemCursor.CursorType.TEXT);
    }

    private static void renderText(ProtocolInlineEditor e, TextRenderer font, SettingsGuiPalette palette, Themes.Theme theme) {
        String[] lines = e.lines();
        float viewX = e.textArea.x() + ProtocolInlineEditor.GUTTER_W + ProtocolInlineEditor.TEXT_PAD - e.scrollX;
        float viewY = e.textArea.y() + 3f * CombatProtocolHeuristicsEditorState.SCALE - e.scrollY;
        boolean clipped = ScissorFunction.pushRaw(e.textArea.x(), e.textArea.y(), e.textArea.w(), e.textArea.h());
        int globalOffset = 0;
        int caretLine = 0;
        int caretCol = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            float lineY = viewY + i * ProtocolInlineEditor.LINE_H;
            int lineStart = globalOffset;
            int lineEnd = lineStart + line.length();
            if (e.caret >= lineStart && e.caret <= lineEnd) { caretLine = i; caretCol = e.caret - lineStart; }
            if (lineY + ProtocolInlineEditor.LINE_H >= e.textArea.y() && lineY <= e.textArea.y() + e.textArea.h()) {
                if (e.focused && e.selection.hasRange() && e.selection.appliesToLine(0)) {
                    int selStart = Math.min(e.selection.anchor(), e.selection.caret());
                    int selEnd = Math.max(e.selection.anchor(), e.selection.caret());
                    int a = Math.max(lineStart, selStart);
                    int b = Math.min(lineEnd, selEnd);
                    if (b > a) {
                        float x0 = viewX + ClickGuiRenderer.textWidth(font, line.substring(0, a - lineStart), ProtocolInlineEditor.FONT_SIZE);
                        float x1 = viewX + ClickGuiRenderer.textWidth(font, line.substring(0, b - lineStart), ProtocolInlineEditor.FONT_SIZE);
                        ClickGuiRenderer.drawRoundedRect(x0, lineY, Math.max(1f, x1 - x0), ProtocolInlineEditor.LINE_H, 2f * CombatProtocolHeuristicsEditorState.SCALE, SettingsGuiPalette.withAlpha(theme.accentSoft(), 155));
                    }
                }
                String lineNumber = Integer.toString(i + 1);
                float numberW = ClickGuiRenderer.textWidth(font, lineNumber, 5.1f * CombatProtocolHeuristicsEditorState.SCALE);
                ClickGuiRenderer.drawText(font, lineNumber, e.textArea.x() + ProtocolInlineEditor.GUTTER_W - 4f * CombatProtocolHeuristicsEditorState.SCALE - numberW, lineY + 0.5f * CombatProtocolHeuristicsEditorState.SCALE, 5.1f * CombatProtocolHeuristicsEditorState.SCALE, palette.panelMuted(), false);
                int textColor = line.isBlank() || CombatProtocolHeuristicsEditorState.validPattern(line) ? palette.moduleDescriptionText() : 0xFFFF8A80;
                ClickGuiRenderer.drawText(font, line, viewX, lineY + 0.2f * CombatProtocolHeuristicsEditorState.SCALE, ProtocolInlineEditor.FONT_SIZE, textColor, false);
            }
            globalOffset = lineEnd + 1;
        }
        ClickGuiRenderer.drawRect(e.textArea.x() + ProtocolInlineEditor.GUTTER_W, e.textArea.y() + 2f * CombatProtocolHeuristicsEditorState.SCALE, 0.4f * CombatProtocolHeuristicsEditorState.SCALE, e.textArea.h() - 4f * CombatProtocolHeuristicsEditorState.SCALE, palette.panelDivider());
        if (e.focused && ((int) (e.blink * 2f) & 1) == 0) {
            String line = lines[Math.max(0, Math.min(caretLine, lines.length - 1))];
            float caretX = viewX + ClickGuiRenderer.textWidth(font, line.substring(0, Math.min(caretCol, line.length())), ProtocolInlineEditor.FONT_SIZE);
            float caretY = viewY + caretLine * ProtocolInlineEditor.LINE_H;
            ClickGuiRenderer.drawRoundedRect(caretX, caretY, 0.65f * CombatProtocolHeuristicsEditorState.SCALE, ProtocolInlineEditor.LINE_H, 0.3f * CombatProtocolHeuristicsEditorState.SCALE, theme.accent());
        }
        if (clipped) ScissorFunction.pop();
    }

    private static int invalidCount(Iterable<String> patterns) {
        int invalid = 0;
        if (patterns == null) return invalid;
        for (String pattern : patterns) if (pattern != null && !pattern.isBlank() && !CombatProtocolHeuristicsEditorState.validPattern(pattern)) invalid++;
        return invalid;
    }

    static void ensureVisible(ProtocolInlineEditor e) {
        String[] lines = e.lines();
        CombatProtocolHeuristicsEditorState.LineColumn pos = CombatProtocolHeuristicsEditorState.lineColumn(e.caret, lines);
        float caretY = pos.line() * ProtocolInlineEditor.LINE_H;
        float visibleH = Math.max(ProtocolInlineEditor.LINE_H, e.textArea.h() - 6f * CombatProtocolHeuristicsEditorState.SCALE);
        if (caretY < e.targetScrollY) e.targetScrollY = caretY;
        if (caretY + ProtocolInlineEditor.LINE_H > e.targetScrollY + visibleH) e.targetScrollY = caretY + ProtocolInlineEditor.LINE_H - visibleH;
        dev.loki.lovisual.render.engine.text.render.TextRenderer font = ClickGuiRenderer.getInterRegular();
        String line = lines[pos.line()];
        float caretX = ClickGuiRenderer.textWidth(font, line.substring(0, Math.min(pos.column(), line.length())), ProtocolInlineEditor.FONT_SIZE);
        float visibleW = Math.max(20f * CombatProtocolHeuristicsEditorState.SCALE, e.textArea.w() - ProtocolInlineEditor.GUTTER_W - ProtocolInlineEditor.TEXT_PAD * 2f);
        if (caretX < e.targetScrollX) e.targetScrollX = caretX;
        if (caretX + 2f * CombatProtocolHeuristicsEditorState.SCALE > e.targetScrollX + visibleW) e.targetScrollX = caretX + 2f * CombatProtocolHeuristicsEditorState.SCALE - visibleW;
        clamp(e);
    }

    static void clamp(ProtocolInlineEditor e) {
        String[] lines = e.lines();
        float maxY = Math.max(0f, lines.length * ProtocolInlineEditor.LINE_H - Math.max(ProtocolInlineEditor.LINE_H, e.textArea.h() - 6f * CombatProtocolHeuristicsEditorState.SCALE));
        float maxLineW = 0f;
        dev.loki.lovisual.render.engine.text.render.TextRenderer font = ClickGuiRenderer.getInterRegular();
        for (String l : lines) maxLineW = Math.max(maxLineW, ClickGuiRenderer.textWidth(font, l, ProtocolInlineEditor.FONT_SIZE));
        float maxX = Math.max(0f, maxLineW - Math.max(20f * CombatProtocolHeuristicsEditorState.SCALE, e.textArea.w() - ProtocolInlineEditor.GUTTER_W - ProtocolInlineEditor.TEXT_PAD * 2f));
        e.targetScrollY = CombatProtocolHeuristicsEditorState.clamp(e.targetScrollY, 0f, maxY);
        e.targetScrollX = CombatProtocolHeuristicsEditorState.clamp(e.targetScrollX, 0f, maxX);
    }

    static void insertText(ProtocolInlineEditor e, String text) {
        if (text == null || text.isEmpty()) return;
        e.deleteSelection();
        String n = text.replace("\r\n", "\n").replace('\r', '\n');
        e.buffer.insert(e.caret, n);
        e.caret += n.length();
        e.selection.clear();
        e.preferredColumn = -1;
        e.blink = 0f;
        e.changed();
    }

    static void backspace(ProtocolInlineEditor e) {
        if (e.deleteSelection()) { e.changed(); return; }
        if (e.caret <= 0) return;
        e.buffer.deleteCharAt(e.caret - 1);
        e.caret--;
        e.selection.clear();
        e.preferredColumn = -1;
        e.changed();
    }

    static void deleteForward(ProtocolInlineEditor e) {
        if (e.deleteSelection()) { e.changed(); return; }
        if (e.caret >= e.buffer.length()) return;
        e.buffer.deleteCharAt(e.caret);
        e.selection.clear();
        e.preferredColumn = -1;
        e.changed();
    }

    static void moveHorizontal(ProtocolInlineEditor e, int delta, boolean extend) {
        int target;
        if (!extend && e.selection.hasRange()) target = delta < 0 ? Math.min(e.selection.anchor(), e.selection.caret()) : Math.max(e.selection.anchor(), e.selection.caret());
        else target = CombatProtocolHeuristicsEditorState.clamp(e.caret + delta, 0, e.buffer.length());
        updateSelection(e, target, extend);
        e.caret = target; e.preferredColumn = -1; e.blink = 0f; e.ensureCaretVisible();
    }

    static void moveVertical(ProtocolInlineEditor e, int delta, boolean extend) {
        String[] lines = e.lines(); CombatProtocolHeuristicsEditorState.LineColumn cur = CombatProtocolHeuristicsEditorState.lineColumn(e.caret, lines);
        if (e.preferredColumn < 0) e.preferredColumn = cur.column();
        int tl = CombatProtocolHeuristicsEditorState.clamp(cur.line() + delta, 0, lines.length - 1);
        int target = CombatProtocolHeuristicsEditorState.indexForLineColumn(lines, tl, Math.min(e.preferredColumn, lines[tl].length()));
        updateSelection(e, target, extend); e.caret = target; e.blink = 0f; e.ensureCaretVisible();
    }

    static void moveLineBoundary(ProtocolInlineEditor e, boolean end, boolean extend) {
        String[] lines = e.lines(); CombatProtocolHeuristicsEditorState.LineColumn cur = CombatProtocolHeuristicsEditorState.lineColumn(e.caret, lines);
        int target = CombatProtocolHeuristicsEditorState.indexForLineColumn(lines, cur.line(), end ? lines[cur.line()].length() : 0);
        updateSelection(e, target, extend); e.caret = target; e.preferredColumn = -1; e.blink = 0f; e.ensureCaretVisible();
    }

    static void updateSelection(ProtocolInlineEditor e, int target, boolean extend) {
        if (extend) { if (!e.selection.hasCaret()) e.selection.begin(0, e.caret); e.selection.updateCaret(0, target); } else e.selection.clear();
    }

    static void selectAll(ProtocolInlineEditor e) { e.selection.begin(0, 0); e.selection.updateCaret(0, e.buffer.length()); e.caret = e.buffer.length(); e.preferredColumn = -1; e.blink = 0f; e.ensureCaretVisible(); }

    static void copy(ProtocolInlineEditor e) {
        if (e.selection.hasRange()) { int s = CombatProtocolHeuristicsEditorState.clamp(Math.min(e.selection.anchor(), e.selection.caret()), 0, e.buffer.length()); int en = CombatProtocolHeuristicsEditorState.clamp(Math.max(e.selection.anchor(), e.selection.caret()), 0, e.buffer.length()); if (en > s) dev.loki.lovisual.util.text.edit.ClipboardUtil.copy(e.buffer.substring(s, en)); } else if (!e.buffer.isEmpty()) dev.loki.lovisual.util.text.edit.ClipboardUtil.copy(e.buffer.toString());
    }

    static void cut(ProtocolInlineEditor e) { if (!e.selection.hasRange()) return; copy(e); if (e.deleteSelection()) e.changed(); }
}
