package dev.loki.lovisual.features.gui.clickgui.settings.bind;

import dev.loki.lovisual.config.definition.SettingOwner;
import dev.loki.lovisual.config.values.color.KeyBindValue;
import dev.loki.lovisual.util.input.KeyManager;
import dev.loki.lovisual.util.wav.util.ClickGuiSounds;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRendererBridge;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.runtime.UiState;

public class KeyBindSetting extends Setting {

    private final KeyBindValue value;
    private final UiState ui = new UiState();

    public KeyBindSetting(KeyBindValue value) {
        super(value.getName(), value);
        this.value = value;

        if (!value.isNone()) {
            KeyManager.registerCombo(bindingName(), value.get());
        }
    }

    public KeyBindValue getValue() {
        return value;
    }

    UiState ui() {
        return ui;
    }

    private String bindingName() {
        return getParent() != null ? getParent().name() : value.getName();
    }

    @Override
    public void setParent(SettingOwner owner) {
        super.setParent(owner);
        // re-register under module name once parent is known
        KeyManager.unregisterAll(value.getName());
        if (!value.isNone()) {
            KeyManager.registerCombo(bindingName(), value.get());
        }
    }

    public void setKeyByName(String name) {
        value.set(name);
        KeyManager.unregisterAll(bindingName());
        if (!value.isNone()) {
            KeyManager.registerCombo(bindingName(), name);
        }
    }

    @Override
    public String getTranslationKey() {
        return "setting.module.bind";
    }
    // ========================== CONFIG ============================ //

    @Override
    public Object save() {
        return value.toJson();
    }

    @Override
    public void load(Object o) {
        value.fromJson(o);

        KeyManager.unregisterAll(bindingName());

        if (!value.isNone())
            KeyManager.registerCombo(bindingName(), value.get());
    }

    // =============================== UI ================================ //

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

    public void onKeySelected(String comboString) {
        String previous = value.get();
        boolean wasBound = previous != null && !previous.equalsIgnoreCase("NONE");
        KeyManager.unregisterAll(bindingName());

        if (comboString == null || comboString.equalsIgnoreCase("NONE")) {
            value.set("NONE");
            ClickGuiSounds.bindingNull();
        } else {
            value.set(comboString.toUpperCase());
            KeyManager.registerCombo(bindingName(), comboString);
            if (wasBound) {
                ClickGuiSounds.bindingReset();
            } else {
                ClickGuiSounds.bindingSuccess();
            }
        }

        ui.waiting = false;
        if (getParent() != null) getParent().saveConfig();
    }

    public static final class UiState {
        public float lastX;
        public float lastY;
        public boolean waiting = false;
        public float lastW;
        public float lastH;
        public float lastBx, lastBy, lastBw, lastBh;
        public float hoverAnim = 0f;
        public float waitAnim = 0f;
        public float layoutW = Float.NaN;
        public String layoutLabel = null;
        public String layoutDisplay = null;
        public String[] layoutLines = new String[]{""};
        public float layoutLineH = 0f;
        public float layoutLabelY = 0f;
        public float layoutBoxY = 0f;
        public float layoutRowH = 46f;
        public float layoutBoxW = 36f;
        public float layoutTextW = 0f;
    }
}

