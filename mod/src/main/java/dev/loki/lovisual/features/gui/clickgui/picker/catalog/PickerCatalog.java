/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.picker.catalog;

import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;

import java.util.List;
import dev.loki.lovisual.features.gui.clickgui.picker.catalog.PickerEntryData;

@FunctionalInterface
public interface PickerCatalog {
    List<PickerEntryData> entries(TextListSetting owner);
}
