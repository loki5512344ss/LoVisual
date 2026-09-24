package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module;

import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;

enum HudPreviewMode {
    ALL_ENABLED,
    ONLY_CURRENT
}

record SettingRow(Setting setting, float anim, float height, float gap, float scale) {
}

record SettingHit(Setting setting, float x, float y, float w, float h, float scale) {
}
