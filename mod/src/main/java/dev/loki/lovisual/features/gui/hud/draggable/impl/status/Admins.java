package dev.loki.lovisual.features.gui.hud.draggable.impl.status;

import dev.loki.lovisual.features.gui.hud.config.HudBgStyles;
import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.impl.HudPanelLayoutModes;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedListHudPanel;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.features.relations.StaffData;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@HudElementRegister(order = 50)
public final class Admins extends DraggableHudElement {

    {
        defaultLinkedLayout(16.0f, 212.0f, "keybinds", "RIGHT", 0.0f, 0.0f);
    }

    static final float HEADER_HEIGHT = 15.5f;
    static final float CONTENT_START_Y = 25.0f;
    static final float ROW_STEP = 11.0f;
    static final float MIN_WIDTH = 96.0f;
    static final float ROW_DIVIDER_X = 15.0f;
    static final float ROW_DIVIDER_H = 6.0f;
    static final float TITLE_TEXT_X = 22.0f;
    static final float COUNT_LABEL_OFFSET = 22.0f;
    static final float ROW_HEAD_X = 3.5f;
    static final float ROW_HEAD_SIZE = 8.0f;
    static final float ROW_TEXT_X = 18.0f;
    static final float ROW_STATUS_RIGHT = 8.0f;
    static final float ROW_CONTENT_CENTER_OFFSET = 1.95f;
    static final String COLOR_THEME = "Theme";
    static final String COLOR_CUSTOM = "Custom";

    final HudGlobalConfig hud = HudGlobalConfig.get();
    final ScriptedListHudPanel scriptedPanel = new ScriptedListHudPanel();
    final NumberValue<Double> scaleValue = new NumberValue<>("admins_scale", 1.0, HudPanelLayoutModes.SCALE_MIN, HudPanelLayoutModes.SCALE_MAX);
    final ModeValue layoutMode = new ModeValue("admins_layout", "Split Header", HudPanelLayoutModes.SPLIT_HEADER, HudPanelLayoutModes.UNIFIED_DIVIDER);
    final ModeValue colorMode = new ModeValue("admins_color_mode", "Theme", COLOR_THEME, COLOR_CUSTOM);
    final ModeValue bgEffect = new ModeValue("admins_bg_effect", HudBgStyles.AUTO, HudBgStyles.NONE, HudBgStyles.BLUR, HudBgStyles.AUTO);
    final RGBAColorValue bg = new RGBAColorValue("admins_bg", "#EB111318");
    final RGBAColorValue bg2 = new RGBAColorValue("admins_bg_secondary", "#F7161616");
    final NumberValue<Integer> bgAlpha = new NumberValue<>("admins_bg_alpha", 131, 0, 255);
    final RGBColorValue stroke = new RGBColorValue("admins_stroke", "#5A5A5A");
    final RGBColorValue text = new RGBColorValue("admins_text", "#FFFFFF");
    final RGBColorValue muted = new RGBColorValue("admins_muted", "#A5A5A5");
    final NumberValue<Integer> blurAlpha = new NumberValue<>("admins_blur_alpha", 255, 0, 255);
    final BooleanValue headerIconPulse = new BooleanValue("admins_header_icon_pulse", true);
    final NumberValue<Integer> headerIconPulseSpeed = new NumberValue<>("admins_header_icon_pulse_speed", 18, 1, 60);
    final NumberValue<Integer> headerIconPulseIntensity = new NumberValue<>("admins_header_icon_pulse_intensity", 100, 0, 100);

    final Map<String, Float> entryAnim = new LinkedHashMap<>();
    final Map<String, StaffData.StaffEntry> lastEntries = new LinkedHashMap<>();
    float displayWidth = -1.0f;
    float displayHeight = -1.0f;
    float visibilityAnim = 0.0f;
    int uiHeaderLeft; int uiHeaderRight; int uiBodyLeft; int uiBodyRight; int uiOutline; int uiText; int uiMuted; int uiCounter; int uiTitleText; int uiDivider; int uiBlurTint;

    public Admins() { super("admins", "Admins", true); }

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
        defs.add(SettingDef.bool(headerIconPulse));
        defs.add(SettingDef.number(headerIconPulseSpeed).visibleWhen(headerIconPulse::get));
        defs.add(SettingDef.number(headerIconPulseIntensity).visibleWhen(headerIconPulse::get));
    }

    @Override public void applyDefaultPosition(int screenW, int screenH) { this.x = screenW - 200.0f; this.y = 80.0f; }
    @Override public boolean usesEngineRenderer() { return true; }

    @Override
    public void renderEngine(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx,
                             float tickDelta, int screenW, int screenH) {
        AdminsRenderer.render(this, renderer, textRenderer, ctx, tickDelta, screenW, screenH);
    }

    @Override public boolean isDraggable() { return super.isDraggable(); }

    boolean isThemeMode() { return AdminsPalette.isThemeMode(this); }
    boolean isCustomMode() { return AdminsPalette.isCustomMode(this); }
    boolean hasEffect() { return AdminsPalette.hasEffect(this); }

    void setSize(float w, float h) { setBounds(getX(), getY(), w, h); }
    void setEmpty() { setBounds(getX(), getY(), 0.0f, 0.0f); }

    record AnimatedEntry(StaffData.StaffEntry entry, float anim) {}
}
