package dev.loki.lovisual.features.module.modules.visuals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.loki.lovisual.features.module.modules.visuals.weather.sky.AtmoDawnFog;

/**
 * Чистая математика AtmoDawnFog: окно времени суток и высотное затухание.
 */
public final class AtmoDawnFogTest {

    private static final float EPS = 1e-4f;

    @Test
    void dawnPeaksAtSunrise() {
        assertEquals(1.0f, AtmoDawnFog.strengthFor(0.0f, "Dawn", 64.0f, 76.0f), EPS);
        assertTrue(AtmoDawnFog.strengthFor(0.999f, "Dawn", 64.0f, 76.0f) > 0.98f);
    }

    @Test
    void dawnSilentAtNoonAndDusk() {
        assertEquals(0.0f, AtmoDawnFog.strengthFor(0.25f, "Dawn", 64.0f, 76.0f), EPS);
        assertEquals(0.0f, AtmoDawnFog.strengthFor(0.5f, "Dawn", 64.0f, 76.0f), EPS);
    }

    @Test
    void duskPeaksAtSunset() {
        assertEquals(1.0f, AtmoDawnFog.strengthFor(0.5f, "Dusk", 64.0f, 76.0f), EPS);
        assertEquals(0.0f, AtmoDawnFog.strengthFor(0.0f, "Dusk", 64.0f, 76.0f), EPS);
    }

    @Test
    void customModeIgnoresTime() {
        assertEquals(1.0f, AtmoDawnFog.strengthFor(0.3f, "Custom", 64.0f, 76.0f), EPS);
        assertEquals(1.0f, AtmoDawnFog.strengthFor(0.7f, "Custom", 64.0f, 76.0f), EPS);
    }

    @Test
    void altitudeFadesAboveScatterHeight() {
        float below = AtmoDawnFog.strengthFor(0.0f, "Dawn", 76.0f, 76.0f);
        float above = AtmoDawnFog.strengthFor(0.0f, "Dawn", 76.0f + 16.0f, 76.0f);
        assertEquals(1.0f, below, EPS);
        assertEquals(0.5f, above, EPS);
    }

    @Test
    void altitudeClampsAtZero() {
        assertEquals(0.0f, AtmoDawnFog.strengthFor(0.0f, "Custom", 76.0f + 1000.0f, 76.0f), EPS);
        assertTrue(AtmoDawnFog.strengthFor(0.0f, "Custom", 40.0f, 76.0f) <= 1.0f);
    }
}
