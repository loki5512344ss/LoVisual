package dev.loki.lovisual.features.module.modules.visuals.items.armor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ArmorTintTest {
    private static final int FULL = 0x00FF00;
    private static final int WARN = 0xFFFF00;
    private static final int BROKEN = 0xFF0000;

    @Test
    void fractionIsRemainingDurability() {
        assertEquals(1.0f, ArmorTint.fraction(0, 100), 1e-6f);
        assertEquals(0.25f, ArmorTint.fraction(75, 100), 1e-6f);
        assertEquals(0.0f, ArmorTint.fraction(200, 100), 1e-6f);
        assertEquals(-1.0f, ArmorTint.fraction(0, 0), 1e-6f);
    }

    @Test
    void gradientBlendsBetweenStops() {
        assertEquals(FULL, ArmorTint.ramp(1.0f, ArmorTint.GRADIENT, FULL, WARN, BROKEN));
        assertEquals(WARN, ArmorTint.ramp(0.5f, ArmorTint.GRADIENT, FULL, WARN, BROKEN));
        assertEquals(BROKEN, ArmorTint.ramp(0.0f, ArmorTint.GRADIENT, FULL, WARN, BROKEN));
        assertEquals(0x80FF00, ArmorTint.ramp(0.75f, ArmorTint.GRADIENT, FULL, WARN, BROKEN));
        assertEquals(0xFF8000, ArmorTint.ramp(0.25f, ArmorTint.GRADIENT, FULL, WARN, BROKEN));
    }

    @Test
    void stepsSnapToOneStop() {
        assertEquals(FULL, ArmorTint.ramp(0.9f, ArmorTint.STEPS, FULL, WARN, BROKEN));
        assertEquals(WARN, ArmorTint.ramp(0.4f, ArmorTint.STEPS, FULL, WARN, BROKEN));
        assertEquals(WARN, ArmorTint.ramp(0.26f, ArmorTint.STEPS, FULL, WARN, BROKEN));
        assertEquals(BROKEN, ArmorTint.ramp(0.25f, ArmorTint.STEPS, FULL, WARN, BROKEN));
    }

    @Test
    void tintBlendsFromWhiteByStrength() {
        assertEquals(0xFF00FF00, ArmorTint.tint(0x00FF00, 1.0f));
        assertEquals(0xFFFFFFFF, ArmorTint.tint(0x00FF00, 0.0f));
        assertEquals(0xFF80FF80, ArmorTint.tint(0x00FF00, 0.5f));
    }

    @Test
    void applyMultipliesLayerColorAndKeepsNoTint() {
        assertEquals(0xFF804020, ArmorTint.apply(0xFF804020, ArmorTint.NO_TINT));
        assertEquals(0xFF00FF00, ArmorTint.apply(0xFFFFFFFF, 0xFF00FF00));
        assertEquals(0xFF400000, ArmorTint.apply(0xFF800000, 0xFF808080));
    }

    @Test
    void pulseNeverBrightensAndStaysAboveSixtyPercent() {
        for (long ms = 0; ms < 2000; ms += 37) {
            int out = ArmorTint.pulse(0xFF8040, ms);
            assertTrue((out >> 16 & 0xFF) <= 0xFF && (out >> 16 & 0xFF) >= Math.round(0xFF * 0.6f) - 1);
            assertTrue((out >> 8 & 0xFF) <= 0x80);
        }
    }
}
