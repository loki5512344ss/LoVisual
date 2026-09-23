/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings.color;

import dev.loki.lovisual.config.definition.ConfigObject;
import dev.loki.lovisual.config.io.ConfigSerializer;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.features.gui.clickgui.settings.color.ColorSettingNoAlpha;
import dev.loki.lovisual.features.module.core.Module;

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
