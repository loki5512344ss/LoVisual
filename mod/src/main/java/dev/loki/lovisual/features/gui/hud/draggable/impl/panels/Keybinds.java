/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.panels;

import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.impl.HudPanelLayoutModes;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedListHudPanel;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@HudElementRegister(order = 45)
public final class Keybinds extends DraggableHudElement {

    {
        defaultLinkedLayout(456.6228f, 248.0f, "potions", "RIGHT", 0.0f, 0.0f);
    }

    static final float HEADER_HEIGHT = 15.5f;
    static final float CONTENT_START_Y = 25.0f;
    static final float ROW_STEP = 11.0f;
    static final float MIN_WIDTH = 102.0f;
    static final float HEADER_ICON_SCALE = 1.18f;
    static final float TITLE_TEXT_X = 22.0f;
    static final float COUNT_LABEL_OFFSET = 22.0f;
    static final String COLOR_THEME = "Theme";
    static final String COLOR_CUSTOM = "Custom";
    static final String EFFECT_NONE = "None";
    static final String EFFECT_BLUR = "Blur";
    static final String MODE_PRESS = "Press";
    static final String MODE_HOLD = "Hold";

    final HudGlobalConfig hud = HudGlobalConfig.get();
    final ScriptedListHudPanel scriptedPanel = new ScriptedListHudPanel();
    final NumberValue<Double> scaleValue = new NumberValue<>("keybinds_scale", 1.0, HudPanelLayoutModes.SCALE_MIN, HudPanelLayoutModes.SCALE_MAX);
    final ModeValue layoutMode = new ModeValue("keybinds_layout", "Unified Divider", HudPanelLayoutModes.SPLIT_HEADER, HudPanelLayoutModes.UNIFIED_DIVIDER);
    final ModeValue colorMode = new ModeValue("keybinds_color_mode", "Theme", COLOR_THEME, COLOR_CUSTOM);
    final ModeValue bgEffect = new ModeValue("keybinds_bg_effect", "None", EFFECT_NONE, EFFECT_BLUR);
    final RGBAColorValue bg = new RGBAColorValue("keybinds_bg", "#F7343434");
    final RGBAColorValue bg2 = new RGBAColorValue("keybinds_bg_secondary", "#F7161616");
    final NumberValue<Integer> bgAlpha = new NumberValue<>("keybinds_bg_alpha", 225, 0, 255);
    final RGBColorValue stroke = new RGBColorValue("keybinds_stroke", "#5A5A5A");
    final RGBColorValue text = new RGBColorValue("keybinds_text", "#FFFFFF");
    final RGBColorValue muted = new RGBColorValue("keybinds_muted", "#A5A5A5");
    final NumberValue<Integer> blurAlpha = new NumberValue<>("keybinds_blur_alpha", 140, 0, 255);
    final BooleanValue localActions = new BooleanValue("keybinds_local_actions", true);
    final BooleanMapValue localActionModes = new BooleanMapValue("keybinds_local_action_modes", new LinkedHashMap<>() {{
        put(MODE_PRESS, true);
        put(MODE_HOLD, true);
    }});
    final BooleanValue headerIconPulse = new BooleanValue("keybinds_header_icon_pulse", true);
    final NumberValue<Integer> headerIconPulseSpeed = new NumberValue<>("keybinds_header_icon_pulse_speed", 18, 1, 60);
    final NumberValue<Integer> headerIconPulseIntensity = new NumberValue<>("keybinds_header_icon_pulse_intensity", 100, 0, 100);

    final Map<String, Float> entryAnim = new LinkedHashMap<>();
    final Map<String, Row> lastEntries = new LinkedHashMap<>();
    float displayWidth = -1.0f;
    float displayHeight = -1.0f;
    float visibilityAnim = 0.0f;
    int uiHeaderLeft;
    int uiHeaderRight;
    int uiBodyLeft;
    int uiBodyRight;
    int uiOutline;
    int uiText;
    int uiMuted;
    int uiCounter;
    int uiTitleText;
    int uiDivider;
    int uiBlurTint;

    public Keybinds() {
        super("keybinds", "Keybinds", true);
    }

    @Override
    protected void defineSettings(List<SettingDef> defs) {
        defs.add(SettingDef.number(scaleValue));
        defs.add(SettingDef.mode(layoutMode));
        defs.add(SettingDef.mode(colorMode));
        defs.add(SettingDef.color(bg).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.color(bg2).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(stroke).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.number(bgAlpha).visibleWhen(this::isThemeMode));
        defs.add(SettingDef.colorNoAlpha(text).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(muted).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.mode(bgEffect));
        defs.add(SettingDef.number(blurAlpha).visibleWhen(this::hasEffect));
        defs.add(SettingDef.bool(localActions));
        defs.add(SettingDef.group("keybinds_local_action_modes", localActionModes).visibleWhen(localActions::get));
        defs.add(SettingDef.bool(headerIconPulse));
        defs.add(SettingDef.number(headerIconPulseSpeed).visibleWhen(headerIconPulse::get));
        defs.add(SettingDef.number(headerIconPulseIntensity).visibleWhen(headerIconPulse::get));
    }

    @Override
    public void applyDefaultPosition(int screenW, int screenH) {
        this.x = screenW - 128.0f;
        this.y = 80.0f;
    }

    @Override
    public boolean usesEngineRenderer() {
        return true;
    }

    @Override
    public void renderEngine(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) {
        KeybindsRenderer.render(this, renderer, textRenderer, ctx, tickDelta, screenW, screenH);
    }

    @Override
    public boolean isDraggable() {
        return super.isDraggable();
    }

    boolean isThemeMode() {
        return KeybindsPalette.isThemeMode(this);
    }

    boolean isCustomMode() {
        return KeybindsPalette.isCustomMode(this);
    }

    boolean hasEffect() {
        return KeybindsPalette.hasEffect(this);
    }

    void setSize(float w, float h) {
        setBounds(getX(), getY(), w, h);
    }

    void setEmpty() {
        setBounds(getX(), getY(), 0.0f, 0.0f);
    }

    record Row(String key, String name, String bindText, String icon, boolean local) {}
    record AnimatedRow(Row row, float anim) {}
}
