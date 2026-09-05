/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.module;

import dev.loki.lovisual.config.SettingDef;
import dev.loki.lovisual.config.common.CommonBooleanGroupSchema;
import dev.loki.lovisual.config.values.BindMode;
import dev.loki.lovisual.config.values.BooleanMapValue;
import dev.loki.lovisual.config.values.BooleanValue;
import dev.loki.lovisual.config.values.ConfigValue;
import dev.loki.lovisual.config.values.EnumValue;
import dev.loki.lovisual.config.values.ItemIdSetValue;
import dev.loki.lovisual.config.values.KeyBindValue;
import dev.loki.lovisual.config.values.ModeValue;
import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.config.values.RGBAColorValue;
import dev.loki.lovisual.config.values.RGBColorValue;
import dev.loki.lovisual.config.values.SetValue;
import dev.loki.lovisual.config.values.StringValue;
import dev.loki.lovisual.features.gui.clickgui.settings.TextListSetting;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Factory helpers for module setting values. Every factory creates the config value,
 * builds its {@link SettingDef} and hands both to the caller's declare callback so the
 * owning module can register them exactly as before.
 */
public final class ModuleSettingValues {

    private ModuleSettingValues() {
    }

    public static BooleanValue bool(String configName, String settingId, boolean def, BiConsumer<ConfigValue<?>, SettingDef> declare) {
        BooleanValue value = new BooleanValue(configName, def);
        declare.accept(value, SettingDef.bool(settingId, value));
        return value;
    }

    public static NumberValue<Integer> num(String configName, String settingId, int def, int min, int max, BiConsumer<ConfigValue<?>, SettingDef> declare) {
        NumberValue<Integer> value = new NumberValue<>(configName, def, min, max);
        declare.accept(value, SettingDef.number(settingId, value));
        return value;
    }

    public static NumberValue<Long> num(String configName, String settingId, long def, long min, long max, BiConsumer<ConfigValue<?>, SettingDef> declare) {
        NumberValue<Long> value = new NumberValue<>(configName, def, min, max);
        declare.accept(value, SettingDef.number(settingId, value));
        return value;
    }

    public static NumberValue<Float> num(String configName, String settingId, float def, float min, float max, BiConsumer<ConfigValue<?>, SettingDef> declare) {
        NumberValue<Float> value = new NumberValue<>(configName, def, min, max);
        declare.accept(value, SettingDef.number(settingId, value));
        return value;
    }

    public static NumberValue<Double> num(String configName, String settingId, double def, double min, double max, BiConsumer<ConfigValue<?>, SettingDef> declare) {
        NumberValue<Double> value = new NumberValue<>(configName, def, min, max);
        declare.accept(value, SettingDef.number(settingId, value));
        return value;
    }

    public static ModeValue modeSetting(String configName, String settingId, String def, BiConsumer<ConfigValue<?>, SettingDef> declare, String... options) {
        ModeValue value = new ModeValue(configName, def, options);
        declare.accept(value, SettingDef.mode(settingId, value));
        return value;
    }

    public static <E extends Enum<E>> EnumValue<E> enumSetting(String configName, String settingId, E def, BiConsumer<ConfigValue<?>, SettingDef> declare) {
        Class<E> enumClass = def.getDeclaringClass();
        EnumValue<E> value = new EnumValue<>(configName, def, enumClass);
        declare.accept(value, SettingDef.mode(settingId, value));
        return value;
    }

    public static <E extends Enum<E>> EnumValue<E> enumSetting(String configName, String settingId, E def, Class<E> enumClass, BiConsumer<ConfigValue<?>, SettingDef> declare) {
        EnumValue<E> value = new EnumValue<>(configName, def, enumClass);
        declare.accept(value, SettingDef.mode(settingId, value));
        return value;
    }

    @SafeVarargs
    public static <E extends Enum<E>> EnumValue<E> enumSetting(String configName, String settingId, E def, BiConsumer<ConfigValue<?>, SettingDef> declare, E... options) {
        EnumValue<E> value = new EnumValue<>(configName, def, options);
        declare.accept(value, SettingDef.mode(settingId, value));
        return value;
    }

    public static RGBAColorValue color(String configName, String settingId, String defHex, BiConsumer<ConfigValue<?>, SettingDef> declare) {
        RGBAColorValue value = new RGBAColorValue(configName, defHex);
        declare.accept(value, SettingDef.color(settingId, value));
        return value;
    }

    public static RGBColorValue colorNoAlpha(String configName, String settingId, String defHex, BiConsumer<ConfigValue<?>, SettingDef> declare) {
        RGBColorValue value = new RGBColorValue(configName, defHex);
        declare.accept(value, SettingDef.colorNoAlpha(settingId, value));
        return value;
    }

    public static StringValue text(String configName, String settingId, String def, BiConsumer<ConfigValue<?>, SettingDef> declare) {
        StringValue value = new StringValue(configName, def);
        declare.accept(value, SettingDef.text(settingId, value));
        return value;
    }

    public static SetValue textList(String configName, String settingId, TextListSetting.PickerMode pickerMode, BiConsumer<ConfigValue<?>, SettingDef> declare) {
        SetValue value = new SetValue(configName);
        declare.accept(value, SettingDef.textList(settingId, value, pickerMode));
        return value;
    }

    public static SetValue textList(String configName, String settingId, TextListSetting.PickerMode pickerMode, Set<String> defaults, BiConsumer<ConfigValue<?>, SettingDef> declare) {
        SetValue value = new SetValue(configName, defaults);
        declare.accept(value, SettingDef.textList(settingId, value, pickerMode));
        return value;
    }

    public static ItemIdSetValue itemList(String configName, String settingId, TextListSetting.PickerMode pickerMode, BiConsumer<ConfigValue<?>, SettingDef> declare) {
        ItemIdSetValue value = new ItemIdSetValue(configName);
        declare.accept(value, SettingDef.textList(settingId, value, pickerMode));
        return value;
    }

    public static BooleanMapValue group(String configName, String settingId, Map<String, Boolean> defaults, BiConsumer<ConfigValue<?>, SettingDef> declare) {
        BooleanMapValue value = new BooleanMapValue(configName, defaults);
        declare.accept(value, SettingDef.group(settingId, value));
        return value;
    }

    public static KeyBindValue bind(String configName, String settingId, String def, BindMode mode, BiConsumer<ConfigValue<?>, SettingDef> declare) {
        KeyBindValue value = new KeyBindValue(configName, def);
        declare.accept(value, SettingDef.bind(settingId, value, mode));
        return value;
    }

    public static BooleanMapValue protocolHeuristics(String configName,
                                                     String settingId,
                                                     CommonBooleanGroupSchema schema,
                                                     BiConsumer<ConfigValue<?>, SettingDef> declare) {
        Map<String, Boolean> defaults = schema == null ? Map.of() : schema.defaults();
        BooleanMapValue value = new BooleanMapValue(configName, defaults);
        SettingDef def = SettingDef.protocolHeuristics(settingId, value);
        if (schema != null) def.common(schema);
        declare.accept(value, def);
        return value;
    }
}
