/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.relations;

import dev.loki.lovisual.config.ConfigNameProvider;
import dev.loki.lovisual.config.ConfigObject;
import dev.loki.lovisual.config.ConfigSerializer;
import dev.loki.lovisual.config.values.BooleanValue;
import dev.loki.lovisual.config.values.ConfigValue;
import dev.loki.lovisual.config.values.SetValue;
import dev.loki.lovisual.util.text.LegacyTextUtil;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class StaffHeuristicsConfig implements ConfigObject, ConfigNameProvider {
    public static final StaffHeuristicsConfig INSTANCE = new StaffHeuristicsConfig();

    private final BooleanValue enabled = new BooleanValue("enabled", false);
    private final SetValue prefixes = new SetValue("prefixes");
    private final SetValue suffixes = new SetValue("suffixes");
    private final SetValue contains = new SetValue("contains");

    private StaffHeuristicsConfig() {
        ConfigSerializer.load(this);
    }

    public static StaffHeuristicsConfig get() {
        return INSTANCE;
    }

    public boolean enabled() {
        return enabled.get();
    }

    public void setEnabled(boolean value) {
        enabled.set(value);
        save();
    }

    public Set<String> prefixes() {
        return prefixes.get();
    }

    public Set<String> suffixes() {
        return suffixes.get();
    }

    public Set<String> contains() {
        return contains.get();
    }

    public SetValue prefixesValue() {
        return prefixes;
    }

    public SetValue suffixesValue() {
        return suffixes;
    }

    public SetValue containsValue() {
        return contains;
    }

    public boolean addPrefix(String value) {
        return add(prefixes, value);
    }

    public boolean addSuffix(String value) {
        return add(suffixes, value);
    }

    public boolean addContains(String value) {
        return add(contains, value);
    }

    public boolean removePrefix(String value) {
        return remove(prefixes, value);
    }

    public boolean removeSuffix(String value) {
        return remove(suffixes, value);
    }

    public boolean removeContains(String value) {
        return remove(contains, value);
    }

    public boolean matches(String name) {
        return matches(name, null);
    }

    public boolean matches(String name, Component displayName) {
        if (!enabled()) return false;
        Component convertedDisplay = displayName == null ? null : LegacyTextUtil.convertLegacyCodes(displayName);
        String display = convertedDisplay == null ? "" : convertedDisplay.getString();
        return StaffHeuristicsMatcher.matches(name, display, prefixes.get(), suffixes.get(), contains.get());
    }

    public void save() {
        ConfigSerializer.requestSave(this);
    }

    @Override
    public String getConfigName() {
        return "staffheuristics";
    }

    @Override
    public List<ConfigValue<?>> getConfigValues() {
        List<ConfigValue<?>> out = new ArrayList<>();
        out.add(enabled);
        out.add(prefixes);
        out.add(suffixes);
        out.add(contains);
        return out;
    }

    private static boolean add(SetValue target, String raw) {
        String value = cleanRule(raw);
        if (value.isEmpty()) return false;
        boolean changed = target.get().add(value);
        if (changed) StaffHeuristicsConfig.get().save();
        return changed;
    }

    private static boolean remove(SetValue target, String raw) {
        if (raw == null || raw.isBlank()) return false;
        String selected = null;
        for (String entry : target.get()) {
            if (entry != null && entry.equalsIgnoreCase(raw.trim())) {
                selected = entry;
                break;
            }
        }
        boolean changed = selected != null && target.get().remove(selected);
        if (changed) StaffHeuristicsConfig.get().save();
        return changed;
    }

    public static String cleanRule(String raw) {
        return raw == null ? "" : LegacyTextUtil.stripLegacy(raw).trim();
    }

}
