package dev.loki.lovisual.features.gui.hud.draggable.impl.panels;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import dev.loki.lovisual.config.i18n.ConfigValueAliasProvider;
import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

import java.util.ArrayList;
import java.util.List;

@HudElementRegister(order = 10)
public final class ModuleList extends DraggableHudElement implements ConfigValueAliasProvider {

    static final float BASE_LINE_H = 9f;
    static final float BASE_TEXT_Y = 3f;
    static final float BASE_TEXT_X = 3f;
    static final float BASE_TEXT_X_REV = 2f;
    static final float BASE_STRING_PAD = 3f;
    static final float BASE_RECT_EXTRA = 1f;
    static final float BASE_RADIUS = 2f;
    static final float BASE_SOFTNESS = 0.55f;

    final HudGlobalConfig hud = HudGlobalConfig.get();
    final NumberValue<Double> scaleValue =
            new NumberValue<>("module_list_scale", 3.67, 0.5, 5.0);
    final EnumValue<ColorMode> colorMode =
            new EnumValue<>("module_list_color_mode", ColorMode.CUSTOM, ColorMode.THEME, ColorMode.CUSTOM);
    final EnumValue<BgEffect> bgEffect =
            new EnumValue<>("module_list_bg_effect", BgEffect.BLUR, BgEffect.NONE, BgEffect.BLUR, BgEffect.GLASS);
    final NumberValue<Integer> bgAlpha =
            new NumberValue<>("module_list_bg_alpha", 226, 0, 255);
    final NumberValue<Integer> blurAlpha =
            new NumberValue<>("module_list_blur_alpha", 255, 0, 255);
    final RGBAColorValue bgTopColor =
            new RGBAColorValue("module_list_bg", "#FF444242");
    final RGBAColorValue bgBottomColor =
            new RGBAColorValue("module_list_bg_secondary", "#22000000");
    final RGBColorValue textTopColor =
            new RGBColorValue("module_list_text", "RNB#E87171");
    final RGBColorValue textBottomColor =
            new RGBColorValue("module_list_text_secondary", "#93118C");
    final EnumValue<AnimatedRenderColors.Mode> textGradientMode =
            new EnumValue<>("module_list_text_gradient_mode",
                    AnimatedRenderColors.Mode.RAINBOW,
                    AnimatedRenderColors.Mode.STATIC,
                    AnimatedRenderColors.Mode.RAINBOW,
                    AnimatedRenderColors.Mode.LIGHT_RAINBOW,
                    AnimatedRenderColors.Mode.SKY,
                    AnimatedRenderColors.Mode.FADE,
                    AnimatedRenderColors.Mode.DOUBLE_COLOR,
                    AnimatedRenderColors.Mode.ANALOGOUS,
                    AnimatedRenderColors.Mode.THEME);
    final NumberValue<Integer> textGradientSpeed =
            new NumberValue<>("module_list_text_gradient_speed", 18, 2, 54);
    final EnumValue<TextEffect> textEffect =
            new EnumValue<>("module_list_text_effect", TextEffect.NONE, TextEffect.NONE, TextEffect.SHIMMER);
    final NumberValue<Integer> textEffectSpeed =
            new NumberValue<>("module_list_text_effect_speed", 30, 1, 60);

    final List<Row> rows = new ArrayList<>();
    final List<Module> enabledModules = new ArrayList<>();
    float renderX;
    float renderY;
    int cachedModulesHash;
    int cachedModulesCount;
    float cachedTextScale = -1f;

    public ModuleList() {
        super("module_list", "ModuleList", false);
    }

    @Override
    public List<String> getLegacyValueNames(String currentValueName) {
        if (currentValueName == null) return List.of();
        if (currentValueName.startsWith("module_list_")) {
            return List.of("active_modules_" + currentValueName.substring("module_list_".length()));
        }
        return List.of();
    }

    @Override
    protected void defineSettings(List<SettingDef> defs) {
        defs.add(SettingDef.number(scaleValue));
        defs.add(SettingDef.mode(colorMode));
        defs.add(SettingDef.mode(bgEffect));
        defs.add(SettingDef.number(bgAlpha).visibleWhen(this::isThemeMode));
        defs.add(SettingDef.number(blurAlpha).visibleWhen(this::hasEffect));
        defs.add(SettingDef.color(bgTopColor).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.color(bgBottomColor).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(textTopColor).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(textBottomColor).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.mode(textGradientMode));
        defs.add(SettingDef.number(textGradientSpeed).visibleWhen(this::hasAnimatedTextGradient));
        defs.add(SettingDef.mode(textEffect));
        defs.add(SettingDef.number(textEffectSpeed).visibleWhen(() -> textEffect.get() != TextEffect.NONE));
    }

    @Override
    public void applyDefaultPosition(int screenW, int screenH) {
        this.x = screenW - 80f;
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
        ModuleListRenderer.render(this, renderer, textRenderer, ctx, tickDelta, screenW, screenH);
    }

    @Override
    public boolean contains(float mx, float my) {
        return mx >= renderX && mx <= renderX + width
                && my >= renderY && my <= renderY + height;
    }

    boolean isThemeMode() {
        return ModuleListPalette.isThemeMode(this);
    }

    boolean isCustomMode() {
        return ModuleListPalette.isCustomMode(this);
    }

    boolean hasEffect() {
        return ModuleListPalette.hasEffect(this);
    }

    boolean hasAnimatedTextGradient() {
        return ModuleListPalette.hasAnimatedTextGradient(this);
    }

    void setSize(float w, float h) { setBounds(getX(), getY(), w, h); }
    void setEmpty() { setBounds(getX(), getY(), 0f, 0f); }
    void clampX(float maxX) { float nx = getX(); if (nx > maxX) nx = maxX; if (nx < 0f) nx = 0f; moveTo(nx, getY()); }

    enum ColorMode implements EnumValue.IdProvider { THEME("Theme"), CUSTOM("Custom"); private final String id; ColorMode(String id){this.id=id;} @Override public String id(){return id;} }
    enum BgEffect implements EnumValue.IdProvider { NONE("None"), BLUR("Blur"), GLASS("Glass"); private final String id; BgEffect(String id){this.id=id;} @Override public String id(){return id;} }
    enum TextEffect implements EnumValue.IdProvider { NONE("None"), SHIMMER("Shimmer"); private final String id; TextEffect(String id){this.id=id;} @Override public String id(){return id;} }

    static final class Row {
        final Module module;
        final String label;
        float textWidth;
        float x;
        float y;
        float w;
        int fillTop;
        int fillBottom;

        Row(Module module, String label) {
            this.module = module;
            this.label = label;
        }
    }
}
