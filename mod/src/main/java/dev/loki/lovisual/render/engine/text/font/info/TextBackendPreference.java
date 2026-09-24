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
