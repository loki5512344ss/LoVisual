package dev.loki.lovisual.features.gui.hud.base;

import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.collection.SetValue;
import dev.loki.lovisual.config.values.color.KeyBindValue;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.BindMode;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Owns the ordered list of {@link SettingDef}s and the value→def index for a
 * {@link BaseHudElement}, and provides the concrete config-value builders behind the base
 * class' protected settings DSL. Each builder receives an already-resolved setting id.
 * Extracted so the base class stays under the file-size cap.
 */
final class HudSettingRegistry {

    private final List<SettingDef> defs = new ArrayList<>();
    private final Map<ConfigValue<?>, SettingDef> byValue = new IdentityHashMap<>();

    List<SettingDef> defs() {
        return defs;
    }

    BooleanValue bool(String configName, String settingId, boolean def) {
        BooleanValue value = new BooleanValue(configName, def);
        declare(value, SettingDef.bool(settingId, value));
        return value;
    }

    NumberValue<Integer> numInt(String configName, String settingId, int def, int min, int max) {
        NumberValue<Integer> value = new NumberValue<>(configName, def, min, max);
        declare(value, SettingDef.number(settingId, value));
        return value;
    }

    NumberValue<Float> numFloat(String configName, String settingId, float def, float min, float max) {
        NumberValue<Float> value = new NumberValue<>(configName, def, min, max);
        declare(value, SettingDef.number(settingId, value));
        return value;
    }

    NumberValue<Double> numDouble(String configName, String settingId, double def, double min, double max) {
        NumberValue<Double> value = new NumberValue<>(configName, def, min, max);
        declare(value, SettingDef.number(settingId, value));
        return value;
    }

    ModeValue mode(String configName, String settingId, String def, String... options) {
        ModeValue value = new ModeValue(configName, def, options);
        declare(value, SettingDef.mode(settingId, value));
        return value;
    }

    <E extends Enum<E>> EnumValue<E> enumSetting(String configName, String settingId, E def) {
        return enumSetting(configName, settingId, def, def.getDeclaringClass());
    }

    <E extends Enum<E>> EnumValue<E> enumSetting(String configName, String settingId, E def, Class<E> enumClass) {
        EnumValue<E> value = new EnumValue<>(configName, def, enumClass);
        declare(value, SettingDef.mode(settingId, value));
        return value;
    }

    @SafeVarargs
    final <E extends Enum<E>> EnumValue<E> enumSetting(String configName, String settingId, E def, E... options) {
        EnumValue<E> value = new EnumValue<>(configName, def, options);
        declare(value, SettingDef.mode(settingId, value));
        return value;
    }

    RGBAColorValue color(String configName, String settingId, String defHex) {
        RGBAColorValue value = new RGBAColorValue(configName, defHex);
        declare(value, SettingDef.color(settingId, value));
        return value;
    }

    RGBColorValue colorNoAlpha(String configName, String settingId, String defHex) {
        RGBColorValue value = new RGBColorValue(configName, defHex);
        declare(value, SettingDef.colorNoAlpha(settingId, value));
        return value;
    }

    SetValue textList(String configName, String settingId, TextListSetting.PickerMode pickerMode) {
        SetValue value = new SetValue(configName);
        declare(value, SettingDef.textList(settingId, value, pickerMode));
        return value;
    }

    KeyBindValue bind(String configName, String settingId, String def, BindMode mode) {
        KeyBindValue value = new KeyBindValue(configName, def);
        declare(value, SettingDef.bind(settingId, value, mode));
        return value;
    }

    BooleanMapValue group(String configName, String settingId, Map<String, Boolean> defaults) {
        BooleanMapValue value = new BooleanMapValue(configName, defaults);
        declare(value, SettingDef.group(settingId, value));
        return value;
    }

    <V extends ConfigValue<?>> V visibleWhen(V value, Supplier<Boolean> supplier) {
        SettingDef def = byValue.get(value);
        if (def != null) def.visibleWhen(supplier);
        return value;
    }

    <V extends ConfigValue<?>> V common(V value, String commonI18nKey) {
        SettingDef def = byValue.get(value);
        if (def != null && commonI18nKey != null && !commonI18nKey.isBlank()) def.common(commonI18nKey);
        return value;
    }

    SettingDef settingDef(ConfigValue<?> value) {
        return byValue.get(value);
    }

    void declare(ConfigValue<?> value, SettingDef def) {
        defs.add(def);
        byValue.put(value, def);
    }
}
