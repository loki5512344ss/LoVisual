package dev.loki.lovisual.features.gui.mainmenu.hero.paint;

import dev.loki.lovisual.features.gui.mainmenu.MainMenuTextUtil;
import dev.loki.lovisual.features.gui.mainmenu.hero.logic.HeroLayout;
import dev.loki.lovisual.features.gui.mainmenu.hero.logic.HeroLayout.Layout;
import dev.loki.lovisual.features.gui.mainmenu.hero.logic.Rect;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.resources.language.I18n;

import static dev.loki.lovisual.features.gui.mainmenu.hero.paint.HeroPaint.*;

/** Hero menu buttons: accent primary, dark glass secondaries, a red-tinted quit, icon on the left. */
public final class HeroButtonPainter {
    private static final String[] LABEL_KEYS = {"multiplayer", "singleplayer", "accounts", "addons", "options", "quit"};
    private static final String[] ICONS = {"swords", "tree-deciduous", "user", "package", "columns-3-cog", "cross"};

    private HeroButtonPainter() {
    }

    public static void draw(Layout l, int index, float a, float hover, boolean badge) {
        float k = l.k();
        Rect r = l.buttons()[index];
        float rad = 9f * k;
        boolean compact = index >= HeroLayout.OPTIONS;
        fill(r, rad, k, a, hover, index);

        float iconSize = (compact ? 14f : 17f) * k;
        float iconX = r.x() + (compact ? 13f : 16f) * k + iconSize * 0.5f;
        float nudge = hover * 1.2f * k;
        icon(ICONS[index], iconX, r.y() + r.h() * 0.5f - nudge * 0.2f, iconSize + nudge, argb(0xFFFFFF, a * (0.85f + hover * 0.15f)));

        TextRenderer bold = MainMenuTextUtil.interBold(TextRenderer.get());
        float px = (compact ? l.sizes().small() : l.sizes().label());
        float textX = iconX + iconSize * 0.5f + (compact ? 8f : 11f) * k;
        textCentered(bold, I18n.get("screen.lovisual.hero_menu." + LABEL_KEYS[index]), textX, r.y(), r.h(), px, argb(0xFFFFFF, a));
        if (badge) {
            Renderer2D.COLOR.circle(r.x() + r.w() - 14f * k, r.y() + r.h() * 0.5f, 3.6f * k, argb(0xFFB74D, a));
        }
    }

    private static void fill(Rect r, float rad, float k, float a, float hover, int index) {
        int accent = Theme.theme().accent();
        if (index == HeroLayout.MULTIPLAYER) {
            Renderer2D.COLOR.roundedRectSoftShadow(r.x(), r.y() + 3f * k, r.w(), r.h(), rad, 18f * k, 0.05f + hover * 0.06f, argb(accent, a * 0.55f));
            int top = argb(mix(accent, 0xFFFFFF, 0.10f + hover * 0.12f), a);
            int bottom = argb(mix(accent, 0x000000, 0.10f - hover * 0.04f), a);
            Renderer2D.COLOR.roundedRectGradientQuad(r.x(), r.y(), r.w(), r.h(), rad, top, top, bottom, bottom);
            return;
        }
        boolean quit = index == HeroLayout.QUIT;
        int base = quit ? mix(0x1C1E26, 0xB0303C, hover * 0.8f) : mix(0x1C1E26, 0x2E3240, hover);
        Renderer2D.COLOR.roundedRectSoftShadow(r.x(), r.y() + 2f * k, r.w(), r.h(), rad, 10f * k, 0.03f, argb(0x000000, a * 0.4f));
        Renderer2D.COLOR.roundedRect(r.x(), r.y(), r.w(), r.h(), rad, argb(base, a * (0.76f + hover * 0.16f)));
        Renderer2D.COLOR.roundedRectStrokeGradient(r.x(), r.y(), r.w(), r.h(), rad, 1f,
                argb(0xFFFFFF, a * (0.10f + hover * 0.14f)), argb(0xFFFFFF, a * (0.03f + hover * 0.05f)), 90f);
    }
}
