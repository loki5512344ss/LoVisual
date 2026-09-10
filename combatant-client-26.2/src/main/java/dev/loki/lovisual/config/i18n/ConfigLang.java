/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.config.i18n;

import net.minecraft.client.resources.language.I18n;
import dev.loki.lovisual.config.values.collection.ConfigValue;

public enum ConfigLang {
    ;

    public static String getName(ConfigValue<?> v) {
        String key = "config." + v.getName() + ".name";
        return translateOrFallback(key, v.getName());
    }

    public static String getDesc(ConfigValue<?> v) {
        String key = "config." + v.getName() + ".desc";
        return translateOrFallback(key, "");
    }

    private static String translateOrFallback(String key, String fallback) {
        String translated = I18n.get(key);
        return translated.equals(key) ? fallback : translated;
    }
}
