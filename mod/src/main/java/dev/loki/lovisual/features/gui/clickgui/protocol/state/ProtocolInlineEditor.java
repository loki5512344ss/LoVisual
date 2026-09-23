package dev.loki.lovisual.features.gui.clickgui.protocol;

import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.combat.protocol.heuristics.CombatProtocolHeuristicSource;
import dev.loki.lovisual.util.combat.protocol.heuristics.CombatProtocolHeuristicsConfig;
import dev.loki.lovisual.util.text.edit.TextSelection;
import org.lwjgl.glfw.GLFW;
import java.util.LinkedHashSet;
import java.util.Set;

final class ProtocolInlineEditor {
    static final float LINE_H = 8f * CombatProtocolHeuristicsEditorState.SCALE;
    static final float FONT_SIZE = 5.9f * CombatProtocolHeuristicsEditorState.SCALE;
    static final float GUTTER_W = 19f * CombatProtocolHeuristicsEditorState.SCALE;
    static final float TEXT_PAD = 5f * CombatProtocolHeuristicsEditorState.SCALE;
    final CombatProtocolHeuristicSource source;
    final CombatProtocolHeuristicsConfig.ProtocolFamily family;
    final StringBuilder buffer = new StringBuilder();
    final TextSelection selection = new TextSelection();
    CombatProtocolHeuristicsEditorState.Rect bounds = CombatProtocolHeuristicsEditorState.Rect.EMPTY;
    CombatProtocolHeuristicsEditorState.Rect textArea = CombatProtocolHeuristicsEditorState.Rect.EMPTY;
    int caret;
    int preferredColumn = -1;
    float scrollY, targetScrollY, scrollX, targetScrollX, focusAnim, hoverAnim, blink;
    boolean focused, dragging;
    ProtocolInlineEditor(CombatProtocolHeuristicSource source, CombatProtocolHeuristicsConfig.ProtocolFamily family, Set<String> patterns) { this.source = source; this.family = family; replace(patterns); }
    void replace(Set<String> patterns) { buffer.setLength(0); if (patterns != null && !patterns.isEmpty()) buffer.append(String.join("\n", patterns)); caret = Math.min(caret, buffer.length()); selection.clear(); targetScrollX = targetScrollY = scrollX = scrollY = 0f; }
    void setFocused(boolean v) { focused = v; blink = 0f; if (!v) { dragging = false; selection.clear(); } }
    int patternCount() { return parsedPatterns().size(); }
    void render(float x, float y, float w, float h, TextRenderer regular, TextRenderer medium, float mx, float my, dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette p, dev.loki.lovisual.features.theme.Themes.Theme t) { ProtocolInlineEditorRenderer.render(this, x, y, w, h, regular, medium, mx, my, p, t); }
    boolean mousePressed(float mx, float my, int button) { if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !textArea.contains(mx, my)) return false; int target = caretFromPoint(mx, my); boolean shift = CombatProtocolHeuristicsEditorState.shiftDown(); if (shift) { if (!selection.hasCaret()) selection.begin(0, caret); selection.updateCaret(0, target); } else { selection.clear(); selection.begin(0, target); } caret = target; preferredColumn = -1; dragging = true; blink = 0f; ensureCaretVisible(); return true; }
    void mouseMoved(float mx, float my) { if (!focused || !dragging) return; int target = caretFromPoint(mx, my); if (!selection.hasCaret()) selection.begin(0, caret); selection.updateCaret(0, target); caret = target; preferredColumn = -1; blink = 0f; ensureCaretVisible(); }
    void mouseReleased(int button) { if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) dragging = false; }
    boolean scroll(float mx, float my, double delta) { if (!bounds.contains(mx, my)) return false; targetScrollY -= (float) delta * LINE_H * 2f; clampScroll(); return true; }
    boolean charTyped(char chr) { if (!focused || Character.isISOControl(chr)) return false; ProtocolInlineEditorRenderer.insertText(this, String.valueOf(chr)); return true; }
    boolean keyPressed(int key, int modifiers) { if (!focused) return false; boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0; boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0; if (ctrl) { switch (key) { case GLFW.GLFW_KEY_A -> ProtocolInlineEditorRenderer.selectAll(this); case GLFW.GLFW_KEY_C -> ProtocolInlineEditorRenderer.copy(this); case GLFW.GLFW_KEY_X -> ProtocolInlineEditorRenderer.cut(this); case GLFW.GLFW_KEY_V -> ProtocolInlineEditorRenderer.insertText(this, dev.loki.lovisual.util.text.edit.ClipboardUtil.get()); default -> { return true; } } return true; } switch (key) { case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> ProtocolInlineEditorRenderer.insertText(this, "\n"); case GLFW.GLFW_KEY_BACKSPACE -> ProtocolInlineEditorRenderer.backspace(this); case GLFW.GLFW_KEY_DELETE -> ProtocolInlineEditorRenderer.deleteForward(this); case GLFW.GLFW_KEY_LEFT -> ProtocolInlineEditorRenderer.moveHorizontal(this, -1, shift); case GLFW.GLFW_KEY_RIGHT -> ProtocolInlineEditorRenderer.moveHorizontal(this, 1, shift); case GLFW.GLFW_KEY_UP -> ProtocolInlineEditorRenderer.moveVertical(this, -1, shift); case GLFW.GLFW_KEY_DOWN -> ProtocolInlineEditorRenderer.moveVertical(this, 1, shift); case GLFW.GLFW_KEY_HOME -> ProtocolInlineEditorRenderer.moveLineBoundary(this, false, shift); case GLFW.GLFW_KEY_END -> ProtocolInlineEditorRenderer.moveLineBoundary(this, true, shift); default -> { return true; } } return true; }
    Set<String> parsedPatterns() { LinkedHashSet<String> out = new LinkedHashSet<>(); for (String line : lines()) { String clean = line.trim(); if (!clean.isEmpty()) out.add(clean); } return out; }
    int caretFromPoint(float mx, float my) { String[] lines = lines(); int line = CombatProtocolHeuristicsEditorState.clamp((int) Math.floor((my - textArea.y() - 3f * CombatProtocolHeuristicsEditorState.SCALE + scrollY) / LINE_H), 0, lines.length - 1); String text = lines[line]; float localX = mx - (textArea.x() + GUTTER_W + TEXT_PAD) + scrollX; int column = 0; TextRenderer font = ClickGuiRenderer.getInterRegular(); for (int i = 1; i <= text.length(); i++) { float prev = ClickGuiRenderer.textWidth(font, text.substring(0, i - 1), FONT_SIZE); float next = ClickGuiRenderer.textWidth(font, text.substring(0, i), FONT_SIZE); if (localX < (prev + next) * 0.5f) break; column = i; } return CombatProtocolHeuristicsEditorState.indexForLineColumn(lines, line, column); }
    void ensureCaretVisible() { ProtocolInlineEditorRenderer.ensureVisible(this); }
    void clampScroll() { ProtocolInlineEditorRenderer.clamp(this); }
    String[] lines() { return buffer.toString().split("\n", -1); }
    void changed() { CombatProtocolHeuristicsConfig.get().setPatterns(source, family, parsedPatterns()); clampScroll(); ensureCaretVisible(); }
    boolean deleteSelection() { if (!selection.hasRange()) return false; int s = CombatProtocolHeuristicsEditorState.clamp(Math.min(selection.anchor(), selection.caret()), 0, buffer.length()); int e = CombatProtocolHeuristicsEditorState.clamp(Math.max(selection.anchor(), selection.caret()), 0, buffer.length()); if (e <= s) return false; buffer.delete(s, e); caret = s; selection.clear(); return true; }
}
