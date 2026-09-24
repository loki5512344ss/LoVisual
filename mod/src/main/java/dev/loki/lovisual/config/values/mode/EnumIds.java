package dev.loki.lovisual.config.values.mode;

import java.util.Locale;

public enum EnumIds {
    ;

    public static String defaultId(Enum<?> value) {
        if (value == null) return "";
        return value.name().toLowerCase(Locale.ROOT);
    }
}
