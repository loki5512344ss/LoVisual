package dev.loki.lovisual.features.gui.clickgui.settings.mode;

import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRendererBridge;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.runtime.UiState;

public class ModeSetting extends Setting {

    private final ModeValue modeValue;
    private final EnumValue<?> enumValue;
    private final Runnable onChange;
    private final UiState ui = new UiState();

    public ModeSetting(String name, ModeValue value) {
        this(name, value, null);
    }

    public ModeSetting(String name, ModeValue value, Runnable onChange) {
        super(name, value);
        this.modeValue = value;
        this.enumValue = null;
        this.onChange = onChange;
    }

    public ModeSetting(String name, EnumValue<?> value) {
        this(name, value, null);
    }

    public ModeSetting(String name, EnumValue<?> value, Runnable onChange) {
        super(name, value);
        this.modeValue = null;
        this.enumValue = value;
        this.onChange = onChange;
    }

    Runnable onChange() {
        return onChange;
    }

    UiState ui() {
        return ui;
    }

    List<String> optionIds() {
        return modeValue != null ? modeValue.getOptions() : enumValue.getOptions();
    }

    String selectedId() {
        return modeValue != null ? modeValue.get() : enumValue.getId();
    }

    void selectId(String id) {
        if (modeValue != null) {
            modeValue.fromJson(id);
        } else {
            enumValue.setById(id);
        }
    }

    @Override
    public void preflightI18n() {
        super.preflightI18n();
        for (String opt : optionIds()) {
            preflightOptionI18n(opt);
        }
    }

    @Override
    public Object save() {
        if (modeValue != null) return modeValue.toJson();
        return enumValue == null ? null : enumValue.toJson();
    }

    @Override
    public void load(Object o) {
        if (modeValue != null) {
            modeValue.fromJson(o);
        } else if (enumValue != null) {
            enumValue.fromJson(o);
        }
        if (onChange != null) onChange.run();
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
        public float baseX, baseY, baseW;
        public float cachedHeight = 64f;
        public float selectX, selectY, selectW;
        public boolean selectInit = false;
        public float lastLayoutW = Float.NaN;
        public int layoutOptionCount = -1;
    }

    public record OptionLayout(String id, String label, float x, float y, float w) {
        public String getId() {
            return id;
        }
    }
}
