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
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@HudElementRegister(order = 20)
public final class Potions extends DraggableHudElement {

    {
        defaultLayout(31.729004f, 182.0f);
    }

    static final float HEADER_HEIGHT = 15.5f;
    static final float BODY_Y_OFFSET = 18.5f;
    static final float CONTENT_START_Y = 25.0f;
    static final float BODY_INSET_Y = 6.5f;
    static final float ROW_STEP = 11.0f;
    static final float MIN_WIDTH = 95.0f;
    static final float PANEL_RADIUS = 4.0f;
    static final float PANEL_SOFTNESS = 1.0f;
    static final float PANEL_STROKE = 0.55f;
    static final float HEADER_DIVIDER_X = 18.0f;
    static final float HEADER_DIVIDER_Y = 5.0f;
    static final float HEADER_DIVIDER_H = 6.0f;
    static final float ROW_DIVIDER_X = 14.0f;
    static final float ROW_DIVIDER_H = 7.0f;
    static final float TITLE_ICON_X = 5.0f;
    static final float TITLE_TEXT_X = 22.0f;
    static final float COUNT_LABEL_OFFSET = 22.0f;
    static final float COUNT_VALUE_OFFSET = 3.0f;
    static final float ROW_ICON_X = 3.5f;
    static final float ROW_ICON_SIZE = 8.0f;
    static final float ROW_TEXT_X = 18.0f;
    static final float ROW_TIME_RIGHT = 8.0f;
    static final float ROW_CONTENT_CENTER_OFFSET = 2.1f;
    static final int BLINK_THRESHOLD_TICKS = 200;
    static final double BLINK_SPEED = 0.009;
    static final String COLOR_THEME = "Theme";
    static final String COLOR_CUSTOM = "Custom";
    static final int NEGATIVE_EFFECT_RGB = 0xFFFF554B;
    static final Identifier[] PREVIEW_ICONS = new Identifier[]{
            Identifier.fromNamespaceAndPath("minecraft", "textures/mob_effect/speed.png"),
            Identifier.fromNamespaceAndPath("minecraft", "textures/mob_effect/haste.png"),
            Identifier.fromNamespaceAndPath("minecraft", "textures/mob_effect/fire_resistance.png"),
            Identifier.fromNamespaceAndPath("minecraft", "textures/mob_effect/regeneration.png"),
            Identifier.fromNamespaceAndPath("minecraft", "textures/mob_effect/night_vision.png")
    };

    final HudGlobalConfig hud = HudGlobalConfig.get();
    final ScriptedListHudPanel scriptedPanel = new ScriptedListHudPanel();
    final NumberValue<Double> scaleValue = new NumberValue<>("potions_scale", 1.05, HudPanelLayoutModes.SCALE_MIN, HudPanelLayoutModes.SCALE_MAX);
    final ModeValue layoutMode = new ModeValue("potions_layout", "Unified Divider", HudPanelLayoutModes.SPLIT_HEADER, HudPanelLayoutModes.UNIFIED_DIVIDER);
    final ModeValue colorMode = new ModeValue("potions_color_mode", "Theme", COLOR_THEME, COLOR_CUSTOM);
    final ModeValue bgEffect = new ModeValue("potions_bg_effect", HudBgStyles.AUTO, HudBgStyles.NONE, HudBgStyles.BLUR, HudBgStyles.AUTO);
    final RGBAColorValue bg = new RGBAColorValue("potions_bg", "#EB784F4F");
    final RGBAColorValue bg2 = new RGBAColorValue("potions_bg_secondary", "#F7161616");
    final NumberValue<Integer> bgAlpha = new NumberValue<>("potions_bg_alpha", 200, 0, 255);
    final RGBColorValue stroke = new RGBColorValue("potions_stroke", "#5A5A5A");
    final RGBColorValue text = new RGBColorValue("potions_text", "#FFFFFF");
    final RGBColorValue muted = new RGBColorValue("potions_muted", "#A5A5A5");
    final NumberValue<Integer> blurAlpha = new NumberValue<>("potions_blur_alpha", 159, 0, 255);
    final BooleanValue headerIconPulse = new BooleanValue("potions_header_icon_pulse", true);
    final NumberValue<Integer> headerIconPulseSpeed = new NumberValue<>("potions_header_icon_pulse_speed", 18, 1, 60);
    final NumberValue<Integer> headerIconPulseIntensity = new NumberValue<>("potions_header_icon_pulse_intensity", 100, 0, 100);

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
    int uiAmpText;
    int uiDivider;
    int uiBlurTint;

    public Potions() {
        super("potions", "Potions", true);
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
        defs.add(SettingDef.bool(headerIconPulse));
        defs.add(SettingDef.number(headerIconPulseSpeed).visibleWhen(headerIconPulse::get));
        defs.add(SettingDef.number(headerIconPulseIntensity).visibleWhen(headerIconPulse::get));
    }

    @Override
    public void applyDefaultPosition(int screenW, int screenH) {
        this.x = screenW - 120.0f;
        this.y = 20.0f;
    }

    @Override
    public boolean usesEngineRenderer() {
        return true;
    }

    @Override
    public void renderEngine(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) {
        PotionsRenderer.render(this, renderer, textRenderer, ctx, tickDelta, screenW, screenH);
    }

    @Override
    public boolean isDraggable() {
        return super.isDraggable();
    }

    boolean isThemeMode() {
        return PotionsPalette.isThemeMode(this);
    }

    boolean isCustomMode() {
        return PotionsPalette.isCustomMode(this);
    }

    boolean hasEffect() {
        return PotionsPalette.hasEffect(this);
    }

    void setSize(float w, float h) {
        setBounds(getX(), getY(), w, h);
    }

    void setEmpty() {
        setBounds(getX(), getY(), 0.0f, 0.0f);
    }

    record Row(String key, MobEffectInstance effect, String name, String amp, String duration, Identifier iconId) {}
    record AnimatedRow(Row row, float anim) {}
}
