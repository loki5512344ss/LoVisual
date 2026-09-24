package dev.loki.lovisual.features.gui.clickgui.util;

import net.minecraft.locale.Language;

import java.util.IllegalFormatException;
import java.util.Locale;

public enum ClickGuiI18n {
    ;

    public static String tr(String key, String fallback, Object... args) {
        String pattern = Language.getInstance().getOrDefault(key, fallback);
        if (args == null || args.length == 0) {
            return pattern;
        }
        try {
            return String.format(Locale.ROOT, pattern, args);
        } catch (IllegalFormatException ignored) {
            try {
                return String.format(Locale.ROOT, fallback, args);
            } catch (IllegalFormatException ignoredAgain) {
                return fallback;
            }
        }
    }
}
