package dev.loki.lovisual.features.gui.hud.draggable.impl.metrics;

import dev.loki.lovisual.features.gui.hud.config.HudBgStyles;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.hud.base.HudTextEffects;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.registry.HudElementInfo;
import dev.loki.lovisual.features.gui.hud.script.CompactHudStatModel;
import dev.loki.lovisual.features.gui.hud.script.ScriptableHudStatWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

@HudElementInfo(
        id = "game_time",
        displayName = "Game Time",
        enabledByDefault = false,
        order = 180
)
public final class GameTime extends DraggableHudElement implements ScriptableHudStatWidget {

    static final float SCALE_MULT = 0.68f;
    static final float BOX_HEIGHT = 20f;
    static final float BOX_RADIUS = 5f;
    static final float BOX_SOFTNESS = 1.0f;
    static final float STROKE_WIDTH = 0.55f;
    static final float ICON_X = 5f;
    static final float DIVIDER_X = 21f;
    static final float DIVIDER_Y = 4f;
    static final float DIVIDER_WIDTH = 0.6f;
    static final float TEXT_LEFT_X = 26f;
    static final float RIGHT_PAD = 5f;
    static final float ICON_SCALE = 0.92f;
    static final String WIDTH_TEMPLATE = "00:00";
    static final String COLOR_THEME = "Theme";
    static final String COLOR_CUSTOM = "Custom";
    static final long SUNRISE_START = 23000L;
    static final long SUNRISE_END = 1000L;
    static final long SUNSET_START = 12000L;
    static final long SUNSET_END = 13000L;
    static final int ICON_SUNRISE = 0xEA0E;
    static final int ICON_SUN = 0xEA04;
    static final int ICON_SUNSET = 0xEA0F;
    static final int ICON_RAIN_DAY = 0xEA01;
    static final int ICON_RAIN_NIGHT = 0xEA09;
    static final int ICON_THUNDER_DAY = 0xEA05;
    static final int ICON_THUNDER_NIGHT = 0xEA0C;
    static final int ICON_SNOW_DAY = 0xEA03;
    static final int ICON_SNOW_NIGHT = 0xEA0B;
    static final int ICON_THUNDER_SNOW_DAY = 0xEA02;
    static final int ICON_THUNDER_SNOW_NIGHT = 0xEA0A;

    final HudGlobalConfig hud = HudGlobalConfig.get();
    final BooleanValue showWeather = bool("game_time_weather", "weather", true);
    final NumberValue<Double> scale = num("game_time_scale", 2.37, 0.5, 5.0);
    final ModeValue colorMode = mode("game_time_color_mode", "color_mode", "Theme", new String[]{COLOR_THEME, COLOR_CUSTOM});
    final RGBColorValue iconColor = visibleWhen(colorNoAlpha("game_time_icon_color", "#FFFFFF"), this::isCustomMode);
    final RGBColorValue valueColor = visibleWhen(colorNoAlpha("game_time_value_color", "#FFFFFF"), this::isCustomMode);
    final ModeValue bgEffect = mode("game_time_bg_effect", "bg_effect", HudBgStyles.AUTO, HudBgStyles.options());
    final RGBAColorValue bg = visibleWhen(color("game_time_bg", "#EB111318"), () -> isCustomMode() && !isGlassEffect());
    final RGBAColorValue bg2 = visibleWhen(color("game_time_bg_secondary", "#F7161616"), () -> isCustomMode() && !isGlassEffect());
    final RGBColorValue stroke = visibleWhen(colorNoAlpha("game_time_stroke", "#5A5A5A"), () -> isCustomMode() && !isGlassEffect());
    final NumberValue<Integer> bgAlpha = visibleWhen(num("game_time_bg_alpha", 235, 0, 255), () -> isThemeMode() && !isGlassEffect());
    final NumberValue<Integer> blurAlpha = visibleWhen(num("game_time_blur_alpha", 140, 0, 255), this::hasEffect);
    final EnumValue<HudTextEffects.Effect> iconEffect =
            enumSetting("game_time_icon_effect", HudTextEffects.Effect.MIX,
                    HudTextEffects.Effect.NONE, HudTextEffects.Effect.MIX, HudTextEffects.Effect.FLOW,
                    HudTextEffects.Effect.PULSE, HudTextEffects.Effect.STRIPE);
    final NumberValue<Integer> iconEffectSpeed =
            visibleWhen(num("game_time_icon_effect_speed", 18, 1, 60),
                    () -> iconEffect.get() != HudTextEffects.Effect.NONE);
    final CompactHudStatModel scriptModel = new CompactHudStatModel("game_time");

    int uiBgPrimary;
    int uiBgSecondary;
    int uiStroke;
    int uiIconColor;
    int uiValueColor;
    float displayTimeWidth = -1f;

    @Override
    public void applyDefaultPosition(int screenW, int screenH) {
        this.x = 16f;
        this.y = 76f;
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
        GameTimeRenderer.render(this, renderer, textRenderer, ctx, tickDelta, screenW, screenH);
    }

    @Override
    public CompactHudStatModel compactStatModel() {
        return scriptModel;
    }

    float hudX() {
        return getX();
    }

    float hudY() {
        return getY();
    }

    void setHudSize(float w, float h) {
        this.width = w;
        this.height = h;
    }

    boolean isThemeMode() {
        return COLOR_THEME.equals(colorMode.get());
    }

    boolean isCustomMode() {
        return COLOR_CUSTOM.equals(colorMode.get());
    }

    boolean isGlassEffect() {
        return HudBgStyles.isGlass(bgEffect.get());
    }

    boolean hasEffect() {
        return HudBgStyles.hasEffect(bgEffect.get());
    }
}
