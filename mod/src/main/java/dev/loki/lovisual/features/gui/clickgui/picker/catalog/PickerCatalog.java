package dev.loki.lovisual.features.gui.clickgui.picker.catalog;

import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;

import java.util.List;
import dev.loki.lovisual.features.gui.clickgui.picker.catalog.PickerEntryData;

@FunctionalInterface
public interface PickerCatalog {
    List<PickerEntryData> entries(TextListSetting owner);
}
