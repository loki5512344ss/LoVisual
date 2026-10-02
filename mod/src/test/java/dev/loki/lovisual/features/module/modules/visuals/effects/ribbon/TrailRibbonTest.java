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
}
