package dev.loki.lovisual.features.gui.hud.draggable.impl.metrics;

import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import dev.loki.lovisual.features.gui.hud.registry.HudElementInfo;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.base.HudTextEffects;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.script.CompactHudStatModel;
import dev.loki.lovisual.features.gui.hud.script.ScriptableHudStatWidget;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

@HudElementInfo(
        id = "system_time",
        displayName = "System Time",
        enabledByDefault = false,
        order = 170
)
public final class SystemTime extends DraggableHudElement implements ScriptableHudStatWidget {

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
    static final float ICON_SCALE = 0.88f;
    static final String WIDTH_TEMPLATE = "00:00";
    static final String WIDTH_TEMPLATE_SECONDS = "00:00:00";
    static final String COLOR_THEME = "Theme";
    static final String COLOR_CUSTOM = "Custom";
    static final String EFFECT_NONE = "None";
    static final String EFFECT_BLUR = "Blur";
    static final String EFFECT_GLASS = "Glass";

    final HudGlobalConfig hud = HudGlobalConfig.get();
    final BooleanValue showSeconds = bool("system_time_seconds", false);
    final NumberValue<Double> scale = num("system_time_scale", 2.37, 0.5, 5.0);
    final ModeValue colorMode = mode("system_time_color_mode", "color_mode", "Theme", new String[]{COLOR_THEME, COLOR_CUSTOM});
    final RGBColorValue iconColor = visibleWhen(colorNoAlpha("system_time_icon_color", "#FFFFFF"), this::isCustomMode);
    final RGBColorValue valueColor = visibleWhen(colorNoAlpha("system_time_value_color", "#FFFFFF"), this::isCustomMode);
    final ModeValue bgEffect = mode("system_time_bg_effect", "bg_effect", "Blur", new String[]{EFFECT_NONE, EFFECT_BLUR, EFFECT_GLASS});
    final RGBAColorValue bg = visibleWhen(color("system_time_bg", "#EB111318"), () -> isCustomMode() && !isGlassEffect());
    final RGBAColorValue bg2 = visibleWhen(color("system_time_bg_secondary", "#F7161616"), () -> isCustomMode() && !isGlassEffect());
    final RGBColorValue stroke = visibleWhen(colorNoAlpha("system_time_stroke", "#5A5A5A"), () -> isCustomMode() && !isGlassEffect());
    final NumberValue<Integer> bgAlpha = visibleWhen(num("system_time_bg_alpha", 235, 0, 255), () -> isThemeMode() && !isGlassEffect());
    final NumberValue<Integer> blurAlpha = visibleWhen(num("system_time_blur_alpha", 140, 0, 255), this::hasEffect);
    final EnumValue<HudTextEffects.Effect> iconEffect =
            enumSetting("system_time_icon_effect", HudTextEffects.Effect.NONE,
                    HudTextEffects.Effect.NONE, HudTextEffects.Effect.MIX, HudTextEffects.Effect.FLOW,
                    HudTextEffects.Effect.PULSE, HudTextEffects.Effect.STRIPE);
    final NumberValue<Integer> iconEffectSpeed =
            visibleWhen(num("system_time_icon_effect_speed", 18, 1, 60),
                    () -> iconEffect.get() != HudTextEffects.Effect.NONE);
    final CompactHudStatModel scriptModel = new CompactHudStatModel("system_time");

    int uiBgPrimary;
    int uiBgSecondary;
    int uiStroke;
    int uiIconColor;
    int uiValueColor;
    float displayTimeWidth = -1f;

    @Override
    public void applyDefaultPosition(int screenW, int screenH) {
        this.x = 16f;
        this.y = 48f;
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
        SystemTimeRenderer.render(this, renderer, textRenderer, ctx, tickDelta, screenW, screenH);
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
        return EFFECT_GLASS.equals(bgEffect.get());
    }

    boolean hasEffect() {
        return !EFFECT_NONE.equals(bgEffect.get());
    }
}
