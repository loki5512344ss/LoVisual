package dev.loki.lovisual.features.gui.clickgui.layout.screen.modules;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.modules.model.MenuPresence;
import dev.loki.lovisual.features.gui.clickgui.tabs.ClickGuiSearch;

/** Builds the {@link MenuPresence} snapshot of the modules menu for the given mouse position. */
final class ModulesMenuPresence {

    private static final int NO_MODULE = 255;

    private ModulesMenuPresence() {
    }

    static MenuPresence snapshot(ModulesMenuScreen screen, float mx, float my, boolean pressed) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (ModulesMenuPanel p : screen.panels) {
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            maxX = Math.max(maxX, p.x + p.w);
            maxY = Math.max(maxY, p.y + p.h);
        }
        if (maxX <= minX || maxY <= minY) return null;
        int category = -1;
        int module = NO_MODULE;
        boolean settings = false;
        int scroll = 0;
        for (ModulesMenuPanel p : screen.panels) {
            if (!ModulesMenuScreen.inside(mx, my, p.x, p.y, p.w, p.h)) continue;
            category = p.index;
            settings = p.selected != null;
            if (settings) {
                scroll = ratio(p.settingsSmoothScroll, p.maxSettingsScroll);
                break;
            }
            scroll = ratio(p.modulesSmoothScroll, p.maxModulesScroll);
            for (int i = 0; i < p.hits.size() && i < NO_MODULE; i++) {
                ModulesMenuPanel.ModuleHit h = p.hits.get(i);
                if (ModulesMenuScreen.inside(mx, my, h.x(), h.y(), h.w(), h.h())) {
                    module = i;
                    break;
                }
            }
            break;
        }
        float x = Math.max(0.0f, Math.min(1.0f, (mx - minX) / (maxX - minX)));
        float y = Math.max(0.0f, Math.min(1.0f, (my - minY) / (maxY - minY)));
        return new MenuPresence(category, module, settings, ClickGuiSearch.isActive(), x, y, pressed, scroll);
    }

    private static int ratio(float value, float max) {
        return max <= 0.0f ? 0 : Math.max(0, Math.min(255, Math.round(Math.abs(value) / max * 255.0f)));
    }
}
