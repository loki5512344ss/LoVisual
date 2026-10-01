package dev.loki.lovisual.features.module.modules.combat.targetesp.constellation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConstellationLayoutTest {
    @Test
    void starsStayWithinOrbitAndAroundBodyHeight() {
        int n = 12;
        double[] p = new double[n * 3];
        ConstellationLayout.positions(p, n, 0.6f, 1.8f, 1.1f, 3.7, 1.0f);
        double maxR = 1.1 * 0.6 * 1.0;
        for (int i = 0; i < n; i++) {
            assertTrue(Math.hypot(p[i * 3], p[i * 3 + 2]) <= maxR + 1e-9);
            assertEquals(0.9, p[i * 3 + 1], 0.9);
        }
    }

    @Test
    void spinMovesStars() {
        double[] a = new double[9];
        double[] b = new double[9];
        ConstellationLayout.positions(a, 3, 0.6f, 1.8f, 1.0f, 0.0, 1.0f);
        ConstellationLayout.positions(b, 3, 0.6f, 1.8f, 1.0f, 1.0, 1.0f);
        assertNotEquals(a[0], b[0]);
    }

    @Test
    void pulseStaysInRange() {
        for (int s = 0; s < 9; s++) {
            float v = ConstellationLayout.pulse(12.3, s, 9);
            assertTrue(v >= 0.5f && v <= 1.0f);
        }
    }
}
