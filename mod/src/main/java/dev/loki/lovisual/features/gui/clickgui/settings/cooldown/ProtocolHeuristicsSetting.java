package dev.loki.lovisual.features.gui.clickgui.settings.cooldown;

import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.collection.SetValue;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.UnifiedSettingsSkin;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.runtime.UiState;

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
