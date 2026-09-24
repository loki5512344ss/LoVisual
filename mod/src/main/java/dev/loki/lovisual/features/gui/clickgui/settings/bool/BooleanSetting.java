package dev.loki.lovisual.features.gui.clickgui.settings.bool;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRendererBridge;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.runtime.UiState;

public class BooleanSetting extends Setting {

    private final BooleanValue value;
    private final UiState ui = new UiState();

    public BooleanSetting(String name, BooleanValue value) {
        super(name, value);
        this.value = value;
        this.ui.anim = value.get() ? 1f : 0f;
    }

    public boolean get() {
        return value.get();
    }

    public void set(boolean v) {
        value.set(v);
    }

    UiState ui() {
        return ui;
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
        public float lastX, lastY, lastW;
        public float lastH;
        public float lastControlX, lastControlY, lastControlW, lastControlH;
        public float anim = 0f;
        public float hoverAnim = 0f;
        public float layoutW = Float.NaN;
        public String layoutLabel = null;
        public String[] layoutLines = new String[]{""};
        public float layoutLineH = 0f;
        public float layoutLabelY = 0f;
        public float layoutControlY = 0f;
        public float layoutRowH = 44f;
    }
}
