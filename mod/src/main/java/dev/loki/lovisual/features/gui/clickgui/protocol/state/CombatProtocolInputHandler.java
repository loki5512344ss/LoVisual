package dev.loki.lovisual.features.gui.clickgui.protocol;

import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.util.combat.protocol.heuristics.CombatProtocolHeuristicSource;
import dev.loki.lovisual.util.combat.protocol.heuristics.CombatProtocolHeuristicsConfig;
import org.lwjgl.glfw.GLFW;

final class CombatProtocolInputHandler {
    private CombatProtocolInputHandler() {}

    static void handleMouseMove(CombatProtocolHeuristicsEditorState s, float mx, float my) {
        if (s.focusedEditor != null) s.focusedEditor.mouseMoved(mx, my);
    }

    static void handleMouseDown(CombatProtocolHeuristicsEditorState s, float mx, float my, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return;
        if (!CombatProtocolHeuristicsEditorState.inside(mx, my, s.windowX, s.windowY, CombatProtocolHeuristicsEditorState.WINDOW_W, CombatProtocolHeuristicsEditorState.WINDOW_H) || s.close.contains(mx, my)) {
            ClickGuiRenderer.closeProtocolHeuristicsEditor();
            return;
        }
        for (CombatProtocolHeuristicsEditorState.SourceHit hit : s.sourceHits) {
            if (!hit.card().contains(mx, my)) continue;
            s.blurEditor();
            s.selected = hit.source();
            boolean enabled = s.sourceEnabled(hit.source());
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) s.owner.setSourceEnabled(hit.source().key(), !enabled);
            return;
        }
        ProtocolInlineEditor legacy = s.editor(s.selected, CombatProtocolHeuristicsConfig.ProtocolFamily.LEGACY);
        ProtocolInlineEditor modern = s.editor(s.selected, CombatProtocolHeuristicsConfig.ProtocolFamily.MODERN);
        if (legacy.mousePressed(mx, my, button)) { s.focus(legacy); return; }
        if (modern.mousePressed(mx, my, button)) { s.focus(modern); return; }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && s.reset.contains(mx, my)) {
            s.blurEditor();
            CombatProtocolHeuristicsConfig.get().reset(s.selected);
            s.reloadEditors(s.selected);
            return;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) s.blurEditor();
    }

    static void handleMouseUp(CombatProtocolHeuristicsEditorState s, float mx, float my, int button) {
        if (s.focusedEditor != null) s.focusedEditor.mouseReleased(button);
    }

    static void handleScroll(CombatProtocolHeuristicsEditorState s, double delta) {
        float mx = ClickGuiRenderer.getMouseX();
        float my = ClickGuiRenderer.getMouseY();
        ProtocolInlineEditor legacy = s.editor(s.selected, CombatProtocolHeuristicsConfig.ProtocolFamily.LEGACY);
        ProtocolInlineEditor modern = s.editor(s.selected, CombatProtocolHeuristicsConfig.ProtocolFamily.MODERN);
        if (legacy.scroll(mx, my, delta)) return;
        modern.scroll(mx, my, delta);
    }

    static boolean charTyped(CombatProtocolHeuristicsEditorState s, char chr) {
        return s.focusedEditor != null && s.focusedEditor.charTyped(chr);
    }

    static boolean keyPressed(CombatProtocolHeuristicsEditorState s, int key, int scancode, int action, int modifiers) {
        if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT) return s.focusedEditor != null;
        if (s.focusedEditor != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { s.blurEditor(); return true; }
            return s.focusedEditor.keyPressed(key, modifiers);
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) { ClickGuiRenderer.closeProtocolHeuristicsEditor(); return true; }
        return true;
    }
}
