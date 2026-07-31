/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.picker;

import dev.loki.lovisual.features.gui.clickgui.settings.TextListSetting;

import java.util.List;

@FunctionalInterface
public interface PickerCatalog {
    List<PickerEntryData> entries(TextListSetting owner);
}
