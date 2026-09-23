/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings.core;

import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.collection.ItemCooldownRulesValue;
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
import dev.loki.lovisual.features.gui.clickgui.settings.bind.FunctionBindSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.bool.BooleanSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.color.ColorSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.cooldown.CooldownRulesSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.cooldown.ProtocolHeuristicsSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.group.GroupSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.mode.ModeSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.slider.SliderSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.text.TextSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;

/**
 * Builds GUI settings from GUI-neutral SettingDef.
 */
public enum SettingFactory {
    ;

    public static Setting fromDef(SettingDef def) {
        if (def == null) return null;
        Setting setting = switch (def.kind()) {
            case BOOLEAN -> def.value() instanceof BooleanValue v
                    ? new BooleanSetting(def.getId(), v) : null;
            case NUMBER -> def.value() instanceof NumberValue<?> v
                    ? new SliderSetting<>(def.getId(), v) : null;
            case MODE -> def.value() instanceof ModeValue v
                    ? new ModeSetting(def.getId(), v)
                    : def.value() instanceof EnumValue<?> ev
                    ? new ModeSetting(def.getId(), ev)
                    : null;
            case COLOR -> def.value() instanceof RGBAColorValue v
                    ? new ColorSetting(def.getId(), v) : null;
            case COLOR_NO_ALPHA -> def.value() instanceof RGBColorValue v
                    ? new ColorSetting(def.getId(), v) : null;
            case TEXT -> def.value() instanceof StringValue v
                    ? new TextSetting(def.getId(), v) : null;
            case TEXT_LIST -> def.value() instanceof SetValue v
                    ? new TextListSetting(def.getId(), v, def.pickerMode())
                    : def.value() instanceof ItemIdSetValue iv
                    ? new TextListSetting(def.getId(), iv, def.pickerMode())
                    : null;
            case COOLDOWN_RULES -> def.value() instanceof ItemCooldownRulesValue v
                    ? new CooldownRulesSetting(def.getId(), v) : null;
            case PROTOCOL_HEURISTICS -> def.value() instanceof BooleanMapValue v
                    ? new ProtocolHeuristicsSetting(def.getId(), v) : null;
            case GROUP -> def.value() instanceof BooleanMapValue v
                    ? new GroupSetting(def.getId(), v) : null;
            case BIND -> def.value() instanceof KeyBindValue v
                    ? new FunctionBindSetting(def.getId(), v, def.bindMode() != null ? def.bindMode() : BindMode.PRESS)
                    : null;
        };
        if (setting != null) {
            setting.setCommonI18nKeys(def.commonI18nKeys());
            setting.setI18nEnabled(def.i18nNameEnabled(), def.i18nOptionsEnabled());
            setting.unavailableReason(def.unavailableReason());

            if (def.visibility() != null) {
                setting.visibleWhen(def.visibility());
            }
        }

        return setting;
    }

    public static java.util.List<Setting> fromDefs(java.util.List<SettingDef> defs) {
        if (defs == null || defs.isEmpty()) return java.util.List.of();
        java.util.List<Setting> out = new java.util.ArrayList<>();
        for (SettingDef def : defs) {
            Setting setting = fromDef(def);
            if (setting != null) out.add(setting);
        }
        return out;
    }
}
