package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.look;

import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;

/** The pages of the Themes category: colour themes, then how widgets and the ClickGUI are painted. */
public enum ThemesPage {
    THEMES("themes", "Themes"),
    HUD("hud", "HUD"),
    CLICKGUI("clickgui", "ClickGUI");

    private final String key;
    private final String fallbackTitle;

    ThemesPage(String key, String fallbackTitle) {
        this.key = key;
        this.fallbackTitle = fallbackTitle;
    }

    public String title() {
        return ClickGuiI18n.tr("clickgui.settings.themes.page." + key, fallbackTitle);
    }
}
