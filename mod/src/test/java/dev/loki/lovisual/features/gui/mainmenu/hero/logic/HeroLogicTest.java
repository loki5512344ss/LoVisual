package dev.loki.lovisual.features.gui.mainmenu.hero.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeroLogicTest {
    private static final TextMetrics METRICS = px -> px * 1.35f;

    private static HeroLayout.Layout layout(float w, float h) {
        return HeroLayout.compute(w, h, 6, 0f, METRICS);
    }

    @Test
    void periodsCoverTheWholeDay() {
        assertEquals(DayCycle.NIGHT, DayCycle.period(3));
        assertEquals(DayCycle.MORNING, DayCycle.period(5));
        assertEquals(DayCycle.DAY, DayCycle.period(12));
        assertEquals(DayCycle.EVENING, DayCycle.period(21));
        assertEquals(DayCycle.NIGHT, DayCycle.period(22));
        assertEquals(DayCycle.NIGHT, DayCycle.period(23));
    }

    @Test
    void thumbStaysInsideBar() {
        assertEquals(105f, DayCycle.thumbX(100f, 200f, 10f, 0f), 1e-4);
        assertEquals(295f, DayCycle.thumbX(100f, 200f, 10f, 1f), 1e-4);
        assertEquals(200f, DayCycle.thumbX(100f, 200f, 10f, DayCycle.progress(12, 0)), 1e-4);
        assertEquals(105f, DayCycle.thumbX(100f, 200f, 10f, -3f), 1e-4);
    }

    @Test
    void buttonsAreStackedWithoutOverlap() {
        HeroLayout.Layout l = layout(1920f, 1080f);
        assertEquals(HeroLayout.BUTTON_COUNT, l.buttons().length);
        for (int i = 0; i < HeroLayout.ADDONS; i++) {
            assertTrue(l.buttons()[i].y() + l.buttons()[i].h() < l.buttons()[i + 1].y());
        }
        Rect o = l.buttons()[HeroLayout.OPTIONS];
        Rect q = l.buttons()[HeroLayout.QUIT];
        assertTrue(o.x() + o.w() < q.x());
        assertEquals(l.buttons()[0].x() + l.buttons()[0].w(), q.x() + q.w(), 1e-3);
    }

    @Test
    void hitTestFindsButtonsAndDots() {
        HeroLayout.Layout l = layout(1920f, 1080f);
        for (int i = 0; i < HeroLayout.BUTTON_COUNT; i++) {
            Rect r = l.buttons()[i];
            assertEquals(i, HeroLayout.buttonAt(l, r.x() + r.w() / 2, r.y() + r.h() / 2));
        }
        assertEquals(-1, HeroLayout.buttonAt(l, 0f, 0f));
        for (int i = 0; i < 6; i++) {
            assertEquals(i, HeroLayout.dotAt(l, 6, l.dotsStartX() + i * l.dotSpacing(), l.dotY()));
        }
        assertEquals(-1, HeroLayout.dotAt(l, 6, 0f, 0f));
    }

    @Test
    void dotsAndWidgetFitInsideViewport() {
        HeroLayout.Layout l = layout(1920f, 1080f);
        assertTrue(l.dotsStartX() - l.dotRadius() >= l.widget().x());
        assertTrue(l.widget().x() + l.widget().w() <= 1920f);
        assertEquals(12f / 12f, HeroLayout.font(12f), 1e-6);
    }

    @Test
    void textBlocksNeverOverlapAtAnyResolution() {
        float[][] sizes = {{1920f, 1080f}, {1280f, 720f}, {854f, 480f}, {3840f, 2160f}, {1024f, 600f}};
        for (float[] s : sizes) {
            HeroLayout.Layout l = layout(s[0], s[1]);
            float titleH = METRICS.height(l.sizes().title());
            assertTrue(l.tagY() + METRICS.height(l.sizes().tag()) <= l.titleY1() + 1e-3, "tag");
            assertTrue(l.titleY1() + titleH <= l.titleY2() + 1e-3, "title lines");
            assertTrue(l.titleY2() + titleH <= l.subY() + 1e-3, "title/sub");
            assertTrue(l.subY() + METRICS.height(l.sizes().sub()) <= l.buttons()[0].y() + 1e-3, "sub/buttons");
            Rect last = l.buttons()[HeroLayout.QUIT];
            assertTrue(last.y() + last.h() <= l.footerY() + 1e-3, "buttons/footer " + s[1]);
            assertTrue(l.account().y() + l.avatarSize() <= l.tagY(), "account/tag " + s[1]);
        }
    }

    @Test
    void layoutScalesDownWhenTheViewportIsShort() {
        assertTrue(layout(1024f, 400f).k() < layout(1920f, 1080f).k());
    }
}
