package dev.loki.lovisual.render.engine.renderer.ui.runtime.style.token;

import java.util.Locale;

public enum UiJustify {
    START,
    CENTER,
    END,
    BETWEEN;

    public static UiJustify parse(String raw, UiJustify fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        return switch (raw.trim().replace('-', '_').toUpperCase(Locale.ROOT)) {
            case "CENTER", "MIDDLE" -> CENTER;
            case "END", "RIGHT", "BOTTOM" -> END;
            case "BETWEEN", "SPACE_BETWEEN" -> BETWEEN;
            default -> START;
        };
    }
}
