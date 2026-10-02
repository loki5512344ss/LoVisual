package dev.loki.lovisual.features.module.modules.visuals.weather.blizzard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Спецификация метели: сколько снежинок, как они падают, куда их сносит ветер. */
class SnowFlakesTest {

    private static final SnowFlakes.Wind CALM = SnowFlakes.Wind.ZERO;

    private static double meanY(SnowFlakes f) {
        double sum = 0;
        for (int i = 0; i < f.count; i++) sum += f.y[i];
        return sum / f.count;
    }

    private static double meanX(SnowFlakes f) {
        double sum = 0;
        for (int i = 0; i < f.count; i++) sum += f.x[i];
        return sum / f.count;
    }

    // ---- сколько снежинок

    @Test
    void noStormMeansNoFlakes() {
        assertEquals(0, SnowFlakes.targetCount(0f, 1f, 1f));
        assertEquals(0, SnowFlakes.targetCount(14f, 0f, 1f));
        assertEquals(0, SnowFlakes.targetCount(14f, 1f, 0f));
        assertEquals(0, SnowFlakes.targetCount(-3f, 1f, 1f));
    }

    @Test
    void moreRadiusDensityOrStrengthMeansMoreFlakes() {
        assertTrue(SnowFlakes.targetCount(20f, 1f, 0.5f) > SnowFlakes.targetCount(10f, 1f, 0.5f));
        assertTrue(SnowFlakes.targetCount(10f, 2f, 0.5f) > SnowFlakes.targetCount(10f, 1f, 0.5f));
        assertTrue(SnowFlakes.targetCount(10f, 1f, 0.8f) > SnowFlakes.targetCount(10f, 1f, 0.4f));
    }

    @Test
    void halfTheIntensityGivesAboutHalfTheFlakes() {
        double full = SnowFlakes.targetCount(10f, 1f, 1.0f);
        double half = SnowFlakes.targetCount(10f, 1f, 0.5f);
        assertEquals(0.5, half / full, 0.02);
    }

    @Test
    void flakeCountIsCappedAndStrengthAboveOneChangesNothing() {
        assertEquals(SnowFlakes.MAX_FLAKES, SnowFlakes.targetCount(32f, 3f, 1f));
        assertEquals(SnowFlakes.targetCount(10f, 1f, 1f), SnowFlakes.targetCount(10f, 1f, 7f));
    }

    @Test
    void activeFlakeCountFollowsTheTargetUpAndDown() {
        SnowFlakes f = new SnowFlakes();
        f.update(0.016f, 0, 64, 0, 14f, CALM, 0f, 1f, 500, 0f);
        assertEquals(500, f.count);
        f.update(0.016f, 0, 64, 0, 14f, CALM, 0f, 1f, 120, 0f);
        assertEquals(120, f.count);
        f.update(0.016f, 0, 64, 0, 14f, CALM, 0f, 1f, 0, 0f);
        assertEquals(0, f.count);
    }

    // ---- где они живут

    @Test
    void freshFlakesSpawnInsideTheCylinderAroundTheCamera() {
        SnowFlakes f = new SnowFlakes();
        f.update(0.0001f, 100, 70, -50, 12f, CALM, 0f, 1f, 800, 0f);
        for (int i = 0; i < f.count; i++) {
            double dx = f.x[i] - 100, dz = f.z[i] + 50;
            assertTrue(Math.sqrt(dx * dx + dz * dz) <= 12.0 + 1e-6);
            assertTrue(f.y[i] >= 70 - SnowFlakes.SPAWN_BELOW - 1e-6 && f.y[i] <= 70 + SnowFlakes.SPAWN_ABOVE + 1e-6);
        }
    }

    @Test
    void flakesFollowAMovingCameraAndNeverLeaveTheStormVolume() {
        SnowFlakes f = new SnowFlakes();
        double cx = 0, cz = 0;
        for (int step = 0; step < 600; step++) {
            cx += 0.8;
            cz -= 0.3;
            f.update(0.02f, cx, 64, cz, 14f, SnowFlakes.wind(step * 0.02f, 1.0f, 1.0f), 0.5f, 1f, 600, step * 0.02f);
        }
        double limit = 14.0 * SnowFlakes.RECYCLE_MARGIN;
        for (int i = 0; i < f.count; i++) {
            double dx = f.x[i] - cx, dz = f.z[i] - cz;
            assertTrue(dx * dx + dz * dz <= limit * limit + 1e-6, "flake outside the cylinder");
            assertTrue(f.y[i] >= 64 - SnowFlakes.SPAWN_BELOW - 1e-6 && f.y[i] <= 64 + SnowFlakes.SPAWN_ABOVE + 1e-6);
        }
    }

    // ---- как они движутся

    @Test
    void snowFallsDownwardInCalmAir() {
        SnowFlakes f = new SnowFlakes();
        f.update(0.0001f, 0, 64, 0, 14f, CALM, 0f, 1f, 200, 0f);
        double before = meanY(f);
        f.update(0.1f, 0, 64, 0, 14f, CALM, 0f, 1f, 200, 0.1f);
        assertTrue(meanY(f) < before);
        for (int i = 0; i < f.count; i++) assertTrue(f.vy[i] < 0f);
    }

