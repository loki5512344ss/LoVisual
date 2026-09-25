package dev.loki.lovisual.features.gui.hud.draggable.impl.targethud;

import dev.loki.lovisual.features.gui.hud.config.HudBgStyles;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.common.CommonSettingSchemas;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.pvp.opponents.TotemPopCounter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.render.TargetHudRenderer;

@HudElementRegister(order = 60)
public final class TargetHud extends DraggableHudElement {

    {
        defaultLayout(7.6799927f, 721.12f, "CENTER", "FREE");
    }

    public static final float BASE_WIDTH = 112.0f;
    public static final float BASE_HEIGHT = 40.0f;
    public static final float FACE_SIZE = 24.0f;
    public static final float FACE_X = 9.0f;
    public static final float FACE_Y = 8.0f;
    public static final float FACE_RADIUS = 4.0f;
    public static final float CONTENT_GAP = 6.0f;
    public static final float CONTENT_X_OFFSET = -2.0f;
    public static final float CONTENT_Y_OFFSET = -1.0f;
    public static final float PANEL_INSET = 2.0f;
    public static final float PANEL_RADIUS = 6.0f;
    public static final float NAME_Y = 13.0f;
    public static final float BAR_Y_OFFSET = 12.0f;
    public static final float BAR_WIDTH = 64.0f;
    public static final float BAR_HEIGHT = 4.0f;
    public static final float BAR_RADIUS = 2.0f;
    public static final float NAME_TEXT_SCALE = 0.34f;
    public static final float DISTANCE_TEXT_SCALE = 0.32f;
    public static final float HP_TEXT_SCALE = 0.34f;
    public static final float ARC_HP_TEXT_SCALE = 0.28f;
    public static final float DISTANCE_Y_OFFSET = 8.0f;
    public static final float ARC_CENTER_Y = 18.0f;
    public static final float ARC_RIGHT_INSET = 13.8f;
    public static final float ARC_RADIUS = 9.9f;
    public static final float ARC_THICKNESS = 3.5f;
    public static final float ARC_RESERVED_WIDTH = 34.0f;
    public static final float PLACEHOLDER_TEXT_SCALE = 0.62f;
    public static final float BASE_NAME_SCROLL_SPEED = 22.0f;
    public static final float BASE_NAME_SCROLL_GAP = 14.0f;
    public static final float BASE_NAME_FADE = 12.0f;
    public static final float NAME_SCROLL_PAUSE_SEC = 0.8f;
    public static final float EFFECTS_PANEL_GAP = 3.0f;
    public static final float EFFECTS_CHIP_HEIGHT = 10.0f;
    public static final float EFFECTS_CHIP_RADIUS = 3.0f;
    public static final float EFFECTS_CHIP_PAD_X = 3.0f;
    public static final float EFFECTS_CHIP_GAP = 2.0f;
    public static final float EFFECTS_ICON_SIZE = 8.0f;
    public static final float EFFECTS_TEXT_GAP = 2.0f;
    public static final float EFFECTS_TEXT_SCALE = 0.22f;
    public static final float EQUIPMENT_TOP_GAP = 2.0f;
    public static final float EQUIPMENT_BOTTOM_PAD = 2.0f;
    public static final float EQUIPMENT_SLOT = 11.0f;
    public static final float EQUIPMENT_GAP = 2.0f;
    public static final float EQUIPMENT_ROW_Y_OFFSET = 1.15f;
    public static final long HIT_FLASH_MS = 220L;
    public static final float HEALTH_SPEED = 6.0f;
    public static final float BAR_SPEED = 4.0f;
    public static final float BAR_TAIL_FADE_LENGTH = BAR_HEIGHT;
    public static final float ARC_TAIL_FADE_DEGREES = 20.0f;
    public static final float PROGRESS_SNAP_EPSILON = 0.0005f;
    public static final float DISTANCE_SPEED = 7.0f;
    public static final float EQUIPMENT_SPEED = 7.0f;
    public static final float HEALTH_TEXT_STEP = 0.25f;
    public static final float HEALTH_TEXT_SMALL_STEP = 0.10f;
    public static final float HEALTH_ZERO_CLAMP = 0.05f;
    public static final float HEALTH_ZERO_SNAP_THRESHOLD = 0.60f;
    public static final float TOTEM_BADGE_BOTTOM_PAD = 1.45f;
    public static final float TOTEM_BADGE_ICON_SIZE = 8.0f;
    public static final float TOTEM_BADGE_TEXT_GAP = 1.25f;
    public static final float TOTEM_BADGE_TEXT_SCALE = 0.24f;
    public static final float TOTEM_BADGE_ANIM_SPEED = 10.0f;
    public static final float TOTEM_BADGE_PULSE_SPEED = 8.0f;

