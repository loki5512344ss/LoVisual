package dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars;

import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.collection.SetValue;
import dev.loki.lovisual.config.values.color.KeyBindValue;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.BindMode;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.primitive.StringValue;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.HudNotifier;
import dev.loki.lovisual.util.input.KeyManager;

import java.util.List;

import static dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars.CustomBarModel.COLOR_CUSTOM;
import static dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars.CustomBarModel.COLOR_THEME;
import static dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars.CustomBarModel.XP_TEXT_SCALE;

/**
 * Settings schema and live value holders for {@link CustomBar}.
 * Declared in the {@code bars} package (impl subfolder) so the facade can read
 * these package-private fields directly from its getters.
 */
final class BarSettings {

    final NumberValue<Integer> overlayAlpha =
            new NumberValue<>("overlay_alpha", 255, 30, 255);
    final BooleanValue xpBar =
            new BooleanValue("xp_bar", true);
    final BooleanValue xpBarShowLevel =
            new BooleanValue("xp_bar_show_level", true);
    final EnumValue<CustomBarModel.BarMode> barMode =
            new EnumValue<>("bar_mode", CustomBarModel.BarMode.LEVEL, CustomBarModel.BarMode.values());
    final ModeValue colorMode =
            new ModeValue("bar_color_mode", "Theme", COLOR_THEME, COLOR_CUSTOM);
    final RGBColorValue xpBarBgColor =
            new RGBColorValue("xp_bar_bg_color", "#151515");
    final RGBColorValue xpBarFillStart =
            new RGBColorValue("xp_bar_fill_start", "#4CB2FF");
    final RGBColorValue xpBarFillEnd =
            new RGBColorValue("xp_bar_fill_end", "#76F0FF");
    final RGBColorValue xpBarTextColor =
            new RGBColorValue("xp_bar_text_color", "#A0E5FF");
    final EnumValue<CustomBarModel.XpBarStyle> xpBarStyle =
            new EnumValue<>("xp_bar_style", CustomBarModel.XpBarStyle.GRADIENT, CustomBarModel.XpBarStyle.values());
    final RGBAColorValue customBgColor =
            new RGBAColorValue("custom_bg_color", "#88151515");
    final RGBAColorValue customFillColor =
            new RGBAColorValue("custom_fill_color", "#CC4CB2FF");
    final BooleanValue customFillGradient =
            new BooleanValue("custom_fill_gradient", true);
    final RGBAColorValue customFillGradientColor =
            new RGBAColorValue("custom_fill_gradient_color", "#CC76F0FF");
    final NumberValue<Float> customFillGradientAngle =
            new NumberValue<>("custom_fill_gradient_angle", 0.0f, 0.0f, 360.0f);
    final RGBAColorValue customTextColor =
            new RGBAColorValue("custom_text_color", "#FFA0E5FF");
    final NumberValue<Float> customTextScale =
            new NumberValue<>("custom_text_scale", (float) XP_TEXT_SCALE, 0.3f, 1.2f);
    final BooleanValue locatorLabels =
            new BooleanValue("locator_labels", true);
    final EnumValue<CustomBarModel.LocatorLabelMode> locatorLabelMode =
            new EnumValue<>("locator_label_mode", CustomBarModel.LocatorLabelMode.CENTER, CustomBarModel.LocatorLabelMode.values());
    final NumberValue<Float> locatorLabelAngle =
            new NumberValue<>("locator_label_angle", 6.0f, 4.0f, 45.0f);
    final BooleanValue locatorLabelDistance =
            new BooleanValue("locator_label_distance", true);
    final BooleanValue locatorSearchEnabled =
            new BooleanValue("locator_search_enabled", true);
    final SetValue locatorSearchPlayers =
            new SetValue("locator_search_list", new java.util.LinkedHashSet<>(java.util.List.of("imhorny")));
    final RGBColorValue locatorSearchColor =
            new RGBColorValue("locator_search_color", "#FFAA00");
    final RGBColorValue locatorDimmedColor =
            new RGBColorValue("locator_dimmed_color", "#777777");
    final BooleanValue locatorDimmedScaleOnly =
            new BooleanValue("locator_dimmed_scale_only", true);
    final BooleanValue locatorMapFilterEnabled =
            new BooleanValue("locator_map_filter_enabled", false);
    final StringValue locatorMapServer =
            new StringValue("locator_map_server", "");
    final StringValue locatorMapUrl =
            new StringValue("locator_map_url", "example");
    final KeyBindValue locatorTargetToggle =
            new KeyBindValue("locator_target_toggle", "NONE");
    final KeyBindValue locatorBarToggle =
            new KeyBindValue("locator_bar_toggle", "U");

    BarSettings() {
    }

