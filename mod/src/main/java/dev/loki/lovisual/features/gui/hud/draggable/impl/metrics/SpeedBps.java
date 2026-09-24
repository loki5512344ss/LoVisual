package dev.loki.lovisual.features.gui.hud.draggable.impl.metrics;

import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import dev.loki.lovisual.features.gui.hud.registry.HudElementInfo;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.base.HudTextEffects;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.script.CompactHudStatModel;
import dev.loki.lovisual.features.gui.hud.script.ScriptableHudStatWidget;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

//todo Description
@HudElementInfo(
        id = "speed_bps",
        displayName = "Speed BPS",
        enabledByDefault = true,
        order = 90
)
public final class SpeedBps extends DraggableHudElement implements ScriptableHudStatWidget {

    static final Identifier SPEED_ICON = Identifier.fromNamespaceAndPath("lovisual", "textures/hud/elements/speedometer.png");
    static final float SCALE_MULT = 0.68f;
    static final float BOX_HEIGHT = 20f;
    static final float BOX_RADIUS = 5f;
    static final float BOX_SOFTNESS = 1.0f;
    static final float STROKE_WIDTH = 0.55f;
    static final float ICON_SIZE = 11f;
    static final float ICON_X = 5f;
    static final float DIVIDER_X = 21f;
    static final float DIVIDER_Y = 4f;
    static final float DIVIDER_WIDTH = 0.6f;
    static final float TEXT_LEFT_X = 26f;
    static final float RIGHT_PAD = 5f;
    static final float KMH_MULT = 3.6f;
    static final String COLOR_THEME = "Theme";
    static final String COLOR_CUSTOM = "Custom";
    static final String EFFECT_NONE = "None";
    static final String EFFECT_BLUR = "Blur";
    static final String EFFECT_GLASS = "Glass";

    static final long SAMPLE_INTERVAL_MS = 75L;
    static final int SAMPLE_COUNT = 20;

    final HudGlobalConfig hud = HudGlobalConfig.get();
    final BooleanValue useBps = bool("speed_bps_bps", true);
    final BooleanValue average = bool("speed_bps_average", false);
    final NumberValue<Double> scale = num("speed_bps_scale", 2.37, 0.5, 5.0);
    final ModeValue colorMode = mode("speed_bps_color_mode", "color_mode", "Theme", new String[]{COLOR_THEME, COLOR_CUSTOM});
    final RGBColorValue iconColor = visibleWhen(colorNoAlpha("speed_bps_icon_color", "#FFFFFF"), this::isCustomMode);
    final RGBColorValue valueColor = visibleWhen(colorNoAlpha("speed_bps_value_color", "#FFFFFF"), this::isCustomMode);
    final RGBColorValue metaColor = visibleWhen(colorNoAlpha("speed_bps_meta_color", "#9B9B9B"), this::isCustomMode);
    final ModeValue bgEffect = mode("speed_bps_bg_effect", "bg_effect", "Blur", new String[]{EFFECT_NONE, EFFECT_BLUR, EFFECT_GLASS});
    final RGBAColorValue bg = visibleWhen(color("speed_bps_bg", "#F7343434"), () -> isCustomMode() && !isGlassEffect());
    final RGBAColorValue bg2 = visibleWhen(color("speed_bps_bg_secondary", "#F7161616"), () -> isCustomMode() && !isGlassEffect());
    final RGBColorValue stroke = visibleWhen(colorNoAlpha("speed_bps_stroke", "#5A5A5A"), () -> isCustomMode() && !isGlassEffect());
    final NumberValue<Integer> bgAlpha = visibleWhen(num("speed_bps_bg_alpha", 135, 0, 255), () -> isThemeMode() && !isGlassEffect());
    final NumberValue<Integer> blurAlpha = visibleWhen(num("speed_bps_blur_alpha", 255, 0, 255), this::hasEffect);
    final EnumValue<HudTextEffects.Effect> labelEffect =
            enumSetting("speed_bps_label_effect", HudTextEffects.Effect.MIX,
                    HudTextEffects.Effect.NONE, HudTextEffects.Effect.MIX, HudTextEffects.Effect.FLOW,
                    HudTextEffects.Effect.PULSE, HudTextEffects.Effect.STRIPE);
    final NumberValue<Integer> labelEffectSpeed =
            visibleWhen(num("speed_bps_label_effect_speed", 60, 1, 60),
                    () -> labelEffect.get() != HudTextEffects.Effect.NONE);
    final CompactHudStatModel scriptModel = new CompactHudStatModel("speed_bps");
    final float[] samples = new float[SAMPLE_COUNT];
    final StringBuilder valueBuilder = new StringBuilder(16);
    int sampleIndex = 0;
    int sampleCount = 0;
    long lastSampleMs = 0L;
    float displayedSpeed = 0f;
    int lastTenth = Integer.MIN_VALUE;
    String cachedNumber = "0.0";
    float displayValueWidth = -1f;
    int uiBgPrimary;
    int uiBgSecondary;
    int uiStroke;
    int uiIconColor;
    int uiValueColor;
    int uiMetaColor;

    @Override
    public void applyDefaultPosition(int screenW, int screenH) {
        this.x = 16f;
        this.y = screenH - 48f;
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
        SpeedBpsRenderer.render(this, renderer, textRenderer, ctx, tickDelta, screenW, screenH);
    }

    @Override
    public boolean supportsWidgetAnchoring() {
        return true;
    }

    @Override
    public CompactHudStatModel compactStatModel() {
        return scriptModel;
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
}