    public static final float FACE_U0 = 8f / 64f;
    public static final float FACE_V0 = 8f / 64f;
    public static final float FACE_U1 = 16f / 64f;
    public static final float FACE_V1 = 16f / 64f;
    public static final float HAT_U0 = 40f / 64f;
    public static final float HAT_V0 = 8f / 64f;
    public static final float HAT_U1 = 48f / 64f;
    public static final float HAT_V1 = 16f / 64f;

    public static final String COLOR_THEME = "Theme";
    public static final String COLOR_CUSTOM = "Custom";
    public static final String EFFECTS_OFF = "Off";
    public static final String EFFECTS_COMPACT = "Compact";
    public static final String EQUIPMENT_OFF = "Off";
    public static final String EQUIPMENT_COMPACT = "Compact";
    public static final String HP_MODE_LINE = "Line";
    public static final String HP_MODE_ARC = "Arc";

    public static final ConcurrentHashMap<Integer, Long> HIT_FLASH_MS_BY_ID = new ConcurrentHashMap<>();

    public final HudGlobalConfig hud = HudGlobalConfig.get();

    public final NumberValue<Double> scale =
            new NumberValue<>("target_scale", 3.28, 0.5, 4.0);
    public final NumberValue<Integer> holdMs =
            new NumberValue<>("target_hold_ms", 1000, 0, 5000);
    public final BooleanValue totemPopCounter = TotemPopCounter.enabledValue();
    public final NumberValue<Integer> totemPopResetSeconds = TotemPopCounter.resetAfterSecondsValue();
    public final RGBColorValue totemPopTextColor =
            new RGBColorValue("target_totem_pop_text_color", "RNB#FFFFFF");
    public final BooleanValue marquee =
            new BooleanValue("target_marquee", true);
    public final BooleanValue playersOnly =
            new BooleanValue("target_players_only", false);
    public final ModeValue effectsMode =
            new ModeValue("target_effects_mode", "Off", EFFECTS_OFF, EFFECTS_COMPACT);
    public final BooleanValue effectsSelf =
            new BooleanValue("target_effects_self", true);
    public final ModeValue equipmentMode =
            new ModeValue("target_equipment_mode", "Off", EQUIPMENT_OFF, EQUIPMENT_COMPACT);
    public final ModeValue hpMode =
            new ModeValue("target_hp_mode", "Arc", HP_MODE_LINE, HP_MODE_ARC);
    public final ModeValue colorMode =
            new ModeValue("target_color_mode", "Theme", COLOR_THEME, COLOR_CUSTOM);
    public final RGBAColorValue bg =
            new RGBAColorValue("target_bg", "#F7343434");
    public final RGBAColorValue bg2 =
            new RGBAColorValue("target_bg_secondary", "#F7161616");
    public final RGBColorValue stroke =
            new RGBColorValue("target_stroke", "#5A5A5A");
    public final NumberValue<Integer> blurAlpha =
            new NumberValue<>("target_blur_alpha", 255, 0, 255);
    public final NumberValue<Integer> bgAlpha =
            new NumberValue<>("target_bg_alpha", 162, 0, 255);
    public final RGBColorValue text =
            new RGBColorValue("target_text", "#FFFFFF");
    public final RGBColorValue textSecondary =
            new RGBColorValue("target_text_secondary", "#D7D7D7");
    public final RGBColorValue barBg =
            new RGBColorValue("target_bar_bg", "#1E1E1E");
    public final RGBColorValue healthStart =
            new RGBColorValue("target_health_start", "#F3F3F3");
    public final RGBColorValue healthEnd =
            new RGBColorValue("target_health_end", "#9E9E9E");
    public final RGBColorValue trail =
            new RGBColorValue("target_trail", "#373737");
    public final RGBColorValue absorptionStart =
            new RGBColorValue("target_absorption_start", "#FFC300");
    public final RGBColorValue absorptionEnd =
            new RGBColorValue("target_absorption_end", "#FFD96B");
    public final RGBColorValue hitOuter =
            new RGBColorValue("target_hit_outer", "#FF5A5A");
    public final RGBColorValue hitInner =
            new RGBColorValue("target_hit_inner", "#FF7373");
    public final ModeValue bgEffect =
            new ModeValue("target_bg_effect", HudBgStyles.AUTO, HudBgStyles.NONE, HudBgStyles.BLUR, HudBgStyles.GLASS, HudBgStyles.AUTO);
    public final BooleanValue hitAnim =
            new BooleanValue("target_hit_anim", true);
    public final NumberValue<Double> hitPulse =
            new NumberValue<>("target_hit_pulse", 1.0, 0.0, 2.0);

    public LivingEntity displayTarget;
    public float visibilityAnimation;
    public float healthAnimation;
    public float trailAnimation;
    public float absorptionAnimation;
    public float effectsAnimation;
    public float equipmentAnimation;
    public float totemPopAnimation;
    public float totemPopTextPulse;
    public int totemPopLastCount;
    public UUID totemPopTargetId;
    public float displayedHealth;
    public float displayedDistance;
    public long lastSeenMs;
    public long waveStartMs = System.currentTimeMillis();

