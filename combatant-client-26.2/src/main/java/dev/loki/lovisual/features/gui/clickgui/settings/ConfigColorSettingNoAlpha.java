/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings;

import dev.loki.lovisual.config.ConfigObject;
import dev.loki.lovisual.config.ConfigSerializer;
import dev.loki.lovisual.config.values.RGBColorValue;

/**
 * Color setting that persists changes via a ConfigObject (non-Module owner).
 */
public class ConfigColorSettingNoAlpha extends ColorSettingNoAlpha {

    private final ConfigObject owner;

    public ConfigColorSettingNoAlpha(String name, RGBColorValue value, ConfigObject owner) {
        super(name, value);
        this.owner = owner;
    }

    @Override
    public void mouseReleased(double mx, double my, int button) {
        super.mouseReleased(mx, my, button);
        if (button != 0) return;
        if (owner != null) {
            ConfigSerializer.requestSave(owner);
        }
    }
}
