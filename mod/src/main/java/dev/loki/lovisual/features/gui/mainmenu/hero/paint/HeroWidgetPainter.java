package dev.loki.lovisual.features.gui.mainmenu.hero.paint;

import dev.loki.lovisual.features.gui.mainmenu.MainMenuTextUtil;
import dev.loki.lovisual.features.gui.mainmenu.hero.logic.DayCycle;
import dev.loki.lovisual.features.gui.mainmenu.hero.logic.HeroLayout.Layout;
import dev.loki.lovisual.features.gui.mainmenu.hero.logic.Rect;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.resources.language.I18n;

import java.time.LocalTime;
import java.util.List;

import static dev.loki.lovisual.features.gui.mainmenu.hero.paint.HeroPaint.*;

/** Top-right widget: clock, time-of-day label, theme dots and the daylight bar with its thumb. */
public final class HeroWidgetPainter {
    private static final int[] BAR_STOPS = {0x283450, 0xDC8255, 0xEBA08C, 0x373C5F};

    private HeroWidgetPainter() {
    }

    public static void draw(Layout l, float a, List<Themes.ThemeEntry> dots, String currentThemeId, int hoveredDot, LocalTime now) {
        float k = l.k();
        Rect w = l.widget();
        float rad = 12f * k;
        Renderer2D.COLOR.roundedRectSoftShadow(w.x(), w.y() + 2f * k, w.w(), w.h(), rad, 14f * k, 0.05f, argb(0x060810, a * 0.5f));
        Renderer2D.COLOR.roundedRect(w.x(), w.y(), w.w(), w.h(), rad, argb(0x141820, a * 0.66f));
        Renderer2D.COLOR.roundedRectStrokeGradient(w.x(), w.y(), w.w(), w.h(), rad, 1f, argb(0xFFFFFF, a * 0.12f), argb(0xFFFFFF, a * 0.06f), 90f);

        TextRenderer bold = MainMenuTextUtil.interBold(TextRenderer.get());
        TextRenderer medium = MainMenuTextUtil.interMedium(bold);
        String time = String.format("%02d:%02d", now.getHour(), now.getMinute());
        float cx = w.x() + l.pad() + 2f * k;
        float cy = w.y() + l.pad();
        text(bold, time, cx, cy, l.sizes().time(), argb(0xFFFFFF, a));
        String period = I18n.get("screen.lovisual.hero_menu.period." + DayCycle.period(now.getHour()));
        float periodY = cy + height(bold, l.sizes().time()) - height(medium, 8.5f * k) - 1.5f * k;
        text(medium, period, cx + width(bold, time, l.sizes().time()) + 6f * k, periodY, 8.5f * k, argb(0xFFFFFF, a * 0.55f));

        drawDots(l, a, dots, currentThemeId, hoveredDot);
        drawBar(l, a, DayCycle.progress(now.getHour(), now.getMinute()));
    }

    private static void drawDots(Layout l, float a, List<Themes.ThemeEntry> dots, String currentId, int hovered) {
        float r = l.dotRadius();
        for (int i = 0; i < dots.size(); i++) {
            Themes.ThemeEntry entry = dots.get(i);
            float x = l.dotsStartX() + i * l.dotSpacing();
            int color = entry.theme().accent();
            boolean selected = entry.id().equals(currentId);
            if (selected) {
                Renderer2D.COLOR.circle(x, l.dotY(), r + 2.8f * l.k(), argb(color, a * 0.30f));
            } else if (i == hovered) {
                Renderer2D.COLOR.circle(x, l.dotY(), r + 1.8f * l.k(), argb(0xFFFFFF, a * 0.55f));
            }
            Renderer2D.COLOR.circle(x, l.dotY(), r, argb(color, a));
        }
    }

    private static void drawBar(Layout l, float a, float progress) {
        Rect b = l.bar();
        float rad = b.h() * 0.5f;
        float seg = b.w() / 3f;
        for (int i = 0; i < 3; i++) {
            Renderer2D.COLOR.quad(b.x() + i * seg, b.y(), seg, b.h(),
                    argb(BAR_STOPS[i], a * 0.9f), argb(BAR_STOPS[i + 1], a * 0.9f),
                    argb(BAR_STOPS[i + 1], a * 0.9f), argb(BAR_STOPS[i], a * 0.9f));
        }
        Renderer2D.COLOR.roundedRectStrokeGradient(b.x(), b.y(), b.w(), b.h(), rad, 0.8f * l.k(),
                argb(0xFFFFFF, a * 0.14f), argb(0xFFFFFF, a * 0.14f), 0f);
        float tx = DayCycle.thumbX(b.x(), b.w(), b.h(), progress);
        float ty = b.y() + b.h() * 0.5f;
        float tr = 4f * l.k();
        Renderer2D.COLOR.circle(tx, ty, tr * 1.9f, argb(0xFFFFFF, a * 0.25f));
        Renderer2D.COLOR.circle(tx, ty, tr, argb(0xFFFFFF, a));
    }
}
