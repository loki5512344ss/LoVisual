package dev.loki.lovisual.features.gui.hud.draggable.impl.stats;

import dev.loki.lovisual.features.gui.hud.config.HudBgStyles;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import dev.loki.lovisual.features.gui.hud.registry.HudElementInfo;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.base.HudTextEffects;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.script.CompactHudStatModel;
import dev.loki.lovisual.features.gui.hud.script.ScriptableHudStatWidget;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

@HudElementInfo(
        id = "fps",
        displayName = "FPS",
        enabledByDefault = true,
        order = 120
)
public final class Fps extends DraggableHudElement implements ScriptableHudStatWidget {

    static final Identifier FPS_ICON = Identifier.fromNamespaceAndPath("lovisual", "textures/hud/elements/fps.png");
    static final float SCALE_MULT = 0.68f;
    static final float BOX_HEIGHT = 20f;
    static final float BOX_RADIUS = 5f;
    static final float BOX_SOFTNESS = 1.0f;
    static final float STROKE_WIDTH = 0.55f;
    static final float ICON_SIZE = 12.5f;
    static final float ICON_X = 5f;
    static final float DIVIDER_X = 21f;
    static final float DIVIDER_Y = 4f;
    static final float DIVIDER_WIDTH = 0.6f;
    static final float TEXT_LEFT_X = 26f;
    static final float VALUE_UNIT_GAP = 2f;
    static final float RIGHT_PAD = 5f;
    static final String COLOR_THEME = "Theme";
    static final String COLOR_CUSTOM = "Custom";
    static final long DIGIT_ANIMATION_DURATION_MS = 200L;
    static final float DIGIT_ANIMATION_OFFSET = 8.0f;

    final HudGlobalConfig hud = HudGlobalConfig.get();
    final NumberValue<Double> scale = num("fps_scale", 2.37, 0.5, 5.0);
    final ModeValue colorMode = mode("fps_color_mode", "color_mode", "Theme", new String[]{COLOR_THEME, COLOR_CUSTOM});
    final RGBColorValue iconColor = visibleWhen(colorNoAlpha("fps_icon_color", "#FFFFFF"), this::isCustomMode);
    final RGBColorValue valueColor = visibleWhen(colorNoAlpha("fps_value_color", "#FFFFFF"), this::isCustomMode);
    final RGBColorValue metaColor = visibleWhen(colorNoAlpha("fps_meta_color", "#9B9B9B"), this::isCustomMode);
    final ModeValue bgEffect = mode("fps_bg_effect", "bg_effect", HudBgStyles.AUTO, HudBgStyles.options());
    final RGBAColorValue bg = visibleWhen(color("fps_bg", "#F7343434"), () -> isCustomMode() && !isGlassEffect());
    final RGBAColorValue bg2 = visibleWhen(color("fps_bg_secondary", "#F7161616"), () -> isCustomMode() && !isGlassEffect());
    final RGBColorValue stroke = visibleWhen(colorNoAlpha("fps_stroke", "#5A5A5A"), () -> isCustomMode() && !isGlassEffect());
    final NumberValue<Integer> bgAlpha = visibleWhen(num("fps_bg_alpha", 133, 0, 255), () -> isThemeMode() && !isGlassEffect());
    final NumberValue<Integer> blurAlpha = visibleWhen(num("fps_blur_alpha", 255, 0, 255), this::hasEffect);
    final EnumValue<HudTextEffects.Effect> labelEffect =
            enumSetting("fps_label_effect", HudTextEffects.Effect.FLOW,
                    HudTextEffects.Effect.NONE, HudTextEffects.Effect.MIX, HudTextEffects.Effect.FLOW,
                    HudTextEffects.Effect.PULSE, HudTextEffects.Effect.STRIPE);
    final NumberValue<Integer> labelEffectSpeed =
            visibleWhen(num("fps_label_effect_speed", 18, 1, 60),
                    () -> labelEffect.get() != HudTextEffects.Effect.NONE);
    final BooleanValue digitAnimation = bool("fps_digit_animation", false);
    final NumberValue<Integer> digitPollMs =
            visibleWhen(num("fps_digit_poll_ms", 495, 50, 1000), digitAnimation::get);
    final CompactHudStatModel scriptModel = new CompactHudStatModel("fps");

    String shownFps = "0";
    String previousShownFps = "";
    long digitAnimStartMs = 0L;
    long lastDigitPollMs = 0L;
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
        this.y = 20f;
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
        boolean preview = DraggableHudElementRegistry.isForceVisible();
        if (Minecraft.getInstance() == null && !preview) {
            width = 0f;
            height = 0f;
            scriptModel.setVisible(false);
            return;
        }
        if (!preview && !isEnabled()) {
            width = 0f;
            height = 0f;
            scriptModel.setVisible(false);
            return;
        }
        FpsRender.render(this, renderer, textRenderer, ctx, tickDelta, screenW, screenH);
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
        return HudBgStyles.isGlass(bgEffect.get());
    }

    boolean hasEffect() {
        return HudBgStyles.hasEffect(bgEffect.get());
    }

    public float hudX() {
        return getX();
    }

    public float hudY() {
        return getY();
    }

    public void setHudSize(float w, float h) {
        this.width = w;
        this.height = h;
    }
}
