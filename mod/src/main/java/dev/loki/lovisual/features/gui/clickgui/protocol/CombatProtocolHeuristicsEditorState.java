package dev.loki.lovisual.features.gui.clickgui.protocol;

import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.settings.cooldown.ProtocolHeuristicsSetting;
import dev.loki.lovisual.util.combat.protocol.heuristics.CombatProtocolHeuristicSource;
import dev.loki.lovisual.util.combat.protocol.heuristics.CombatProtocolHeuristicsConfig;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class CombatProtocolHeuristicsEditorState {
    static final float SCALE = 2f;
    static final float WINDOW_W = 420f * SCALE;
    static final float WINDOW_H = 280f * SCALE;
    static final float PADDING = 9f * SCALE;
    static final float HEADER_H = 31f * SCALE;
    static final float SOURCE_W = 108f * SCALE;
    static final float GAP = 7f * SCALE;
    static final float CLOSE_SIZE = 12f * SCALE;

    final ProtocolHeuristicsSetting owner;
    final String title;
    final List<SourceHit> sourceHits = new ArrayList<>();
    final EnumMap<CombatProtocolHeuristicSource, Float> sourceHover = new EnumMap<>(CombatProtocolHeuristicSource.class);
    final EnumMap<CombatProtocolHeuristicSource, Float> sourceSelect = new EnumMap<>(CombatProtocolHeuristicSource.class);
    final EnumMap<CombatProtocolHeuristicSource, EnumMap<CombatProtocolHeuristicsConfig.ProtocolFamily, ProtocolInlineEditor>> editors = new EnumMap<>(CombatProtocolHeuristicSource.class);

    CombatProtocolHeuristicSource selected = CombatProtocolHeuristicSource.BOSSBAR;
    ProtocolInlineEditor focusedEditor;
    float windowX, windowY, openAnim;
    Rect close = Rect.EMPTY;
    Rect reset = Rect.EMPTY;
    float closeHover, resetHover;

    public CombatProtocolHeuristicsEditorState(ProtocolHeuristicsSetting owner, String title) {
        this.owner = owner;
        this.title = title == null || title.isBlank() ? tr("title", "Protocol heuristics") : title;
        CombatProtocolHeuristicsConfig config = CombatProtocolHeuristicsConfig.get();
        for (CombatProtocolHeuristicSource source : CombatProtocolHeuristicSource.values()) {
            EnumMap<CombatProtocolHeuristicsConfig.ProtocolFamily, ProtocolInlineEditor> byFamily = new EnumMap<>(CombatProtocolHeuristicsConfig.ProtocolFamily.class);
            for (CombatProtocolHeuristicsConfig.ProtocolFamily family : CombatProtocolHeuristicsConfig.ProtocolFamily.values()) byFamily.put(family, new ProtocolInlineEditor(source, family, config.patterns(source, family)));
            editors.put(source, byFamily);
            sourceHover.put(source, 0f);
            sourceSelect.put(source, source == selected ? 1f : 0f);
        }
    }

    public void handleMouseMove(float mx, float my) { CombatProtocolInputHandler.handleMouseMove(this, mx, my); }
    public void handleMouseDown(float mx, float my, int button) { CombatProtocolInputHandler.handleMouseDown(this, mx, my, button); }
    public void handleMouseUp(float mx, float my, int button) { CombatProtocolInputHandler.handleMouseUp(this, mx, my, button); }
    public void scroll(double delta) { CombatProtocolInputHandler.handleScroll(this, delta); }
    public boolean charTyped(char chr) { return CombatProtocolInputHandler.charTyped(this, chr); }
    public boolean keyPressed(int key, int scancode, int action, int modifiers) { return CombatProtocolInputHandler.keyPressed(this, key, scancode, action, modifiers); }
    public void render(int width, int height) { CombatProtocolRenderer.render(this, width, height); }

    void focus(ProtocolInlineEditor editor) { if (focusedEditor == editor) return; if (focusedEditor != null) focusedEditor.setFocused(false); focusedEditor = editor; if (focusedEditor != null) focusedEditor.setFocused(true); }
    void blurEditor() { if (focusedEditor != null) focusedEditor.setFocused(false); focusedEditor = null; }
    void reloadEditors(CombatProtocolHeuristicSource source) { CombatProtocolHeuristicsConfig config = CombatProtocolHeuristicsConfig.get(); for (CombatProtocolHeuristicsConfig.ProtocolFamily family : CombatProtocolHeuristicsConfig.ProtocolFamily.values()) editor(source, family).replace(config.patterns(source, family)); }
    ProtocolInlineEditor editor(CombatProtocolHeuristicSource s, CombatProtocolHeuristicsConfig.ProtocolFamily f) { return editors.get(s).get(f); }
    boolean sourceEnabled(CombatProtocolHeuristicSource s) { return owner.sources() != null && owner.sources().get(s.key()); }

    static boolean inside(float mx, float my, float x, float y, float w, float h) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }
    static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    static boolean validPattern(String p) { if (p == null || p.isBlank()) return false; try { Pattern.compile(p, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE); return true; } catch (PatternSyntaxException ignored) { return false; } }
    static LineColumn lineColumn(int index, String[] lines) { int rem = clamp(index, 0, totalLength(lines)); for (int l = 0; l < lines.length; l++) { int len = lines[l].length(); if (rem <= len) return new LineColumn(l, rem); rem -= len + 1; } int last = Math.max(0, lines.length - 1); return new LineColumn(last, lines[last].length()); }
    static int indexForLineColumn(String[] lines, int line, int column) { int idx = 0; int rl = clamp(line, 0, lines.length - 1); for (int i = 0; i < rl; i++) idx += lines[i].length() + 1; return idx + clamp(column, 0, lines[rl].length()); }
    static int totalLength(String[] lines) { int len = 0; for (int i = 0; i < lines.length; i++) { len += lines[i].length(); if (i < lines.length - 1) len++; } return len; }
    static boolean shiftDown() { long w = net.minecraft.client.Minecraft.getInstance().getWindow().handle(); return GLFW.glfwGetKey(w, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(w, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS; }
    static String tr(String suffix, String fallback, Object... args) { return dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n.tr("clickgui.protocol_heuristics." + suffix, fallback, args); }

    record SourceHit(CombatProtocolHeuristicSource source, Rect card) {}
    record LineColumn(int line, int column) {}
    record Rect(float x, float y, float w, float h) { static final Rect EMPTY = new Rect(0f,0f,0f,0f); boolean contains(float mx,float my){ return inside(mx,my,x,y,w,h);} }
}