    void defineSettings(List<SettingDef> defs) {
        defs.add(SettingDef.bool(xpBar));
        defs.add(SettingDef.bool(xpBarShowLevel).visibleWhen(xpBar::get));
        defs.add(SettingDef.mode(barMode).visibleWhen(xpBar::get));
        defs.add(SettingDef.mode(colorMode).visibleWhen(xpBar::get));
        defs.add(SettingDef.color(customBgColor).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.color(customFillColor).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.color(customFillGradientColor).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.color(customTextColor).visibleWhen(this::isCustomMode));

        defs.add(SettingDef.bool(locatorLabels).visibleWhen(this::locatorModeEnabled));
        defs.add(SettingDef.mode(locatorLabelMode)
                .visibleWhen(() -> locatorModeEnabled() && locatorLabels.get()));
        defs.add(SettingDef.number(locatorLabelAngle)
                .visibleWhen(() -> locatorModeEnabled()
                        && locatorLabels.get()
                        && locatorLabelModeUsesCenter()));
        defs.add(SettingDef.bool(locatorLabelDistance)
                .visibleWhen(() -> locatorModeEnabled() && locatorLabels.get()));

        defs.add(SettingDef.bool(locatorSearchEnabled).visibleWhen(this::locatorModeEnabled));
        defs.add(SettingDef.textList(locatorSearchPlayers)
                .visibleWhen(() -> locatorModeEnabled() && locatorSearchEnabled.get()));
        defs.add(SettingDef.colorNoAlpha(locatorDimmedColor)
                .visibleWhen(() -> locatorModeEnabled()
                        && locatorSearchEnabled.get()
                        && !locatorSearchPlayers.get().isEmpty()));
        defs.add(SettingDef.colorNoAlpha(locatorSearchColor)
                .visibleWhen(() -> locatorModeEnabled()
                        && locatorSearchEnabled.get()
                        && !locatorSearchPlayers.get().isEmpty()));
        defs.add(SettingDef.bool(locatorDimmedScaleOnly).visibleWhen(this::locatorModeEnabled));
        defs.add(SettingDef.bool(locatorMapFilterEnabled).visibleWhen(this::locatorModeEnabled));
        defs.add(SettingDef.text(locatorMapServer)
                .visibleWhen(() -> locatorModeEnabled() && locatorMapFilterEnabled.get()));
        defs.add(SettingDef.text(locatorMapUrl)
                .visibleWhen(() -> locatorModeEnabled() && locatorMapFilterEnabled.get()));

        defs.add(SettingDef.bind(locatorTargetToggle, BindMode.PRESS).visibleWhen(this::locatorModeEnabled));
        defs.add(SettingDef.bind(locatorBarToggle, BindMode.PRESS).visibleWhen(this::locatorModeEnabled));
    }

    void registerBinds(CustomBar bar) {
        registerBind(bar, locatorTargetToggle);
        registerBind(bar, locatorBarToggle);
    }

    void handleToggles(CustomBar bar) {
        if (KeyManager.wasPressed(bindingName(bar, locatorTargetToggle))) {
            boolean next = !locatorSearchEnabled.get();
            locatorSearchEnabled.set(next);
            HudNotifier.pushState("Locator target", next);
            bar.saveConfig();
        }

        if (KeyManager.wasPressed(bindingName(bar, locatorBarToggle))) {
            CustomBarModel.BarMode mode = barMode.get();
            if (mode != CustomBarModel.BarMode.AUTO) {
                barMode.set(mode == CustomBarModel.BarMode.LOCATOR ? CustomBarModel.BarMode.LEVEL : CustomBarModel.BarMode.LOCATOR);
                bar.saveConfig();
            }
        }
    }

    boolean isCustomMode() {
        return COLOR_CUSTOM.equals(colorMode.get());
    }

    boolean locatorModeEnabled() {
        if (!xpBar.get()) return false;
        return barMode.get() != CustomBarModel.BarMode.LEVEL;
    }

    boolean isCustomXpStyleEnabled() {
        return xpBar.get() && isCustomMode();
    }

    private boolean locatorLabelModeUsesCenter() {
        CustomBarModel.LocatorLabelMode mode = locatorLabelMode.get();
        return mode == CustomBarModel.LocatorLabelMode.CENTER || mode == CustomBarModel.LocatorLabelMode.CENTER_TARGETS;
    }

    private String bindingName(CustomBar bar, KeyBindValue value) {
        return bar.name() + ":" + value.getName();
    }

    private void registerBind(CustomBar bar, KeyBindValue value) {
        String name = bindingName(bar, value);
        KeyManager.unregisterAll(name);
        if (!value.isNone()) {
            KeyManager.registerCombo(name, value.get());
        }
    }
}
