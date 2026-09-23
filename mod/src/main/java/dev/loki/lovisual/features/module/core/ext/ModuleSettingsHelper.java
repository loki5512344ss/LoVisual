package dev.loki.lovisual.features.module.core;

import dev.loki.lovisual.config.common.CommonBooleanGroupSchema;
import dev.loki.lovisual.config.common.CommonSettingSchema;
import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.collection.ItemIdSetValue;
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
import dev.loki.lovisual.config.values.primitive.StringValue;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.settings.core.SettingFactory;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.module.ModuleNames;
import dev.loki.lovisual.features.module.module.ModuleSettingValues;

import java.util.*;
import java.util.function.Supplier;

final class ModuleSettingsHelper {
    private final Module owner;
    private final List<SettingDef> declared = new ArrayList<>();
    private final Map<ConfigValue<?>, SettingDef> byValue = new IdentityHashMap<>();

    ModuleSettingsHelper(Module owner) { this.owner = owner; }

    List<SettingDef> declared() { return declared; }
    Map<ConfigValue<?>, SettingDef> byValue() { return byValue; }

    void postInit(List<Setting> settings) {
        SettingDef.applyI18nAnnotations(owner, declared);
        settings.addAll(SettingFactory.fromDefs(declared));
        owner.loadSettings(settings);
        for (Setting s : settings) { s.setParent(owner); s.preflightI18n(); }
    }

    private void declare(ConfigValue<?> v, SettingDef d) { declared.add(d); byValue.put(v, d); }
    private String sid(String cn) { return ModuleNames.settingId(cn, owner.name(), owner.getClass().getSimpleName()); }