    public int uiBgPrimary;
    public int uiBgSecondary;
    public int uiStroke;
    public int uiTextPrimary;
    public int uiTextSecondary;
    public int uiBarBackground;
    public int uiHealthStart;
    public int uiHealthEnd;
    public float uiHealthGradientAngle;
    public int uiTrail;
    public int uiAbsorptionStart;
    public int uiAbsorptionEnd;
    public float uiAbsorptionGradientAngle;
    public int uiHitOuter;
    public int uiHitInner;

    public TargetHud() {
        super("target_hud", "TargetHUD", true);
    }

    public static void notifyHit(Entity target) {
        if (target == null) return;
        HIT_FLASH_MS_BY_ID.put(target.getId(), System.currentTimeMillis());
    }

    @Override
    protected void defineSettings(List<SettingDef> defs) {
        defs.add(SettingDef.number(scale));
        defs.add(SettingDef.number(holdMs));
        defs.add(SettingDef.bool(totemPopCounter)
                .common(CommonSettingSchemas.TOTEM_POP_COUNTER.commonI18nKey()));
        defs.add(SettingDef.number(totemPopResetSeconds)
                .common(CommonSettingSchemas.TOTEM_POP_RESET_SECONDS.commonI18nKey())
                .visibleWhen(totemPopCounter::get));
        defs.add(SettingDef.colorNoAlpha(totemPopTextColor)
                .visibleWhen(totemPopCounter::get));
        defs.add(SettingDef.bool(marquee));
        defs.add(SettingDef.bool(playersOnly));
        defs.add(SettingDef.mode(effectsMode));
        defs.add(SettingDef.bool(effectsSelf).visibleWhen(this::isEffectsEnabled));
        defs.add(SettingDef.mode(equipmentMode));
        defs.add(SettingDef.mode(hpMode));
        defs.add(SettingDef.mode(colorMode));
        defs.add(SettingDef.color(bg).visibleWhen(() -> isCustomMode() && !isGlassEffect()));
        defs.add(SettingDef.color(bg2).visibleWhen(() -> isCustomMode() && !isGlassEffect()));
        defs.add(SettingDef.colorNoAlpha(stroke).visibleWhen(() -> isCustomMode() && !isGlassEffect()));
        defs.add(SettingDef.number(bgAlpha).visibleWhen(() -> isThemeMode() && !isGlassEffect()));
        defs.add(SettingDef.colorNoAlpha(text).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(textSecondary).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(barBg).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(healthStart).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(healthEnd).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(trail).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(absorptionStart).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(absorptionEnd).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(hitOuter).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(hitInner).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.mode(bgEffect));
        defs.add(SettingDef.number(blurAlpha).visibleWhen(this::hasEffect));
        defs.add(SettingDef.bool(hitAnim));
        defs.add(SettingDef.number(hitPulse).visibleWhen(hitAnim::get));
    }

    @Override
    public void applyDefaultPosition(int screenW, int screenH) {
        float hudScale = HudScale.scale(screenW, screenH) * scale.get().floatValue();
        this.x = screenW / 2f - (BASE_WIDTH * hudScale) * 0.5f;
        this.y = screenH / 2f + BASE_HEIGHT * hudScale;
    }

    @Override
    public boolean usesEngineRenderer() {
        return true;
    }

    @Override
    public void renderEngine(Renderer2D renderer,
                             TextRenderer textRenderer,
                             GuiGraphicsExtractor ctx,
                             float tickDelta,
                             int screenW,
                             int screenH) {
        TargetHudRenderer.render(this, renderer, textRenderer, ctx, tickDelta, screenW, screenH);
    }

    public float hudX() {
        return x;
    }

    public float hudY() {
        return y;
    }

    public float hudWidth() {
        return width;
    }

    public float hudHeight() {
        return height;
    }

    public void setHudSize(float w, float h) {
        width = w;
        height = h;
    }

    public boolean isThemeMode() {
        return COLOR_THEME.equals(colorMode.get());
    }

    public boolean isCustomMode() {
        return COLOR_CUSTOM.equals(colorMode.get());
    }

    public boolean hasEffect() {
        return HudBgStyles.hasEffect(bgEffect.get());
    }

    public boolean isBlurEffect() {
        return HudBgStyles.isBlur(bgEffect.get());
    }

    public boolean isGlassEffect() {
        return HudBgStyles.isGlass(bgEffect.get());
    }

    public boolean isEffectsEnabled() {
        return !EFFECTS_OFF.equals(effectsMode.get());
    }

    public boolean isEquipmentEnabled() {
        return !EQUIPMENT_OFF.equals(equipmentMode.get());
    }

    public boolean isArcHealthMode() {
        return HP_MODE_ARC.equals(hpMode.get());
    }
}
