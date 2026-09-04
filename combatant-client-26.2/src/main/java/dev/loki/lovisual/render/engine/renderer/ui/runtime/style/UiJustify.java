/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.style;

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
