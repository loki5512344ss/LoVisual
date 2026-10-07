package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.cosmetics;

import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;

import java.util.List;

/** Sub-sections of the Cosmetics settings category; each lists the modules that belong to it. */
public enum CosmeticKind {
    WINGS("wings", "Wings", "wings"),
    HATS("hats", "Hats", "chinahat", "tazikhat"),
    MODELS("models", "Models", "custommodels"),
    MASKS("masks", "Masks", "trollface_mask"),
    WEAPONS("weapons", "Weapons", "knife");

    private final String key;
    private final String fallbackTitle;
    private final List<String> moduleIds;

    CosmeticKind(String key, String fallbackTitle, String... moduleIds) {
        this.key = key;
        this.fallbackTitle = fallbackTitle;
        this.moduleIds = List.of(moduleIds);
    }

    public List<String> moduleIds() {
        return moduleIds;
    }

    public String title() {
        return ClickGuiI18n.tr("clickgui.settings.cosmetics.kind." + key, fallbackTitle);
    }

    /** The kind that lists this module, or null when it is not a cosmetic. */
    public static CosmeticKind ofModule(String moduleId) {
        for (CosmeticKind kind : values()) {
            if (kind.moduleIds.contains(moduleId)) return kind;
        }
        return null;
    }
}
