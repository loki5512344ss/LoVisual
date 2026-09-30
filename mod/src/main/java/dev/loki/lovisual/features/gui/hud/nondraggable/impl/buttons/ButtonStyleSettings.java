package dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons;

import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;

import java.util.List;

/**
 * Settings schema and live value holders for {@link BetterButtons}.
 * Declared package-private so the facade can read these fields directly
 * from its getters, mirroring {@code bars/impl/BarSettings}.
 */
final class ButtonStyleSettings {

    final EnumValue<BetterButtons.Style> style =
            new EnumValue<>("style", BetterButtons.Style.DEFAULT, BetterButtons.Style.values());
    final RGBAColorValue gradientBaseColor =
            new RGBAColorValue("gradient_base_color", "#FF2F2929");
    final RGBAColorValue gradientColor =
            new RGBAColorValue("gradient_color", "#FF902F2F");
    final NumberValue<Float> gradientAngle =
            new NumberValue<>("gradient_angle", 74.25f, 0.0f, 360.0f);
    final NumberValue<Float> gradientIntensity =
            new NumberValue<>("gradient_intensity", 0.6f, 0.0f, 1.0f);
    final RGBAColorValue customBg =
            new RGBAColorValue("custom_bg", "#FF1B1B1B");
    final RGBAColorValue customBgHover =
            new RGBAColorValue("custom_bg_hover", "#FF2A2A2A");
    final RGBAColorValue customBgDisabled =
            new RGBAColorValue("custom_bg_disabled", "#FF141414");
    final RGBAColorValue customStroke =
            new RGBAColorValue("custom_stroke", "#FF9B5DB8");
    final RGBAColorValue customStrokeHover =
            new RGBAColorValue("custom_stroke_hover", "#FFA710B2");
    final RGBAColorValue customStrokeDisabled =
            new RGBAColorValue("custom_stroke_disabled", "#FF2A2A2A");
    final RGBAColorValue customText =
            new RGBAColorValue("custom_text", "#FFECECEC");
    final RGBAColorValue customTextDisabled =
            new RGBAColorValue("custom_text_disabled", "#FF7A7A7A");
    final NumberValue<Float> customRadius =
            new NumberValue<>("custom_radius", 6.0f, 0.0f, 20.0f);
    final NumberValue<Float> customSoftness =
            new NumberValue<>("custom_softness", 1.2f, 0.0f, 4.0f);
    final NumberValue<Float> customStrokeThickness =
            new NumberValue<>("custom_stroke_thickness", 0.7f, 0.1f, 3.0f);
    final BooleanValue customGradient =
            new BooleanValue("custom_gradient", true);
    final RGBAColorValue customGradientColor =
            new RGBAColorValue("custom_gradient_color", "#73521BA7");
    final NumberValue<Float> customGradientAngle =
            new NumberValue<>("custom_gradient_angle", 90.0f, 0.0f, 360.0f);
    final NumberValue<Float> customGradientIntensity =
            new NumberValue<>("custom_gradient_intensity", 0.5f, 0.0f, 1.0f);
    final NumberValue<Float> customTextScale =
            new NumberValue<>("custom_text_scale", 1.0f, 0.6f, 1.6f);
    final NumberValue<Float> customTextPadX =
            new NumberValue<>("custom_text_pad_x", 6.0f, 0.0f, 16.0f);

    void defineSettings(List<SettingDef> defs) {
        defs.add(SettingDef.mode(style));
        defs.add(SettingDef.color(gradientBaseColor).visibleWhen(this::isGradientStyle));
        defs.add(SettingDef.color(gradientColor).visibleWhen(this::isGradientStyle));
        defs.add(SettingDef.number(gradientAngle).visibleWhen(this::isGradientStyle));
        defs.add(SettingDef.number(gradientIntensity).visibleWhen(this::isGradientStyle));

        defs.add(SettingDef.color(customBg).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.color(customBgHover).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.color(customBgDisabled).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.color(customStroke).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.color(customStrokeHover).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.color(customStrokeDisabled).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.color(customText).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.color(customTextDisabled).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.number(customRadius).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.number(customSoftness).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.number(customStrokeThickness).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.bool(customGradient).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.color(customGradientColor).visibleWhen(this::isCustomGradientStyle));
        defs.add(SettingDef.number(customGradientAngle).visibleWhen(this::isCustomGradientStyle));
        defs.add(SettingDef.number(customGradientIntensity).visibleWhen(this::isCustomGradientStyle));
        defs.add(SettingDef.number(customTextScale).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.number(customTextPadX).visibleWhen(this::isCustomStyle));
    }

    private boolean isGradientStyle() {
        return style.get() == BetterButtons.Style.GRADIENT;
    }

    private boolean isCustomStyle() {
        return style.get() == BetterButtons.Style.CUSTOM;
    }

    private boolean isCustomGradientStyle() {
        return isCustomStyle() && customGradient.get();
    }
}
