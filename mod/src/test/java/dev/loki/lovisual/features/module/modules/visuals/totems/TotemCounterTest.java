package dev.loki.lovisual.features.module.modules.visuals.totems;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class TotemCounterTest {

    @Test
    void labelShowsCount() {
        assertEquals("x3", TotemCounter.label(3, true));
        assertEquals("x3", TotemCounter.label(3, false));
    }

    @Test
    void zeroIsHiddenWhenDisabled() {
        assertEquals("x0", TotemCounter.label(0, true));
        assertNull(TotemCounter.label(0, false));
    }

    @Test
    void negativeCountNeverLeaksIntoLabel() {
        assertEquals("x0", TotemCounter.label(-4, true));
    }

    @Test
    void colorTurnsRedAtThreshold() {
        assertEquals(0xFFFFFFFF, TotemCounter.color(3, 2));
        assertEquals(0xFFFF5555, TotemCounter.color(2, 2));
        assertEquals(0xFFFF5555, TotemCounter.color(0, 2));
        assertEquals(0xFFFFFFFF, TotemCounter.color(1, 0));
        assertEquals(0xFFFF5555, TotemCounter.color(0, 0));
    }
}
