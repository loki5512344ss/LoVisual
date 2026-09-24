package dev.loki.lovisual.features.gui.clickgui.settings.group;

import dev.loki.lovisual.config.values.primitive.BooleanMapValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRendererBridge;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.runtime.UiState;

public class GroupSetting extends Setting {

    private final BooleanMapValue value;
    private final UiState ui = new UiState();
    private Predicate<String> optionVisibility = key -> true;

    public GroupSetting(String name, BooleanMapValue value) {
        super(name, value);   // <--- FIX
        this.value = value;
    }

    BooleanMapValue value() {
        return value;
    }

    UiState ui() {
        return ui;
    }

    boolean isOptionVisible(String key) {
        return optionVisibility.test(key);
    }

    public GroupSetting visibleOptionsWhen(Predicate<String> predicate) {
        this.optionVisibility = predicate != null ? predicate : key -> true;
        ui.lastLayoutW = Float.NaN;
        ui.layoutOptionCount = -1;
        return this;
    }

    @Override
    public void preflightI18n() {
        super.preflightI18n();
        if (value == null) return;
        for (String key : value.getAll().keySet()) {
            if (!isOptionVisible(key)) continue;
            preflightOptionI18n(key);
        }
    }

    @Override
    public Object save() {
        return value == null ? null : value.toJson();
    }

    @Override
    public void load(Object o) {
        if (value != null) value.fromJson(o);
    }

    @Override
    public void render(float x, float y, float w, float mx, float my) {
        SettingRendererBridge.render(this, x, y, w, mx, my);
    }

    @Override
    public void mouseClicked(double mx, double my, int b) {
        SettingRendererBridge.mouseClicked(this, mx, my, b);
    }

    @Override
    public float getHeight() {
        return SettingRendererBridge.getHeight(this);
    }

    public static final class UiState {
        public final List<OptionLayout> optionLayouts = new ArrayList<>();
        public final Map<String, Float> hoverAnims = new HashMap<>();
        public final Map<String, Float> selectAnims = new HashMap<>();
        public float baseX, baseY, baseW;
        public float lastLayoutW = Float.NaN;
        public int layoutOptionCount = -1;
        public float cachedHeight = 34f;
    }

    public record OptionLayout(String id, String label, float x, float y, float w) {
        public String getId() {
            return id;
        }
    }
}

