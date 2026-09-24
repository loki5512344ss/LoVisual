package dev.loki.lovisual.render.engine.renderer.ui.runtime.style.token;

import java.util.Locale;

public enum UiAlign {
    START,
    CENTER,
    END,
    STRETCH;

    public static UiAlign parse(String raw, UiAlign fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        return switch (raw.trim().replace('-', '_').toUpperCase(Locale.ROOT)) {
            case "CENTER", "MIDDLE" -> CENTER;
            case "END", "RIGHT", "BOTTOM" -> END;
            case "STRETCH", "FILL" -> STRETCH;
            default -> START;
        };
    }
}
