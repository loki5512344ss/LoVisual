package dev.loki.lovisual.features.gui.mainmenu.hero.paint;

import dev.loki.lovisual.config.i18n.MainConfig;
import dev.loki.lovisual.features.account.SkinManager;
import dev.loki.lovisual.features.gui.mainmenu.MainMenuTextUtil;
import dev.loki.lovisual.features.gui.mainmenu.hero.logic.HeroLayout;
import dev.loki.lovisual.features.gui.mainmenu.hero.logic.HeroLayout.Layout;
import dev.loki.lovisual.features.gui.mainmenu.hero.logic.Rect;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.postprocess.render.MenuBackgroundRenderer;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.player.PlayerHeadRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import static dev.loki.lovisual.features.gui.mainmenu.hero.paint.HeroPaint.*;

/** Draw pass of the hero menu: background, vignette, account card, hero text, buttons and footer. */
public final class HeroMenuRenderer {
    private HeroMenuRenderer() {
    }

    /** Draws the shader background; returns false when the custom menu is switched off. */
    public static boolean background(Minecraft mc) {
        MainConfig cfg = MainConfig.get();
        String mode = cfg != null ? cfg.getMenuBackgroundMode() : "off";
        if (mode == null || mode.equalsIgnoreCase("off")) return false;
        MenuBackgroundRenderer.render(mc, mode.equalsIgnoreCase("aurora"));
        return true;
    }

    public static void vignette(float w, float h, float a) {
        int dark = argb(0x0A0C10, a * 0.88f);
        int mid = argb(0x0A0C10, a * 0.45f);
        int clear = argb(0x0A0C10, 0f);
        Renderer2D.COLOR.quad(0, 0, w * 0.27f, h, dark, mid, mid, dark);
        Renderer2D.COLOR.quad(w * 0.27f, 0, w * 0.27f, h, mid, clear, clear, mid);
    }

    public static void account(GuiGraphicsExtractor ctx, Layout l, float a, float hover, String name, boolean needsAuth) {
        float k = l.k();
        Rect r = l.account();
        int accent = Theme.theme().accent();
        Renderer2D.COLOR.roundedRect(r.x(), r.y(), l.avatarSize(), l.avatarSize(), 6f * k, argb(0xFFFFFF, a * (0.07f + hover * 0.07f)));
        PlayerHeadRenderer.drawRounded(ctx, r.x(), r.y(), l.avatarSize(), 6f * k, SkinManager.getSkin(name),
                new RenderColor(argb(0xFFFFFF, a)), true, new RenderColor(argb(0xFFFFFF, a * 0.14f)), 0.8f, false);
        TextRenderer bold = MainMenuTextUtil.interBold(TextRenderer.get());
        TextRenderer medium = MainMenuTextUtil.interMedium(bold);
        float tx = r.x() + l.avatarSize() + l.sizes().footer();
        String caption = needsAuth ? I18n.get("screen.lovisual.hero_menu.sign_in") : I18n.get("screen.lovisual.hero_menu.account");
        float capH = height(medium, 8.5f * k);
        float nameH = height(bold, 11f * k);
        float top = r.y() + (l.avatarSize() - capH - nameH - 2f * k) * 0.5f;
        text(medium, caption, tx, top, 8.5f * k, needsAuth ? argb(0xFFB74D, a) : argb(0xFFFFFF, a * 0.55f));
        text(bold, name, tx, top + capH + 2f * k, 11f * k, argb(0xFFFFFF, a));
        if (needsAuth) {
            Renderer2D.COLOR.circle(r.x() + l.avatarSize() - 2f * k, r.y() + 2f * k, 3.4f * k, argb(0xFFB74D, a));
        }
        if (hover > 0.01f) {
            Renderer2D.COLOR.roundedRectStrokeGradient(r.x() - 4f * k, r.y() - 4f * k, r.w() + 8f * k, r.h() + 8f * k, l.sizes().footer(), 1f,
                    argb(accent, a * hover * 0.5f), argb(accent, a * hover * 0.2f), 90f);
        }
    }

    public static void hero(Layout l, float a, String name) {
        TextRenderer bold = MainMenuTextUtil.interBold(TextRenderer.get());
        TextRenderer medium = MainMenuTextUtil.interMedium(bold);
        HeroLayout.Sizes sz = l.sizes();
        text(bold, I18n.get("screen.lovisual.hero_menu.greeting", name.toUpperCase()), l.heroX(), l.tagY(), sz.tag(), argb(Theme.theme().accent(), a));
        text(bold, I18n.get("screen.lovisual.hero_menu.title1"), l.heroX(), l.titleY1(), sz.title(), argb(0xFFFFFF, a));
        text(bold, I18n.get("screen.lovisual.hero_menu.title2"), l.heroX(), l.titleY2(), sz.title(), argb(0xFFFFFF, a));
        text(medium, I18n.get("screen.lovisual.hero_menu.subtitle"), l.heroX(), l.subY(), sz.sub(), argb(0xFFFFFF, a * 0.65f));
    }

    public static void footer(Layout l, float a, String version) {
        TextRenderer medium = MainMenuTextUtil.interMedium(MainMenuTextUtil.interBold(TextRenderer.get()));
        int dim = argb(0xFFFFFF, a * 0.45f);
        text(medium, I18n.get("screen.lovisual.hero_menu.disclaimer1"), l.heroX(), l.footerY(), l.sizes().footer(), dim);
        text(medium, I18n.get("screen.lovisual.hero_menu.disclaimer2"), l.heroX(), l.footerY() + l.footerLine(), l.sizes().footer(), dim);
        text(medium, I18n.get("screen.lovisual.hero_menu.version", version), l.heroX(), l.footerY() + l.footerLine() * 2f, l.sizes().footer(), argb(0xFFFFFF, a * 0.55f));
    }
}
