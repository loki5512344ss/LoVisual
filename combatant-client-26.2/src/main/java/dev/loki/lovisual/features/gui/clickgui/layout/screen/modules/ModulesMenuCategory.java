/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.layout.screen.modules;

import dev.loki.lovisual.features.module.ModuleCategory;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum ModulesMenuCategory {
    COMBAT("Combat", "swords", "combat"),
    MOVEMENT("Movement", "accessibility", "movement"),
    PLAYER("Player", "user", "player"),
    VISUALS("Visuals", "tree-pine", "visual", "render", "visuals"),
    OTHER("Other", "ellipsis", "other", "misc", "miscellaneous", "exploit", "world");

    private final String title;
    private final String icon;
    private final List<String> aliases;

    ModulesMenuCategory(String title, String icon, String... aliases) {
        this.title = title;
        this.icon = icon;
        this.aliases = Arrays.asList(aliases);
    }

    public String title() {
        return title;
    }

    public String icon() {
        return icon;
    }

    boolean matches(ModuleCategory category) {
        if (category == null) return this == OTHER;

        String name = category.name().toLowerCase(Locale.ROOT);
        for (String alias : aliases) {
            if (name.equals(alias) || name.contains(alias)) return true;
        }

        if (this == OTHER) {
            for (ModulesMenuCategory c : values()) {
                if (c == OTHER) continue;
                if (c.matches(category)) return false;
            }
            return true;
        }

        return false;
    }
}
