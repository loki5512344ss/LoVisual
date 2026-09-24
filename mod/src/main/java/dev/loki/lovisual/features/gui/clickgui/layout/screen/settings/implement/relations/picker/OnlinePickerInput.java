
package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.relations;

import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;
import dev.loki.lovisual.features.module.modules.misc.DefineTarget;
import dev.loki.lovisual.util.text.mask.ChatNameUtil;
import org.lwjgl.glfw.GLFW;

final class OnlinePickerInput {
    private static final String I18N = "clickgui.settings.relations.online_picker.";
    boolean click(OnlineRelationPlayerPickerComponent host, float mx, float my, int button) {
        if (!host.openTarget || button != 0) return false;
        if (host.closeButton.contains(mx, my)) { host.close(); return true; }
        if (host.searchButton.contains(mx, my)) { focusSearch(host); return true; }
        if (host.searchField.contains(mx, my)) { focusSearch(host); return true; }
        for (OnlineRelationPlayerPickerComponent.RowHit hit : host.rowHits) {
            if (!hit.rect().contains(mx, my)) continue;
            addPlayer(host, hit.name());
            return true;
        }
        if (host.bounds.contains(mx, my)) { host.searchFocused = false; return true; }
        return false;
    }
    boolean mousePressedScrollbar(OnlineRelationPlayerPickerComponent host, float mx, float my, int button) {
        if (!host.openTarget || button != 0) return false;
        return host.scroll.press(mx, my, button);
    }
    void mouseReleased(OnlineRelationPlayerPickerComponent host, int button) { host.scroll.release(button); }
    void scroll(OnlineRelationPlayerPickerComponent host, float mx, float my, double amount) {
        if (!host.openTarget || !host.listRect.contains(mx, my)) return;
        host.scroll.addScroll((float) (amount * 24f));
    }
    boolean keyPressed(OnlineRelationPlayerPickerComponent host, int keyCode, int scanCode, int modifiers) {
        if (!host.openTarget) return false;
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        if (ctrl && keyCode == GLFW.GLFW_KEY_F) { focusSearch(host); return true; }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (host.searchFocused) { host.searchFocused = false; return true; }
            host.close(); return true;
        }
        if (!host.searchFocused) return false;
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { host.searchFocused = false; return true; }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!host.data.search.isEmpty()) { host.data.search = host.data.search.substring(0, host.data.search.length() - 1); host.scroll.reset(); host.data.invalidateFilter(); }
            return true;
        }
        return true;
    }
    boolean charTyped(OnlineRelationPlayerPickerComponent host, char chr, int modifiers) {
        if (!host.openTarget || !host.searchFocused) return false;
        if (chr >= 32 && chr != 127) {
            host.data.search += chr;
            if (host.data.search.length() > 32) host.data.search = host.data.search.substring(0, 32);
            host.scroll.reset();
            host.data.invalidateFilter();
        }
        return true;
    }
    void focusSearch(OnlineRelationPlayerPickerComponent host) {
        if (!host.openTarget) return;
        host.searchFocused = true;
        host.searchOpen = false;
    }
    void clearSearch(OnlineRelationPlayerPickerComponent host) {
        if (host.data.search == null || host.data.search.isEmpty()) return;
        host.data.search = "";
        host.scroll.reset();
        host.data.invalidateFilter();
    }
    void addPlayer(OnlineRelationPlayerPickerComponent host, String rawName) {
        String name = ChatNameUtil.normalizeNickCandidate(rawName);
        if (!ChatNameUtil.isNickLike(name)) { setStatus(host, tr("invalid", "Invalid nick")); return; }
        String modeName = modeDisplayName(host.mode);
        boolean wasIn = DefineTarget.isPlayerInRelationMode(host.mode, name);
        boolean added = DefineTarget.togglePlayerRelation(host.mode, name);
        host.data.refreshNow();
        if (wasIn && !added) setStatus(host, tr("removed", "Removed %s from %s", name, modeName));
        else setStatus(host, tr("added", "Added %s as %s", name, modeName));
    }
    private void setStatus(OnlineRelationPlayerPickerComponent host, String text) { if (host.statusSink != null) host.statusSink.accept(text); }
    private static String modeDisplayName(DefineTarget.RelationTargetMode m) { if (m == null) return ""; return tr("mode_name." + m.id(), m.fallbackLabel()); }
    private static String tr(String k, String f, Object... a) { return ClickGuiI18n.tr(I18N + k, f, a); }
}