    BooleanValue bool(String cn, boolean def) { return bool(cn, sid(cn), def); }
    BooleanValue bool(String cn, String sid, boolean def) { return ModuleSettingValues.bool(cn, sid, def, this::declare); }
    NumberValue<Integer> num(String cn, int def, int min, int max) { return num(cn, sid(cn), def, min, max); }
    NumberValue<Integer> num(String cn, String sid, int def, int min, int max) { return ModuleSettingValues.num(cn, sid, def, min, max, this::declare); }
    NumberValue<Long> numL(String cn, long def, long min, long max) { return numL(cn, sid(cn), def, min, max); }
    NumberValue<Long> numL(String cn, String sid, long def, long min, long max) { return ModuleSettingValues.num(cn, sid, def, min, max, this::declare); }
    NumberValue<Float> numF(String cn, float def, float min, float max) { return numF(cn, sid(cn), def, min, max); }
    NumberValue<Float> numF(String cn, String sid, float def, float min, float max) { return ModuleSettingValues.num(cn, sid, def, min, max, this::declare); }
    NumberValue<Double> numD(String cn, double def, double min, double max) { return numD(cn, sid(cn), def, min, max); }
    NumberValue<Double> numD(String cn, String sid, double def, double min, double max) { return ModuleSettingValues.num(cn, sid, def, min, max, this::declare); }
    ModeValue mode(String cn, String def, String... opts) { return modeSetting(cn, sid(cn), def, opts); }
    ModeValue modeSetting(String cn, String sid, String def, String... opts) { return ModuleSettingValues.modeSetting(cn, sid, def, this::declare, opts); }
    <E extends Enum<E>> EnumValue<E> enumMode(String cn, E def) { return enumSetting(cn, sid(cn), def); }
    <E extends Enum<E>> EnumValue<E> enumSetting(String cn, String sid, E def) { return ModuleSettingValues.enumSetting(cn, sid, def, this::declare); }
    @Deprecated <E extends Enum<E>> EnumValue<E> enumModeCl(String cn, E def, Class<E> c) { return enumSettingCl(cn, sid(cn), def, c); }
    @Deprecated <E extends Enum<E>> EnumValue<E> enumSettingCl(String cn, String sid, E def, Class<E> c) { return ModuleSettingValues.enumSetting(cn, sid, def, c, this::declare); }
    @SafeVarargs final <E extends Enum<E>> EnumValue<E> enumModeArr(String cn, E def, E... opts) { return enumSettingArr(cn, sid(cn), def, opts); }
    @SafeVarargs final <E extends Enum<E>> EnumValue<E> enumSettingArr(String cn, String sid, E def, E... opts) { return ModuleSettingValues.enumSetting(cn, sid, def, this::declare, opts); }
    RGBAColorValue color(String cn, String hex) { return color(cn, sid(cn), hex); }
    RGBAColorValue color(String cn, String sid, String hex) { return ModuleSettingValues.color(cn, sid, hex, this::declare); }
    RGBColorValue colorNoAlpha(String cn, String hex) { return colorNoAlpha(cn, sid(cn), hex); }
    RGBColorValue colorNoAlpha(String cn, String sid, String hex) { return ModuleSettingValues.colorNoAlpha(cn, sid, hex, this::declare); }
    StringValue text(String cn, String def) { return text(cn, sid(cn), def); }
    StringValue text(String cn, String sid, String def) { return ModuleSettingValues.text(cn, sid, def, this::declare); }
    SetValue textList(String cn) { return textList(cn, sid(cn), TextListSetting.PickerMode.ALL); }
    SetValue textList(String cn, TextListSetting.PickerMode m) { return textList(cn, sid(cn), m); }
    SetValue textList(String cn, String sid, TextListSetting.PickerMode m) { return ModuleSettingValues.textList(cn, sid, m, this::declare); }
    SetValue textList(String cn, String sid, TextListSetting.PickerMode m, Set<String> defs) { return ModuleSettingValues.textList(cn, sid, m, defs, this::declare); }
    ItemIdSetValue itemList(String cn) { return itemList(cn, sid(cn), TextListSetting.PickerMode.ITEMS); }
    ItemIdSetValue itemList(String cn, TextListSetting.PickerMode m) { return itemList(cn, sid(cn), m); }
    ItemIdSetValue itemList(String cn, String sid, TextListSetting.PickerMode m) { return ModuleSettingValues.itemList(cn, sid, m, this::declare); }
    BooleanMapValue group(String cn, Map<String, Boolean> defs) { return group(cn, sid(cn), defs); }
    BooleanMapValue group(String cn, String sid, Map<String, Boolean> defs) { return ModuleSettingValues.group(cn, sid, defs, this::declare); }
    KeyBindValue bind(String cn, String def, BindMode mode) { return bind(cn, sid(cn), def, mode); }
    KeyBindValue bind(String cn, String sid, String def, BindMode mode) { return ModuleSettingValues.bind(cn, sid, def, mode, this::declare); }
    BooleanMapValue groupCommon(String cn, String key, Map<String, Boolean> defs) { return groupCommon(cn, sid(cn), key, defs); }
    BooleanMapValue groupCommon(String cn, String sid, String key, Map<String, Boolean> defs) { return common(group(cn, sid, defs), key); }
    ModeValue modeCommon(String cn, String key, String def, String... opts) { return modeCommon(cn, sid(cn), key, def, opts); }
    ModeValue modeCommon(String cn, String sid, String key, String def, String... opts) { return common(modeSetting(cn, sid, def, opts), key); }
    <E extends Enum<E>> EnumValue<E> enumCommon(String cn, String key, E def) { return enumCommon(cn, sid(cn), key, def); }
    <E extends Enum<E>> EnumValue<E> enumCommon(String cn, String sid, String key, E def) { return common(enumSetting(cn, sid, def), key); }
    <E extends Enum<E>> EnumValue<E> enumCommonCl(String cn, String key, E def, Class<E> c) { return enumCommonCl(cn, sid(cn), key, def, c); }
    <E extends Enum<E>> EnumValue<E> enumCommonCl(String cn, String sid, String key, E def, Class<E> c) { return common(enumSettingCl(cn, sid, def, c), key); }
    @SafeVarargs final <E extends Enum<E>> EnumValue<E> enumCommonArr(String cn, String key, E def, E... opts) { return enumCommonArr(cn, sid(cn), key, def, opts); }
    @SafeVarargs final <E extends Enum<E>> EnumValue<E> enumCommonArr(String cn, String sid, String key, E def, E... opts) { return common(enumSettingArr(cn, sid, def, opts), key); }
    NumberValue<Integer> numCommon(String cn, String key, int def, int min, int max) { return numCommon(cn, sid(cn), key, def, min, max); }
    NumberValue<Integer> numCommon(String cn, String sid, String key, int def, int min, int max) { return common(num(cn, sid, def, min, max), key); }
    NumberValue<Float> numCommonF(String cn, String key, float def, float min, float max) { return numCommonF(cn, sid(cn), key, def, min, max); }
    NumberValue<Float> numCommonF(String cn, String sid, String key, float def, float min, float max) { return common(numF(cn, sid, def, min, max), key); }
    NumberValue<Double> numCommonD(String cn, String key, double def, double min, double max) { return numCommonD(cn, sid(cn), key, def, min, max); }
    NumberValue<Double> numCommonD(String cn, String sid, String key, double def, double min, double max) { return common(numD(cn, sid, def, min, max), key); }
    BooleanValue boolCommon(String cn, String key, boolean def) { return boolCommon(cn, sid(cn), key, def); }
    BooleanValue boolCommon(String cn, String sid, String key, boolean def) { return common(bool(cn, sid, def), key); }
    BooleanMapValue groupCommon(String cn, CommonBooleanGroupSchema s) { return groupCommon(cn, sid(cn), s); }
    BooleanMapValue groupCommon(String cn, String sid, CommonBooleanGroupSchema s) { if (s==null) return group(cn,sid,Map.of()); return common(group(cn,sid,s.defaults()), s); }
    BooleanMapValue protocolHeuristics(String cn, String sid, CommonBooleanGroupSchema s) { return ModuleSettingValues.protocolHeuristics(cn, sid, s, this::declare); }
    <E extends Enum<E>> EnumValue<E> enumCommon(String cn, CommonSettingSchema s, E def) { return enumCommon(cn, sid(cn), s, def); }
    <E extends Enum<E>> EnumValue<E> enumCommon(String cn, String sid, CommonSettingSchema s, E def) { return common(enumSetting(cn, sid, def), s); }
    <E extends Enum<E>> EnumValue<E> enumCommonCl(String cn, CommonSettingSchema s, E def, Class<E> c) { return enumCommonCl(cn, sid(cn), s, def, c); }
    <E extends Enum<E>> EnumValue<E> enumCommonCl(String cn, String sid, CommonSettingSchema s, E def, Class<E> c) { return common(enumSettingCl(cn, sid, def, c), s); }
    @SafeVarargs final <E extends Enum<E>> EnumValue<E> enumCommonArr(String cn, CommonSettingSchema s, E def, E... opts) { return enumCommonArr(cn, sid(cn), s, def, opts); }
    @SafeVarargs final <E extends Enum<E>> EnumValue<E> enumCommonArr(String cn, String sid, CommonSettingSchema s, E def, E... opts) { return common(enumSettingArr(cn, sid, def, opts), s); }
    ModeValue modeCommon(String cn, CommonSettingSchema s, String def, String... opts) { return modeCommon(cn, sid(cn), s, def, opts); }
    ModeValue modeCommon(String cn, String sid, CommonSettingSchema s, String def, String... opts) { return common(modeSetting(cn, sid, def, opts), s); }
    NumberValue<Integer> numCommon(String cn, CommonSettingSchema s, int def, int min, int max) { return numCommon(cn, sid(cn), s, def, min, max); }
    NumberValue<Integer> numCommon(String cn, String sid, CommonSettingSchema s, int def, int min, int max) { return common(num(cn, sid, def, min, max), s); }
    NumberValue<Float> numCommonF(String cn, CommonSettingSchema s, float def, float min, float max) { return numCommonF(cn, sid(cn), s, def, min, max); }
    NumberValue<Float> numCommonF(String cn, String sid, CommonSettingSchema s, float def, float min, float max) { return common(numF(cn, sid, def, min, max), s); }
    NumberValue<Double> numCommonD(String cn, CommonSettingSchema s, double def, double min, double max) { return numCommonD(cn, sid(cn), s, def, min, max); }
    NumberValue<Double> numCommonD(String cn, String sid, CommonSettingSchema s, double def, double min, double max) { return common(numD(cn, sid, def, min, max), s); }
    BooleanValue boolCommon(String cn, CommonSettingSchema s, boolean def) { return boolCommon(cn, sid(cn), s, def); }
    BooleanValue boolCommon(String cn, String sid, CommonSettingSchema s, boolean def) { return common(bool(cn, sid, def), s); }

