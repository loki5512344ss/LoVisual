package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.main;

import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;

/**
 * Data types for {@link MainSettingsComponent}: the top-level settings categories and
 * the per-frame hit-test/layout records. Package-private, kept together so the component
 * class stays focused on behavior.
 */
enum Category {
    IMAGE("Image"),
    SECURITY("Security"),
    UTILITY("Utility"),
    MISCELLANIOUS("Miscellanious");

    final String title;

    Category(String title) {
        this.title = title;
    }
}

record SettingRow(Setting setting, float anim, float height, float gap) {
}

record SettingHit(Setting setting, float x, float y, float w, float h) {
}

record CategoryHit(Category category, float x, float y, float w, float h) {
}
