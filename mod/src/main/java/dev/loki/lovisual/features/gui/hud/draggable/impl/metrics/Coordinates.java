package dev.loki.lovisual.features.gui.hud.draggable.impl.metrics;

import dev.loki.lovisual.features.gui.hud.config.HudBgStyles;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
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

@HudElementInfo(
        id = "xyz",
        displayName = "XYZ",
        enabledByDefault = true,
        order = 100
)
public final class Coordinates extends DraggableHudElement implements ScriptableHudStatWidget {

    static final Identifier COORDS_ICON = Identifier.fromNamespaceAndPath("lovisual", "textures/hud/elements/coords.png");
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
    static final float SEGMENT_GAP = 8f;
    static final float LABEL_VALUE_GAP = 2f;
    static final float RIGHT_PAD = 5f;
    static final String COLOR_THEME = "Theme";
    static final String COLOR_CUSTOM = "Custom";

    final HudGlobalConfig hud = HudGlobalConfig.get();
    final EnumValue<NetherMode> netherMode =
            enumSetting("xyz_nether", NetherMode.ON, NetherMode.OFF, NetherMode.ON, NetherMode.ONLY_NETHER);
    final NumberValue<Double> scale = num("xyz_scale", 2.37, 0.5, 5.0);
    final ModeValue colorMode = mode("xyz_color_mode", "color_mode", "Theme", new String[]{COLOR_THEME, COLOR_CUSTOM});
    final RGBColorValue iconColor = visibleWhen(colorNoAlpha("xyz_icon_color", "#FFFFFF"), this::isCustomMode);
    final RGBColorValue labelColor = visibleWhen(colorNoAlpha("xyz_label_color", "#9B9B9B"), this::isCustomMode);
    final RGBColorValue valueColor = visibleWhen(colorNoAlpha("xyz_value_color", "#FFFFFF"), this::isCustomMode);
    final RGBColorValue extraColor = visibleWhen(colorNoAlpha("xyz_extra_color", "#BEBEBE"), this::isCustomMode);
    final ModeValue bgEffect = mode("xyz_bg_effect", "bg_effect", HudBgStyles.AUTO, HudBgStyles.options());
    final RGBAColorValue bg = visibleWhen(color("xyz_bg", "#F7343434"), () -> isCustomMode() && !isGlassEffect());
    final RGBAColorValue bg2 = visibleWhen(color("xyz_bg_secondary", "#F7161616"), () -> isCustomMode() && !isGlassEffect());
    final RGBColorValue stroke = visibleWhen(colorNoAlpha("xyz_stroke", "#5A5A5A"), () -> isCustomMode() && !isGlassEffect());
    final NumberValue<Integer> bgAlpha = visibleWhen(num("xyz_bg_alpha", 184, 0, 255), () -> isThemeMode() && !isGlassEffect());
    final NumberValue<Integer> blurAlpha = visibleWhen(num("xyz_blur_alpha", 255, 0, 255), this::hasEffect);
    final EnumValue<HudTextEffects.Effect> labelEffect =
            enumSetting("xyz_label_effect", HudTextEffects.Effect.FLOW,
                    HudTextEffects.Effect.NONE, HudTextEffects.Effect.MIX, HudTextEffects.Effect.FLOW,
                    HudTextEffects.Effect.PULSE, HudTextEffects.Effect.STRIPE);
    final NumberValue<Integer> labelEffectSpeed =
            visibleWhen(num("xyz_label_effect_speed", 18, 1, 60),
                    () -> labelEffect.get() != HudTextEffects.Effect.NONE);
    final CompactHudStatModel scriptModel = new CompactHudStatModel("xyz");
    int uiBgPrimary;
    int uiBgSecondary;
    int uiStroke;
    int uiIconColor;
    int uiLabelColor;
    int uiValueColor;
    int uiExtraColor;
    float displayXValueWidth = -1f;
    float displayYValueWidth = -1f;
    float displayZValueWidth = -1f;
    float displayExtraWidth = -1f;

    @Override
    public void applyDefaultPosition(int screenW, int screenH) {
        this.x = 16f;
        this.y = screenH - 92f;
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
        CoordinatesRenderer.render(this, renderer, textRenderer, ctx, tickDelta, screenW, screenH);
    }

    @Override
    public boolean supportsWidgetAnchoring() {
        return true;
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

    boolean shouldShowNether(boolean inNether) {
        NetherMode mode = netherMode.get();
        if (mode == NetherMode.ON) return true;
        if (mode == NetherMode.ONLY_NETHER) return !inNether;
        return false;
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

    enum NetherMode implements EnumValue.IdProvider {
        OFF("Off"),
        ON("On"),
        ONLY_NETHER("OnlyNether");

        private final String id;

        NetherMode(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }
}