    <V extends ConfigValue<?>> V visibleWhen(V v, Supplier<Boolean> s) { SettingDef d=byValue.get(v); if(d!=null) d.visibleWhen(s); return v; }
    <V extends ConfigValue<?>> V shownWhen(V v, Supplier<Boolean> s) { return visibleWhen(v,s); }
    <V extends ConfigValue<?>> V unavailableWhen(V v, Supplier<Boolean> s, String r) { SettingDef d=byValue.get(v); if(d!=null) d.unavailableWhen(s,r); return v; }
    <V extends ConfigValue<?>> V unavailableWhen(V v, Supplier<Boolean> s, Supplier<String> r) { SettingDef d=byValue.get(v); if(d!=null) d.unavailableWhen(s,r); return v; }
    <V extends ConfigValue<?>> V notAppliedWhen(V v, Supplier<Boolean> s, String r) { return unavailableWhen(v,s,r); }
    <V extends ConfigValue<?>> V notAppliedWhen(V v, Supplier<Boolean> s, Supplier<String> r) { return unavailableWhen(v,s,r); }
    <V extends ConfigValue<?>> V appliesWhen(V v, Supplier<Boolean> s, String r) { SettingDef d=byValue.get(v); if(d!=null) d.availableWhen(s,r); return v; }
    <V extends ConfigValue<?>> V appliesWhen(V v, Supplier<Boolean> s, Supplier<String> r) { SettingDef d=byValue.get(v); if(d!=null) d.availableWhen(s,r); return v; }
    SettingDef setting(SettingDef d) { if(d!=null&&d.value()!=null) declare(d.value(),d); return d; }
    void addExtension(SettingDef d, List<Setting> settings) { Setting s=SettingFactory.fromDef(d); if(s==null) return; s.setParent(owner); s.preflightI18n(); settings.add(s); if(d!=null&&d.value()!=null) byValue.put(d.value(),d); }
    <V extends ConfigValue<?>> V common(V v, String k) { SettingDef d=byValue.get(v); if(d!=null&&k!=null&&!k.isBlank()) d.common(k); return v; }
    <V extends ConfigValue<?>> V common(V v, CommonSettingSchema s) { SettingDef d=byValue.get(v); if(d!=null&&s!=null) d.common(s); return v; }
}
