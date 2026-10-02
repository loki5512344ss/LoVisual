package dev.loki.lovisual.features.module.modules.visuals.effects.ribbon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Спецификация затухания и сужения ленты: свежее яркое и широкое, старое прозрачное и узкое. */
class TrailRibbonTest {

    @Test
    void lifeFractionGoesFromOneToZeroOverTheLifetime() {
        assertEquals(1.0f, TrailRibbon.lifeFraction(100L, 100L, 1000L), 1e-6f);
        assertEquals(0.5f, TrailRibbon.lifeFraction(600L, 100L, 1000L), 1e-6f);
        assertEquals(0.0f, TrailRibbon.lifeFraction(1100L, 100L, 1000L), 1e-6f);
    }

    @Test
    void lifeFractionIsClampedAndSafeForZeroLifetime() {
        assertEquals(0.0f, TrailRibbon.lifeFraction(9999L, 0L, 1000L), 0.0f);
        assertEquals(1.0f, TrailRibbon.lifeFraction(0L, 500L, 1000L), 0.0f);
        assertEquals(0.0f, TrailRibbon.lifeFraction(100L, 100L, 0L), 0.0f);
    }

    @Test
    void freshPointKeepsItsColour() {
        assertEquals(0x88FF66CC, TrailRibbon.fadeAlpha(0x88FF66CC, 1.0f));
    }

    @Test
    void fadingOnlyTouchesAlphaNeverRgb() {
        int faded = TrailRibbon.fadeAlpha(0xC8FF66CC, 0.3f);
        assertEquals(0xFF66CC, faded & 0x00FFFFFF);
        assertEquals(0, TrailRibbon.fadeAlpha(0xC8FF66CC, 0.0f) >>> 24);
    }

    @Test
    void alphaDecreasesMonotonicallyWithAge() {
        int previous = Integer.MAX_VALUE;
        for (int i = 10; i >= 0; i--) {
            int alpha = TrailRibbon.fadeAlpha(0xFFFFFFFF, i / 10.0f) >>> 24;
            assertTrue(alpha <= previous);
            previous = alpha;
        }
    }

    @Test
    void outOfRangeLifeIsClamped() {
        assertEquals(0x88FF66CC, TrailRibbon.fadeAlpha(0x88FF66CC, 5.0f));
        assertEquals(0x00FF66CC, TrailRibbon.fadeAlpha(0x88FF66CC, -5.0f));
    }

    @Test
    void ribbonTapersTowardTheTailButNeverVanishes() {
        double head = TrailRibbon.halfWidth(0.4f, 1.0f);
        double tail = TrailRibbon.halfWidth(0.4f, 0.0f);
        assertEquals(0.2, head, 1e-6);
        assertTrue(tail > 0.0 && tail < head);
        assertEquals(0.4 * 0.5 * 0.25, tail, 1e-6);
    }

    @Test
    void halfWidthScalesLinearlyWithWidthSetting() {
        assertEquals(2.0 * TrailRibbon.halfWidth(0.3f, 0.6f), TrailRibbon.halfWidth(0.6f, 0.6f), 1e-6);
    }

    // ---- ориентация ленты

    private static final double[][] PATH = {{0, 64, 0}, {1, 64, 0.5}, {2, 64, 1.0}, {3, 65, 1.5}};

    @Test
    void horizontalRibbonSidesAreLevelUnitAndPerpendicularToTheDirectionOfFlight() {
        double[][] side = TrailRibbon.frame(PATH, new net.minecraft.world.phys.Vec3(0, 80, 0), true);
        for (int i = 0; i < PATH.length; i++) {
            assertEquals(0.0, side[i][1], 1e-12, "level");
            assertEquals(1.0, Math.hypot(side[i][0], side[i][2]), 1e-9, "unit");
            double[] a = PATH[Math.max(0, i - 1)], b = PATH[Math.min(PATH.length - 1, i + 1)];
            assertEquals(0.0, side[i][0] * (b[0] - a[0]) + side[i][2] * (b[2] - a[2]), 1e-9, "perpendicular");
        }
    }

    @Test
    void cameraFacingRibbonSidesAreUnitAndPerpendicularToBothTheFlightAndTheViewDirection() {
        net.minecraft.world.phys.Vec3 cam = new net.minecraft.world.phys.Vec3(1, 70, 8);
        double[][] side = TrailRibbon.frame(PATH, cam, false);
        for (int i = 0; i < PATH.length; i++) {
            assertEquals(1.0, Math.sqrt(side[i][0] * side[i][0] + side[i][1] * side[i][1] + side[i][2] * side[i][2]), 1e-9);
            double[] a = PATH[Math.max(0, i - 1)], b = PATH[Math.min(PATH.length - 1, i + 1)];
            double dot = side[i][0] * (b[0] - a[0]) + side[i][1] * (b[1] - a[1]) + side[i][2] * (b[2] - a[2]);
            assertEquals(0.0, dot, 1e-9);
            double view = side[i][0] * (cam.x - PATH[i][0]) + side[i][1] * (cam.y - PATH[i][1]) + side[i][2] * (cam.z - PATH[i][2]);
            assertEquals(0.0, view, 1e-9);
        }
    }

    @Test
    void aPurelyVerticalPathStillGetsAHorizontalSide() {
        double[][] up = {{0, 64, 0}, {0, 65, 0}, {0, 66, 0}};
        double[][] side = TrailRibbon.frame(up, new net.minecraft.world.phys.Vec3(5, 65, 5), true);
        for (double[] s : side) {
            assertEquals(0.0, s[1], 0.0);
            assertEquals(1.0, Math.hypot(s[0], s[2]), 1e-9);
        }
    }

    @Test
    void alphaScalingKeepsColourAndClampsTheMultiplier() {
        assertEquals(0x80123456, TrailRibbon.scaleAlpha(0xFF123456, 0.5f) | 0, 0x01);
        assertEquals(0xFF, TrailRibbon.scaleAlpha(0xFF123456, 9f) >>> 24);
        assertEquals(0x00, TrailRibbon.scaleAlpha(0xFF123456, -2f) >>> 24);
        assertEquals(0x123456, TrailRibbon.scaleAlpha(0xFF123456, 0.3f) & 0xFFFFFF);
    }
}
