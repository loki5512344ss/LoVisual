/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.text.font.info;
import dev.loki.lovisual.render.engine.text.backend.placement.TextPlacementMode;

/**
 * Preferred glyph backend. Placement is handled by TextPlacementMode, not by this enum.
 */
public enum TextBackendPreference {
    AUTO,
    VANILLA_SODIUM,
    BITMAP_ATLAS,
    MSDF
}
