package dev.loki.lovisual.features.gui.hud.nondraggable.impl.tooltips;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TooltipColorsTest {
    private static final int ACCENT = 0xFFE69A3C;

    @Test
    void vanillaBluesBecomeTheAccent() {
        assertEquals(ACCENT, TooltipColors.readable(0xFF5555FF, ACCENT));
        assertEquals(ACCENT, TooltipColors.readable(0xFF0000AA, ACCENT));
    }

    @Test
    void otherColorsAreKept() {
        assertEquals(0xFFFFAA00, TooltipColors.readable(0xFFFFAA00, ACCENT));
        assertEquals(0xFFAAAAAA, TooltipColors.readable(0xFFAAAAAA, ACCENT));
    }
}
