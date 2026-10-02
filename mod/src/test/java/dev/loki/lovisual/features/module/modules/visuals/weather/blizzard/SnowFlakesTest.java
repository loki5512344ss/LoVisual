package dev.loki.lovisual.features.module.modules.visuals.weather.blizzard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Спецификация метели (поведение evo): сколько снежинок, где они живут, как падают, куда их сносит ветер.
 * Снег идёт только над землёй и под открытым небом, внутри помещения его нет.
 */
class SnowFlakesTest {

    private static final SnowFlakes.Wind CALM = SnowFlakes.Wind.ZERO;
    private static final SnowFlakes.Ground FLAT = (x, z) -> 60.0f;
    private static final SnowFlakes.Ground ROOF = (x, z) -> 100.0f;

    private static void tick(SnowFlakes f, float dt, double cx, double cy, double cz, float radius,
                             SnowFlakes.Wind wind, float turb, int target, double seconds, SnowFlakes.Ground ground) {
        f.update(dt, cx, cy, cz, radius, wind, turb, target, seconds, ground);
    }

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
        assertEquals(0, SnowFlakes.targetCount(12f, 0f, 1f));
        assertEquals(0, SnowFlakes.targetCount(12f, 1f, 0f));
        assertEquals(0, SnowFlakes.targetCount(-3f, 1f, 1f));
    }

    @Test
    void moreRadiusDensityOrStrengthMeansMoreFlakes() {
        assertTrue(SnowFlakes.targetCount(20f, 1f, 0.5f) > SnowFlakes.targetCount(10f, 1f, 0.5f));
        assertTrue(SnowFlakes.targetCount(10f, 2f, 0.5f) > SnowFlakes.targetCount(10f, 1f, 0.5f));
        assertTrue(SnowFlakes.targetCount(10f, 1f, 0.8f) > SnowFlakes.targetCount(10f, 1f, 0.4f));
    }

    @Test
    void halfTheStrengthGivesAboutHalfTheFlakes() {
        double full = SnowFlakes.targetCount(10f, 1f, 1.0f);
        double half = SnowFlakes.targetCount(10f, 1f, 0.5f);
        assertEquals(0.5, half / full, 0.02);
    }

    @Test
    void flakeCountIsCapped() {
        assertEquals(SnowFlakes.MAX_FLAKES, SnowFlakes.targetCount(24f, 3f, 1f));
        assertEquals(12000, SnowFlakes.MAX_FLAKES);
    }

    @Test
    void activeFlakeCountFollowsTheTargetUpAndDown() {
        SnowFlakes f = new SnowFlakes();
        tick(f, 0.016f, 0, 64, 0, 14f, CALM, 0f, 500, 0, FLAT);
        assertEquals(500, f.count);
        tick(f, 0.016f, 0, 64, 0, 14f, CALM, 0f, 120, 0, FLAT);
        assertEquals(120, f.count);
        tick(f, 0.016f, 0, 64, 0, 14f, CALM, 0f, 0, 0, FLAT);
        assertEquals(0, f.count);
    }

    // ---- где они живут: над землёй, не в помещении

    @Test
    void freshFlakesSpawnInsideTheCylinderBetweenGroundAndCeiling() {
        SnowFlakes f = new SnowFlakes();
        tick(f, 0.0001f, 100, 70, -50, 12f, CALM, 0f, 800, 0, FLAT);
        for (int i = 0; i < f.count; i++) {
            double dx = f.x[i] - 100, dz = f.z[i] + 50;
            assertTrue(Math.sqrt(dx * dx + dz * dz) <= 12.0 + 1e-6);
            assertTrue(f.y[i] > 64.0 - 1e-6, "not below camera - 6 (above the ground 60)");
            assertTrue(f.y[i] <= 70 + SnowFlakes.SPAWN_CEILING + 1e-6);
        }
    }

    @Test
    void noSnowFallsIndoorsUnderARoof() {
        SnowFlakes f = new SnowFlakes();
        tick(f, 0.016f, 0, 64, 0, 14f, CALM, 0f, 1000, 0, ROOF);
        assertEquals(0, f.count);
    }

    @Test
    void snowStopsAtTheGroundAndNeverSinksBelowIt() {
        SnowFlakes f = new SnowFlakes();
        double cx = 0;
        for (int step = 0; step < 800; step++) {
            tick(f, 0.02f, cx, 62, 0, 14f, CALM, 0.4f, 400, step * 0.02, FLAT);
            for (int i = 0; i < f.count; i++) assertTrue(f.y[i] > 60.0, "flake sank into the ground");
        }
        assertTrue(f.count > 0);
    }

    @Test
    void flakesOverARoofedColumnDisappearButOpenSkyKeepsSnowing() {
        // The left half (x < 0) is open sky, the right half is a roof.
        SnowFlakes.Ground half = (x, z) -> x < 0 ? 60.0f : 100.0f;
        SnowFlakes f = new SnowFlakes();
        for (int step = 0; step < 600; step++) {
            tick(f, 0.02f, 0, 64, 0, 14f, CALM, 0.3f, 600, step * 0.02, half);
        }
        assertTrue(f.count > 0);
        for (int i = 0; i < f.count; i++) assertTrue(f.x[i] < 1.0, "snow appeared under the roof");
    }

    @Test
    void flakesFollowAMovingCameraAndNeverLeaveTheStormVolume() {
        SnowFlakes f = new SnowFlakes();
        double cx = 0, cz = 0;
        for (int step = 0; step < 600; step++) {
            cx += 0.8;
            cz -= 0.3;
            tick(f, 0.02f, cx, 64, cz, 14f, SnowFlakes.wind(step * 0.02, 1.0f, 1.0f), 0.5f, 600, step * 0.02, FLAT);
        }
        double limit = 14.0 * SnowFlakes.RECYCLE_MARGIN;
        for (int i = 0; i < f.count; i++) {
            double dx = f.x[i] - cx, dz = f.z[i] - cz;
            assertTrue(dx * dx + dz * dz <= limit * limit + 1e-6, "flake outside the cylinder");
            assertTrue(f.y[i] <= 64 + SnowFlakes.RECYCLE_HEIGHT + 1e-6);
        }
    }

    @Test
    void teleportingTheCameraBringsTheSnowAlong() {
        SnowFlakes f = new SnowFlakes();
        tick(f, 0.02f, 0, 64, 0, 14f, CALM, 0f, 300, 0, FLAT);
        tick(f, 0.02f, 5000, 64, 5000, 14f, CALM, 0f, 300, 0, FLAT);
        for (int i = 0; i < f.count; i++) {
            assertTrue(Math.abs(f.x[i] - 5000) < 20 && Math.abs(f.z[i] - 5000) < 20);
        }
    }

    // ---- как они движутся

    @Test
    void snowFallsDownwardInCalmAir() {
        SnowFlakes f = new SnowFlakes();
        tick(f, 0.0001f, 0, 70, 0, 14f, CALM, 0f, 200, 0, FLAT);
        double before = meanY(f);
        tick(f, 0.1f, 0, 70, 0, 14f, CALM, 0f, 200, 0.1, FLAT);
        assertTrue(meanY(f) < before);
        for (int i = 0; i < f.count; i++) assertTrue(f.vy[i] < 0f);
    }

    @Test
    void windPushesSnowDownwind() {
        SnowFlakes calm = new SnowFlakes();
        SnowFlakes windy = new SnowFlakes();
        tick(calm, 0.0001f, 0, 70, 0, 14f, CALM, 0f, 300, 0, FLAT);
        tick(windy, 0.0001f, 0, 70, 0, 14f, CALM, 0f, 300, 0, FLAT);
        double c0 = meanX(calm), w0 = meanX(windy);
        tick(calm, 0.2f, 0, 70, 0, 14f, CALM, 0f, 300, 0.2, FLAT);
        tick(windy, 0.2f, 0, 70, 0, 14f, new SnowFlakes.Wind(12, 0, 0), 0f, 300, 0.2, FLAT);
        assertTrue(meanX(windy) - w0 > meanX(calm) - c0 + 1.0);
    }

    @Test
    void sameSeedGivesTheSameStorm() {
        SnowFlakes a = new SnowFlakes();
        SnowFlakes b = new SnowFlakes();
        a.reseed(42L);
        b.reseed(42L);
        tick(a, 0.05f, 1, 70, 3, 14f, CALM, 0.3f, 100, 1, FLAT);
        tick(b, 0.05f, 1, 70, 3, 14f, CALM, 0.3f, 100, 1, FLAT);
        for (int i = 0; i < 100; i++) {
            assertEquals(a.x[i], b.x[i], 0.0);
            assertEquals(a.y[i], b.y[i], 0.0);
        }
    }

    @Test
    void zeroTimeStepChangesNothing() {
        SnowFlakes f = new SnowFlakes();
        tick(f, 0.0001f, 0, 70, 0, 14f, CALM, 0f, 50, 0, FLAT);
        double y0 = f.y[0];
        tick(f, 0f, 0, 70, 0, 14f, CALM, 0f, 50, 0, FLAT);
        assertEquals(y0, f.y[0], 0.0);
    }

    @Test
    void turbulenceAddsSidewaysJitterButNoNetDownwindDrift() {
        SnowFlakes f = new SnowFlakes();
        tick(f, 0.0001f, 0, 70, 0, 14f, CALM, 0f, 400, 0, FLAT);
        double x0 = meanX(f);
        tick(f, 0.1f, 0, 70, 0, 14f, CALM, SnowFlakes.turbulence(1f), 400, 0.1, FLAT);
        boolean sideways = false;
        for (int i = 0; i < f.count; i++) sideways |= Math.abs(f.vx[i]) > 0.05f;
        assertTrue(sideways);
        assertEquals(0.0, meanX(f) - x0, 0.3);
    }

    // ---- ветер

    @Test
    void noWindWithoutPowerOrStorm() {
        assertEquals(SnowFlakes.Wind.ZERO, SnowFlakes.wind(3, 0f, 1f));
        assertEquals(SnowFlakes.Wind.ZERO, SnowFlakes.wind(3, 1f, 0f));
    }

    private static double horizontal(SnowFlakes.Wind w) {
        return Math.hypot(w.x(), w.z());
    }

    @Test
    void windGrowsLinearlyWithPowerAndStormStrength() {
        double base = horizontal(SnowFlakes.wind(5, 0.5f, 0.5f));
        assertEquals(2.0, horizontal(SnowFlakes.wind(5, 1.0f, 0.5f)) / base, 1e-9);
        assertEquals(2.0, horizontal(SnowFlakes.wind(5, 0.5f, 1.0f)) / base, 1e-9);
    }

    @Test
    void gustsStayBoundedAndMostlyHorizontal() {
        double cap = SnowFlakes.WIND_SPEED_MAX * 1.5;
        double min = Double.MAX_VALUE;
        for (int i = 0; i < 2000; i++) {
            SnowFlakes.Wind w = SnowFlakes.wind(i * 0.37, 1.0f, 1.0f);
            assertTrue(horizontal(w) <= cap + 1e-9);
            assertTrue(Math.abs(w.y()) <= horizontal(w) * 0.12 + 1e-9);
            min = Math.min(min, horizontal(w));
        }
        assertTrue(min >= SnowFlakes.WIND_SPEED_MAX * 0.5 - 1e-9, "the wind never calms below half of its power");
    }

    @Test
    void windDirectionTurnsSlowlyOverTime() {
        SnowFlakes.Wind a = SnowFlakes.wind(100, 1f, 1f);
        SnowFlakes.Wind b = SnowFlakes.wind(100.5, 1f, 1f);
        double step = Math.abs(Math.atan2(b.z(), b.x()) - Math.atan2(a.z(), a.x()));
        assertTrue(step < 0.3, "half a second never swings the wind round");
        SnowFlakes.Wind late = SnowFlakes.wind(400, 1f, 1f);
        double total = Math.abs(Math.atan2(late.z(), late.x()) - Math.atan2(a.z(), a.x()));
        assertTrue(total > 0.5, "over minutes the direction drifts");
    }

    @Test
    void turbulenceScalesWithTheSetting() {
        assertEquals(0f, SnowFlakes.turbulence(0f), 0f);
        assertEquals(0f, SnowFlakes.turbulence(-1f), 0f);
        assertEquals(2.0, SnowFlakes.turbulence(1f) / SnowFlakes.turbulence(0.5f), 1e-6);
    }

    // ---- сила шторма и экспозиция

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
        assertTrue(down > up);
        assertEquals(0f, SnowFlakes.ramp(0.01f, 0f, 5f), 0f);
        assertEquals(0.3f, SnowFlakes.ramp(0.3f, 0.3f, 1f), 0f);
        assertEquals(0.5f, SnowFlakes.ramp(0.5f, 1f, 0f), 0f);
    }

    @Test
    void skyExposureIsFullOutdoorsAndZeroUnderACeiling() {
        assertEquals(1f, SnowFlakes.exposureTarget(70, 65f), 0f);
        assertEquals(1f, SnowFlakes.exposureTarget(65, 65f), 0f);
        assertEquals(0f, SnowFlakes.exposureTarget(64, 65f), 0f);
    }

    @Test
    void exposureEasesInsteadOfSnapping() {
        float e = SnowFlakes.approach(0f, 1f, SnowFlakes.EXPOSURE_PER_SECOND, 0.1f);
        assertTrue(e > 0f && e < 0.3f);
        assertEquals(1f, SnowFlakes.approach(0f, 1f, SnowFlakes.EXPOSURE_PER_SECOND, 5f), 0f);
    }

    @Test
    void stormFollowsTheSnowfallOnlyOption() {
        assertEquals(0.85f, SnowFlakes.targetStrength(0.85f, false, 0f, false), 0f);
        assertEquals(0f, SnowFlakes.targetStrength(0.85f, true, 0f, true), 0f);
        assertEquals(0f, SnowFlakes.targetStrength(0.85f, true, 1f, false), 0f);
        assertEquals(0.85f, SnowFlakes.targetStrength(0.85f, true, 1f, true), 0f);
        assertEquals(0.425f, SnowFlakes.targetStrength(0.85f, true, 0.5f, true), 1e-6f);
    }
}