    @Test
    void speedSettingMakesSnowFallFaster() {
        SnowFlakes slow = new SnowFlakes();
        SnowFlakes fast = new SnowFlakes();
        slow.update(0.0001f, 0, 64, 0, 14f, CALM, 0f, 0.5f, 200, 0f);
        fast.update(0.0001f, 0, 64, 0, 14f, CALM, 0f, 2.0f, 200, 0f);
        double slowBefore = meanY(slow), fastBefore = meanY(fast);
        slow.update(0.1f, 0, 64, 0, 14f, CALM, 0f, 0.5f, 200, 0.1f);
        fast.update(0.1f, 0, 64, 0, 14f, CALM, 0f, 2.0f, 200, 0.1f);
        assertTrue(fastBefore - meanY(fast) > slowBefore - meanY(slow));
    }

    @Test
    void windPushesSnowDownwind() {
        SnowFlakes calm = new SnowFlakes();
        SnowFlakes windy = new SnowFlakes();
        calm.update(0.0001f, 0, 64, 0, 14f, CALM, 0f, 1f, 300, 0f);
        windy.update(0.0001f, 0, 64, 0, 14f, CALM, 0f, 1f, 300, 0f);
        double c0 = meanX(calm), w0 = meanX(windy);
        calm.update(0.2f, 0, 64, 0, 14f, CALM, 0f, 1f, 300, 0.2f);
        windy.update(0.2f, 0, 64, 0, 14f, new SnowFlakes.Wind(12, 0, 0), 0f, 1f, 300, 0.2f);
        assertTrue(meanX(windy) - w0 > meanX(calm) - c0 + 1.0);
    }

    @Test
    void sameSeedGivesTheSameStorm() {
        SnowFlakes a = new SnowFlakes();
        SnowFlakes b = new SnowFlakes();
        a.reseed(42L);
        b.reseed(42L);
        a.update(0.05f, 1, 2, 3, 14f, CALM, 0.3f, 1f, 100, 1f);
        b.update(0.05f, 1, 2, 3, 14f, CALM, 0.3f, 1f, 100, 1f);
        for (int i = 0; i < 100; i++) {
            assertEquals(a.x[i], b.x[i], 0.0);
            assertEquals(a.y[i], b.y[i], 0.0);
        }
    }

    @Test
    void zeroTimeStepChangesNothing() {
        SnowFlakes f = new SnowFlakes();
        f.update(0.0001f, 0, 64, 0, 14f, CALM, 0f, 1f, 50, 0f);
        double y0 = f.y[0];
        f.update(0f, 0, 64, 0, 14f, CALM, 0f, 1f, 50, 0f);
        assertEquals(y0, f.y[0], 0.0);
    }

    // ---- ветер

    @Test
    void noWindWithoutPowerOrStorm() {
        assertEquals(SnowFlakes.Wind.ZERO, SnowFlakes.wind(3f, 0f, 1f));
        assertEquals(SnowFlakes.Wind.ZERO, SnowFlakes.wind(3f, 1f, 0f));
    }

    private static double horizontal(SnowFlakes.Wind w) {
        return Math.hypot(w.x(), w.z());
    }

    @Test
    void windGrowsWithPowerAndStormStrength() {
        double base = horizontal(SnowFlakes.wind(5f, 1.0f, 0.5f));
        assertTrue(horizontal(SnowFlakes.wind(5f, 2.0f, 0.5f)) > base);
        assertTrue(horizontal(SnowFlakes.wind(5f, 1.0f, 1.0f)) > base);
        assertEquals(2.0, horizontal(SnowFlakes.wind(5f, 2.0f, 0.5f)) / base, 1e-9);
    }

    @Test
    void gustsStayBoundedAndMostlyHorizontal() {
        double cap = 1.0 * 14.0 * 1.5 * 1.0;
        for (int i = 0; i < 400; i++) {
            SnowFlakes.Wind w = SnowFlakes.wind(i * 0.37f, 1.0f, 1.0f);
            assertTrue(horizontal(w) <= cap + 1e-9);
            assertTrue(Math.abs(w.y()) <= horizontal(w) * 0.2 + 1e-9);
        }
    }

    @Test
    void windDirectionDriftsOverTime() {
        SnowFlakes.Wind early = SnowFlakes.wind(0f, 1f, 1f);
        SnowFlakes.Wind late = SnowFlakes.wind(30f, 1f, 1f);
        double angleDelta = Math.abs(Math.atan2(late.z(), late.x()) - Math.atan2(early.z(), early.x()));
        assertTrue(angleDelta > 0.5);
    }

    @Test
    void strongerWindMeansMoreTurbulence() {
        assertTrue(SnowFlakes.turbulence(2f) > SnowFlakes.turbulence(0.5f));
        assertTrue(SnowFlakes.turbulence(0f) > 0f);
        assertEquals(SnowFlakes.turbulence(0f), SnowFlakes.turbulence(-1f), 0f);
    }

    // ---- плавный разгон шторма

    @Test
    void stormRampsUpToTheIntensitySettingNotToFull() {
        float s = 0f;
        for (int i = 0; i < 400; i++) s = SnowFlakes.ramp(s, 0.4f, 0.016f);
        assertEquals(0.4f, s, 1e-6f);
    }

    @Test
    void rampNeverOvershootsAndFadesOutFasterThanItBuildsUp() {
        float up = SnowFlakes.ramp(0f, 1f, 1f);
        float down = 1f - SnowFlakes.ramp(1f, 0f, 1f);
        assertTrue(up > 0f && up < 1f);
        assertTrue(down > up, "fade-out is quicker than build-up");
        assertEquals(0f, SnowFlakes.ramp(0.01f, 0f, 5f), 0f);
        assertEquals(0.3f, SnowFlakes.ramp(0.3f, 0.3f, 1f), 0f);
        assertEquals(0.5f, SnowFlakes.ramp(0.5f, 1f, 0f), 0f);
    }
}
