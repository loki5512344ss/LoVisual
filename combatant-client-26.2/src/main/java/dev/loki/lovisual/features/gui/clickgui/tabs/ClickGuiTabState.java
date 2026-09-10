package dev.loki.lovisual.features.gui.clickgui.tabs;

import dev.loki.lovisual.addon.manager.ClickGuiSectionManager;
import dev.loki.lovisual.features.gui.clickgui.sections.ClickGuiSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.ClickGuiModel.ClickGuiTabEntry;
import dev.loki.lovisual.features.module.lifecycle.Modules;

public final class ClickGuiTabState {

    private ClickGuiTabState() {
    }

    public static ClickGuiSection getActiveSection() {

        List<ClickGuiTabEntry> tabs = currentTabs();
        ensureActiveTab(tabs);
        for (ClickGuiTabEntry tab : tabs) {
            if (tab.id().equals(ClickGuiRenderer.activeTabId)) {
                return tab.section();
            }
        }
        ClickGuiRenderer.activeTabId = ClickGuiRenderer.MODULES_TAB_ID;
        return ClickGuiRenderer.MODULES_SECTION;
    }

    public static boolean isSettingsTabActive() {

        return ClickGuiRenderer.SETTINGS_TAB_ID.equals(ClickGuiRenderer.activeTabId);
    }

    public static List<ClickGuiTabEntry> currentTabs() {

        List<ClickGuiSectionManager.Entry> addonSections = ClickGuiSectionManager.sections();
        ArrayList<ClickGuiTabEntry> out = new ArrayList<>(2 + addonSections.size());
        out.add(new ClickGuiTabEntry(ClickGuiRenderer.MODULES_TAB_ID, "Modules", ClickGuiRenderer.MODULES_SECTION));
        out.add(new ClickGuiTabEntry(ClickGuiRenderer.SETTINGS_TAB_ID, "Settings", ClickGuiRenderer.SETTINGS_SECTION));
        for (ClickGuiSectionManager.Entry entry : addonSections) {
            out.add(new ClickGuiTabEntry(entry.id(), entry.label(), entry.section()));
        }
        return out;
    }

    static void ensureActiveTab(List<ClickGuiTabEntry> tabs) {
        for (ClickGuiTabEntry tab : tabs) {
            if (tab.id().equals(ClickGuiRenderer.activeTabId)) {
                return;
            }
        }
        ClickGuiRenderer.activeTabId = ClickGuiRenderer.MODULES_TAB_ID;
    }

    static void ensureTabCapacity(int size) {
        if (ClickGuiRenderer.tabX.length >= size && ClickGuiRenderer.tabW.length >= size) return;
        ClickGuiRenderer.tabX = Arrays.copyOf(ClickGuiRenderer.tabX, size);
        ClickGuiRenderer.tabW = Arrays.copyOf(ClickGuiRenderer.tabW, size);
    }

    static float easeOutCubic(float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }

    public static ClickGuiRenderer.ClickGuiIslandState islandState() {

        List<ClickGuiTabEntry> tabs = currentTabs();
        ensureActiveTab(tabs);
        String activeLabel = tabs.isEmpty() ? "" : tabs.get(0).label();
        for (ClickGuiTabEntry tab : tabs) {
            if (tab.id().equals(ClickGuiRenderer.activeTabId)) {
                activeLabel = tab.label();
                break;
            }
        }
        return new ClickGuiRenderer.ClickGuiIslandState(
                ClickGuiRenderer.tabBarX,
                ClickGuiRenderer.tabBarY,
                ClickGuiRenderer.tabBarW,
                ClickGuiRenderer.tabBarH,
                ClickGuiRenderer.tabAnimX,
                ClickGuiRenderer.tabAnimW,
                ClickGuiRenderer.lifecycleAnim,
                activeLabel,
                tabs.size(),
                ClickGuiRenderer.picker != null || ClickGuiRenderer.protocolHeuristicsEditor != null
        );
    }
}
