package dev.loki.lovisual.features.module.modules.visuals.effects.elytratrails;

import dev.loki.lovisual.features.module.modules.visuals.effects.ribbon.RibbonPoint;
import dev.loki.lovisual.features.module.modules.visuals.effects.ribbon.RibbonShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Спецификация вида ленты шлейфа элитры: как настройки влияют на ширину, прозрачность и колебания. */
class ElytraRibbonStyleTest {

    private static ElytraRibbonStyle style(RibbonShape.Taper taper, RibbonShape.Motion motion, boolean speed,
                                           boolean fadeStart) {
        return new ElytraRibbonStyle(taper, 2f, motion, 0.3f, 3f, speed, fadeStart, true, true);
    }

    private static final RibbonPoint FAST = new RibbonPoint(0, 0, 0, 0L, 2.0f);
    private static final RibbonPoint SLOW = new RibbonPoint(0, 0, 0, 0L, 0.0f);

    @Test
    void theRibbonLiesFlatLikeABannerAndHasAHaloOnlyWhenAsked() {
        assertTrue(style(RibbonShape.Taper.GROW, RibbonShape.Motion.STRAIGHT, false, false).horizontal());
        assertTrue(style(RibbonShape.Taper.GROW, RibbonShape.Motion.STRAIGHT, false, false).glowScale() > 1.0f);
        ElytraRibbonStyle none = new ElytraRibbonStyle(RibbonShape.Taper.GROW, 2f, RibbonShape.Motion.STRAIGHT, 0.3f, 3f,
                false, false, false, false);
        assertEquals(0f, none.glowScale(), 0f);
        assertFalse(none.softEdges());
    }

    @Test
    void widthIsHalfTheSettingTimesTaperTimesSpeedFactor() {
        ElytraRibbonStyle plain = style(RibbonShape.Taper.UNIFORM, RibbonShape.Motion.STRAIGHT, false, false);
        assertEquals(0.11, plain.halfWidth(0.22f, 3, 10, FAST, 1f), 1e-9);
        ElytraRibbonStyle grow = style(RibbonShape.Taper.GROW, RibbonShape.Motion.STRAIGHT, false, false);
        assertTrue(grow.halfWidth(0.22f, 0, 10, FAST, 1f) > grow.halfWidth(0.22f, 9, 10, FAST, 1f));
        ElytraRibbonStyle reactive = style(RibbonShape.Taper.UNIFORM, RibbonShape.Motion.STRAIGHT, true, false);
        assertTrue(reactive.halfWidth(0.22f, 3, 10, SLOW, 1f) < reactive.halfWidth(0.22f, 3, 10, FAST, 1f));
    }

    @Test
    void ribbonIsInvisibleRightAtTheCameraAndWhenExpired() {
        ElytraRibbonStyle s = style(RibbonShape.Taper.UNIFORM, RibbonShape.Motion.STRAIGHT, false, false);
        assertEquals(0f, s.alpha(3, 10, FAST, 1f, 0.3f), 0f);
        assertEquals(0f, s.alpha(3, 10, FAST, 0f, 20f), 0f);
        assertEquals(1f, s.alpha(3, 10, FAST, 1f, 20f), 1e-6f);
    }

    @Test
    void fadeStartHidesTheNewestPointOnly() {
        ElytraRibbonStyle s = style(RibbonShape.Taper.UNIFORM, RibbonShape.Motion.STRAIGHT, false, true);
        assertEquals(0f, s.alpha(9, 10, FAST, 1f, 20f), 0f);
        assertEquals(1f, s.alpha(2, 10, FAST, 1f, 20f), 1e-6f);
    }

    @Test
    void onlyWavyMotionsMoveThePoints() {
        assertNull(style(RibbonShape.Taper.UNIFORM, RibbonShape.Motion.STRAIGHT, false, false).motion(FAST));
        assertNotNull(style(RibbonShape.Taper.UNIFORM, RibbonShape.Motion.WAVE, false, false).motion(FAST));
        assertNotNull(style(RibbonShape.Taper.UNIFORM, RibbonShape.Motion.HELIX, false, false).motion(FAST));
    }
}
