package dev.loki.lovisual.features.gui.clickgui.editor;

import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.text.edit.TextSelection;

/**
 * Caret / selection / scroll math for {@link ClickGuiTextEditorState}. Owns the
 * line-splitting helpers, the pixel-to-caret mapping used by mouse clicks and the
 * viewport adjustment that keeps the caret visible after edits or navigation.
 * Lives in a sibling folder sharing the parent's package so the editor state class
 * stays within the two-hundred-line cap.
 */
final class ClickGuiEditorCaret {

    private final ClickGuiTextEditorState owner;

    ClickGuiEditorCaret(ClickGuiTextEditorState owner) {
        this.owner = owner;
    }

    static boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    static String safeSubstring(String s, int from, int to) {
        int start = Math.max(0, Math.min(s.length(), from));
        int end = Math.max(start, Math.min(s.length(), to));
        return s.substring(start, end);
    }

    /** Strip CR and collapse newlines to spaces so a single-line editor never sees embedded breaks. */
    static String normalizeSingleLine(String text) {
        if (text == null) return "";
        return text.replace("\r\n", "\n").replace('\r', '\n').replace('\n', ' ');
    }

    /** Global character index → {line, column} pair, treating the split by "\n" as the source of truth. */
    static int[] caretLineAndColumn(String[] lines, int caretIndex) {
        int remaining = caretIndex;
        for (int i = 0; i < lines.length; i++) {
            int len = lines[i].length();
            if (remaining <= len) return new int[]{i, remaining};
            remaining -= len;
            if (i < lines.length - 1) remaining -= 1;
        }
        return new int[]{Math.max(0, lines.length - 1), lines.length == 0 ? 0 : lines[lines.length - 1].length()};
    }

    /** Line-major character index at {@code (line, column)}; returns buffer length if past the end. */
    int indexForLineColumn(String[] lines, int line, int column) {
        int idx = 0;
        for (int i = 0; i < lines.length; i++) {
            if (i == line) return idx + column;
            idx += lines[i].length();
            if (i < lines.length - 1) idx++;
        }
        return owner.buffer.length();
    }

    /** Binary-style column search: walk the line, accumulate widths, stop where the caret would land. */
    int columnForX(String line, float relX) {
        if (line == null || line.isEmpty()) return 0;
        TextRenderer tr = ClickGuiRenderer.getIosevkaRegular();
        float size = 15f;
        float acc = 0f;
        for (int i = 0; i < line.length(); i++) {
            float w = ClickGuiRenderer.textWidth(tr, String.valueOf(line.charAt(i)), size);
            if (acc + w * 0.5f >= relX) return i;
            acc += w;
        }
        return line.length();
    }

    /** Pixel coordinates (framebuffer space) → global caret index. */
    int caretFromPoint(float mx, float my) {
        String[] lines = owner.buffer.toString().split("\\n", -1);
        float localY = my - (owner.textY + ClickGuiTextEditorState.TEXT_PADDING) - owner.scroll;
        int lineIdx = clamp((int) Math.floor(localY / ClickGuiTextEditorState.LINE_H), 0, Math.max(0, lines.length - 1));
        int lineStart = 0;
        for (int i = 0; i < lineIdx; i++) {
            lineStart += lines[i].length();
            if (i < lines.length - 1) lineStart++;
        }
        String line = lines.length > 0 ? lines[lineIdx] : "";
        float relX = Math.max(0f, mx - (owner.textX + ClickGuiTextEditorState.TEXT_PADDING) + owner.scrollX);
        return Math.min(owner.buffer.length(), lineStart + columnForX(line, relX));
    }

    /** Extend or clear the selection to reflect a move to {@code target}. */
    void updateSelectionForMove(int target, boolean extend) {
        TextSelection sel = owner.selection;
        if (extend) {
            if (!sel.appliesToLine(0)) sel.update(0, owner.caret);
            sel.updateCaret(target);
        } else {
            sel.clear();
        }
    }

    /** Remove the current selection; returns false when there was nothing selected. */
    boolean deleteSelection() {
        TextSelection sel = owner.selection;
        if (!sel.appliesToLine(0) || !sel.hasRange()) return false;
        int start = clamp(Math.min(sel.start(), sel.end()), 0, owner.buffer.length());
        int end = clamp(Math.max(sel.start(), sel.end()) + 1, 0, owner.buffer.length());
        sel.clear();
        if (end <= start) return false;
        owner.buffer.delete(start, end);
        owner.caret = start;
        return true;
    }

    /** Scroll the viewport so the caret stays inside the visible rectangle (both axes). */
    void ensureCaretVisible() {
        String[] lines = owner.buffer.toString().split("\\n", -1);
        float visibleY = owner.textH - ClickGuiTextEditorState.TEXT_PADDING * 2f;
        float contentY = lines.length * ClickGuiTextEditorState.LINE_H;
        float minScrollY = Math.min(0f, visibleY - contentY);
        int[] caretPos = caretLineAndColumn(lines, owner.caret);
        float caretY = caretPos[0] * ClickGuiTextEditorState.LINE_H + owner.scroll;
        float bottom = caretY + ClickGuiTextEditorState.LINE_H;
        if (caretY < 0f) owner.scroll = clamp(owner.scroll - caretY, minScrollY, 0f);
        else if (bottom > visibleY) owner.scroll = clamp(owner.scroll - (bottom - visibleY), minScrollY, 0f);
        else owner.scroll = clamp(owner.scroll, minScrollY, 0f);

        TextRenderer tr = ClickGuiRenderer.getIosevkaRegular();
        float size = 15f;
        float visibleW = Math.max(1f, owner.textW - ClickGuiTextEditorState.TEXT_PADDING * 2f);
        float caretOffsetX = 0f;
        if (lines.length > 0 && caretPos[0] >= 0 && caretPos[0] < lines.length) {
            caretOffsetX = ClickGuiRenderer.textWidth(tr, safeSubstring(lines[caretPos[0]], 0, caretPos[1]), size);
        }
        float maxWidth = 0f;
        for (String line : lines) {
            float w = ClickGuiRenderer.textWidth(tr, line, size);
            if (w > maxWidth) maxWidth = w;
        }
        float maxScrollX = Math.max(0f, maxWidth - visibleW);
        float caretRelativeX = caretOffsetX - owner.scrollX;
        if (caretRelativeX < 0f) owner.scrollX = clamp(owner.scrollX + caretRelativeX, 0f, maxScrollX);
        else if (caretRelativeX > visibleW) owner.scrollX = clamp(owner.scrollX + (caretRelativeX - visibleW), 0f, maxScrollX);
        else owner.scrollX = clamp(owner.scrollX, 0f, maxScrollX);
    }
}
