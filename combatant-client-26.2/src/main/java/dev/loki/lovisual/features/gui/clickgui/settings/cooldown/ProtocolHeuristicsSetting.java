/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings.cooldown;

import dev.loki.lovisual.config.values.BooleanMapValue;
import dev.loki.lovisual.config.values.ConfigValue;
import dev.loki.lovisual.config.values.SetValue;
import dev.loki.lovisual.features.gui.clickgui.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.UnifiedSettingsSkin;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;

/**
 * Compact Edit entry backed by per-module source toggles. The actual regex
 * editor is a dedicated screen and stores its shared rules separately.
 */
public final class ProtocolHeuristicsSetting extends TextListSetting {
    private final BooleanMapValue sources;

    public ProtocolHeuristicsSetting(String name, BooleanMapValue sources) {
        super(name, new SetValue(name + "_editor_placeholder"), PickerMode.TEXT);
        this.sources = sources;
    }

    public BooleanMapValue sources() {
        return sources;
    }

    public void setSourceEnabled(String key, boolean enabled) {
        if (sources == null || key == null) return;
        sources.set(key, enabled);
        if (getParent() != null) getParent().saveConfig();
    }

    @Override
    public void mouseClicked(double mx, double my, int button) {
        if (button != 0) return;
        UiState ui = ui();
        if (!UnifiedSettingsSkin.inside(mx, my, ui.lastButtonX, ui.lastButtonY, ui.lastButtonW, ui.lastButtonH)) {
            return;
        }
        ClickGuiRenderer.openProtocolHeuristicsEditor(this, getDisplayName());
    }

    @Override
    public Object save() {
        return sources == null ? null : sources.toJson();
    }

    @Override
    public void load(Object value) {
        if (sources != null) sources.fromJson(value);
    }

    @Override
    public ConfigValue<?> getConfigValue() {
        return sources;
    }
}
