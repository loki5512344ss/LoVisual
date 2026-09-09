/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings.bind;

import dev.loki.lovisual.config.SettingOwner;
import dev.loki.lovisual.config.values.BindMode;
import dev.loki.lovisual.config.values.KeyBindValue;
import dev.loki.lovisual.util.input.KeyManager;
import dev.loki.lovisual.util.wav.ClickGuiSounds;

import java.util.function.BooleanSupplier;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRendererBridge;

/**
 * Function bind without toggle(): listens to active state (PRESS/HOLD).
 */
public class FunctionBindSetting extends Setting {

    private final KeyBindValue value;
    private final BindMode mode;
    private final UiState ui = new UiState();
    private String registeredName = null;
    private String hudIcon = null;
    private String hudLabel = null;
    private BooleanSupplier hudToggleState = null;

    public FunctionBindSetting(String id, String defaultKey) {
        this(id, defaultKey, BindMode.PRESS);
    }

    public FunctionBindSetting(String id, String defaultKey, BindMode mode) {
        this(id, new KeyBindValue(id, defaultKey), mode);
    }

    public FunctionBindSetting(String id, KeyBindValue value, BindMode mode) {
        super("Function: " + id, value);
        this.value = value;
        this.mode = mode;
    }

    public String get() {
        return value.get();
    }

    public void set(String keyName) {
        value.set(keyName.toUpperCase());
    }

    public boolean isPressed() {
        return KeyManager.wasPressed(bindingName());
    }

    BindMode mode() {
        return mode;
    }

    public BindMode getMode() {
        return mode;
    }

    public String getActionId() {
        return value != null ? value.getName() : getId();
    }

    public String getHudLabel() {
        return hudLabel;
    }

    public String getHudIcon() {
        return hudIcon;
    }

    public FunctionBindSetting hudIcon(String icon) {
        this.hudIcon = icon == null || icon.isBlank() ? null : icon.trim();
        return this;
    }

    public FunctionBindSetting hudSvg(String svgName) {
        return hudIcon(svgName);
    }

    public FunctionBindSetting hudLabel(String label) {
        this.hudLabel = label == null || label.isBlank() ? null : label.trim();
        return this;
    }

    public FunctionBindSetting hud(String label, String icon) {
        return hudLabel(label).hudSvg(icon);
    }

    public FunctionBindSetting hudToggle(BooleanSupplier activeState) {
        this.hudToggleState = activeState;
        return this;
    }

    public boolean isHudToggle() {
        return hudToggleState != null;
    }

    public boolean isHeldForHud() {
        return KeyManager.isHeldAllowScreen(bindingName());
    }

    public boolean isHudToggleActive() {
        if (hudToggleState == null) return false;
        try {
            return hudToggleState.getAsBoolean();
        } catch (Throwable ignored) {
            return false;
        }
    }

    UiState ui() {
        return ui;
    }

    // ====================== CONFIG ======================

    @Override
    public Object save() {
        return value.toJson();
    }

    @Override
    public void load(Object o) {
        value.fromJson(o);
        refreshRegistration();
    }

    // ======================== UI ========================

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

    public void onKeySelected(String combo) {
        String previous = value.get();
        boolean wasBound = previous != null && !previous.equalsIgnoreCase("NONE");
        if (combo == null || combo.equalsIgnoreCase("NONE")) {
            set("NONE");
            ClickGuiSounds.bindingNull();
        } else {
            set(combo);
            if (wasBound) {
                ClickGuiSounds.bindingReset();
            } else {
                ClickGuiSounds.bindingSuccess();
            }
        }
        refreshRegistration();
        ui.waiting = false;
        if (getParent() != null) getParent().saveConfig();
    }

    public String getBindingName() {
        return bindingName();
    }

    @Override
    public void setParent(SettingOwner owner) {
        // unregister under previous name (could be null before parent is set)
        unregister();
        super.setParent(owner);
        refreshRegistration();
    }

    private String bindingName() {
        SettingOwner parent = getParent();
        String actionId = value.getName();
        return parent != null ? parent.name() + ":" + actionId : actionId;
    }

    @Override
    protected String getTranslationId() {
        return value != null ? value.getName() : super.getTranslationId();
    }

    private void refreshRegistration() {
        unregister();
        if (!value.isNone()) {
            registeredName = bindingName();
            KeyManager.registerCombo(registeredName, value.get());
        }
    }

    private void unregister() {
        if (registeredName != null) {
            KeyManager.unregisterAll(registeredName);
        } else {
            // In case it was registered before parent was set, try using current binding name
            KeyManager.unregisterAll(bindingName());
        }
        registeredName = null;
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
        public float layoutMarkerY = 0f;
        public float layoutBoxY = 0f;
        public float layoutRowH = 54f;
        public float layoutBoxW = 36f;
        public float layoutTextW = 0f;
    }
}
