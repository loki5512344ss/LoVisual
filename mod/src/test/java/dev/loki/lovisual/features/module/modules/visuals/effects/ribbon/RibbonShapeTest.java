package dev.loki.lovisual.features.module.modules.visuals.effects.ribbon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Спецификация формы ленты шлейфа: сужение, реакция на скорость, плавное начало, затухание у камеры, колебания. */
class RibbonShapeTest {

    // ---- сужение

    @Test
    void uniformTaperKeepsTheWidth() {
        for (float t : new float[]{0f, 0.3f, 1f}) assertEquals(1f, RibbonShape.taper(RibbonShape.Taper.UNIFORM, 1.8f, t), 0f);
    }

    @Test
    void growTaperWidensTowardsTheTailLikeAJetPlume() {
        float head = RibbonShape.taper(RibbonShape.Taper.GROW, 1.8f, 0f);
        float tail = RibbonShape.taper(RibbonShape.Taper.GROW, 1.8f, 1f);
        assertEquals(1f, head, 1e-6f);
        assertEquals(1.8f, tail, 1e-6f);
        assertTrue(RibbonShape.taper(RibbonShape.Taper.GROW, 1.8f, 0.5f) > head);
    }

    @Test
    void shrinkTaperNarrowsTheTailIntoANeedle() {
        float head = RibbonShape.taper(RibbonShape.Taper.SHRINK, 2f, 0f);
        float tail = RibbonShape.taper(RibbonShape.Taper.SHRINK, 2f, 1f);
        assertEquals(1f, head, 1e-6f);
        assertEquals(0.5f, tail, 1e-6f);
    }

    @Test
    void tailnessIsZeroAtTheNewestPointAndOneAtTheOldest() {
        assertEquals(1f, RibbonShape.tailness(0, 10), 1e-6f);
        assertEquals(0f, RibbonShape.tailness(9, 10), 1e-6f);
        assertEquals(0f, RibbonShape.tailness(0, 1), 0f);
    }

    // ---- скорость

    @Test
    void speedFactorRisesFromSlowGlideToFullSpeed() {
        assertEquals(0f, RibbonShape.speedFactor(0.1f), 0f);
        assertEquals(1f, RibbonShape.speedFactor(5f), 0f);
        assertTrue(RibbonShape.speedFactor(1.0f) > RibbonShape.speedFactor(0.6f));
    }

    @Test
    void speedReactiveRibbonIsThinnerAndFainterWhenSlowButNeverVanishes() {
        assertEquals(1f, RibbonShape.speedWidth(false, 0f), 0f);
        assertEquals(1f, RibbonShape.speedAlpha(false, 0f), 0f);
        assertTrue(RibbonShape.speedWidth(true, 0f) > 0.4f && RibbonShape.speedWidth(true, 0f) < 0.5f);
        assertTrue(RibbonShape.speedAlpha(true, 0f) >= 0.25f);
        assertEquals(1f, RibbonShape.speedWidth(true, 9f), 1e-6f);
        assertEquals(1f, RibbonShape.speedAlpha(true, 9f), 1e-6f);
    }

    // ---- плавное начало и приближение камеры

    @Test
    void fadeStartMakesTheNewestPointsTransparent() {
        assertEquals(0f, RibbonShape.headRamp(true, 9, 10), 0f);
        assertTrue(RibbonShape.headRamp(true, 8, 10) < RibbonShape.headRamp(true, 6, 10));
        assertEquals(1f, RibbonShape.headRamp(true, 5, 10), 0f);
        assertEquals(1f, RibbonShape.headRamp(false, 9, 10), 0f);
    }

    @Test
    void ribbonFadesOutWhenTheCameraIsRightOnIt() {
        assertEquals(0f, RibbonShape.nearFade(0.5f), 0f);
        assertEquals(0f, RibbonShape.nearFade(0.75f), 0f);
        assertEquals(1f, RibbonShape.nearFade(2.25f), 1e-6f);
        assertEquals(1f, RibbonShape.nearFade(40f), 0f);
        assertTrue(RibbonShape.nearFade(1.5f) > 0f && RibbonShape.nearFade(1.5f) < 1f);
    }

    @Test
    void agedPointsFadeLinearlyToNothing() {
        assertEquals(1f, RibbonShape.life(1000, 1000, 1200), 0f);
        assertEquals(0.5f, RibbonShape.life(1600, 1000, 1200), 1e-6f);
        assertEquals(0f, RibbonShape.life(2500, 1000, 1200), 0f);
        assertEquals(0f, RibbonShape.life(1000, 1000, 0), 0f);
    }

    // ---- колебания

    @Test
    void straightMotionDoesNotMoveThePoints() {
        float[] o = RibbonShape.motion(RibbonShape.Motion.STRAIGHT, 12345L, 0.5f, 3f);
        assertEquals(0f, o[0], 0f);
        assertEquals(0f, o[1], 0f);
    }

    @Test
    void waveSwaysOnlySidewaysAndStaysWithinTheAmplitude() {
        for (long ms = 0; ms < 5000; ms += 37) {
            float[] o = RibbonShape.motion(RibbonShape.Motion.WAVE, ms, 0.35f, 3f);
            assertTrue(Math.abs(o[0]) <= 0.35f + 1e-6f);
            assertEquals(0f, o[1], 0f);
        }
    }

    @Test
    void helixCorkscrewsOnACircleOfTheAmplitudeRadius() {
        for (long ms = 0; ms < 5000; ms += 53) {
            float[] o = RibbonShape.motion(RibbonShape.Motion.HELIX, ms, 0.4f, 2f);
            assertEquals(0.4f, Math.hypot(o[0], o[1]), 1e-5);
        }
    }

    @Test
    void motionIsAFunctionOfTheBirthTimeSoItTravelsWithThePoint() {
        float[] a = RibbonShape.motion(RibbonShape.Motion.WAVE, 777L, 0.3f, 3f);
        float[] b = RibbonShape.motion(RibbonShape.Motion.WAVE, 777L, 0.3f, 3f);
        assertArrayEquals(a, b);
    }

    @Test
    void fasterWaveOscillatesMoreOftenInTheSameTime() {
        assertTrue(crossings(1f) < crossings(6f));
    }

    private static int crossings(float freq) {
        int count = 0;
        float prev = RibbonShape.motion(RibbonShape.Motion.WAVE, 0, 1f, freq)[0];
        for (long ms = 10; ms < 4000; ms += 10) {
            float cur = RibbonShape.motion(RibbonShape.Motion.WAVE, ms, 1f, freq)[0];
            if ((prev < 0) != (cur < 0)) count++;
            prev = cur;
        }
        return count;
    }

    // ---- разбор настроек

    @Test
    void namesParseCaseInsensitivelyWithAFallback() {
        assertEquals(RibbonShape.Taper.GROW, RibbonShape.Taper.of("Grow"));
        assertEquals(RibbonShape.Taper.SHRINK, RibbonShape.Taper.of("shrink"));
        assertEquals(RibbonShape.Taper.UNIFORM, RibbonShape.Taper.of("nonsense"));
        assertEquals(RibbonShape.Motion.HELIX, RibbonShape.Motion.of("Helix"));
        assertEquals(RibbonShape.Motion.WAVE, RibbonShape.Motion.of("WAVE"));
        assertEquals(RibbonShape.Motion.STRAIGHT, RibbonShape.Motion.of(null));
    }
}
