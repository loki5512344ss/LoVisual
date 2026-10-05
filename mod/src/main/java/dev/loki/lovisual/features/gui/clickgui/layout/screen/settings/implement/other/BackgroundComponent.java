package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.other;

import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.renderer2d.fx.backdrop.ThemeBackdrop2D;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;

public final class BackgroundComponent {

    /** The theme's shader backdrop, or both sides of the wipe while the theme is changing. */
    private static void renderBackdrop(float x, float y, float w, float h, float radius) {
        Themes.BackdropView view = Themes.backdropView();
        Themes.Theme theme = dev.loki.lovisual.features.theme.Theme.theme();
        float opacity = ClickGuiRenderer.getRenderAlphaMultiplier();
        Renderer2D r = Renderer2D.COLOR;
        if (view.wipe() < 1.0f) {
            ThemeBackdrop2D.draw(r, x, y, w, h, radius, view.outgoing(), theme.accent(), theme.textPrimary(), opacity,
                    ThemeBackdrop2D.Wipe.ERASE, view.wipe());
            ThemeBackdrop2D.draw(r, x, y, w, h, radius, view.target(), theme.accent(), theme.textPrimary(), opacity,
                    ThemeBackdrop2D.Wipe.REVEAL, view.wipe());
        } else {
            ThemeBackdrop2D.draw(r, x, y, w, h, radius, view.target(), theme.accent(), theme.textPrimary(), opacity,
                    ThemeBackdrop2D.Wipe.NONE, 1.0f);
        }
    }

    public void render(float x, float y, float w, float h, float scale) {
        SettingsGuiPalette palette = SettingsGuiPalette.current();
        ClickGuiRenderer.drawBlur(x, y, w, h, 8f * scale, 0xFF000000, 200f / 255f);
        LayoutRender2D.roundedQuad(
                x, y, w, h, 8f * scale,
                palette.menuWindowBgLeft(),
                palette.menuWindowBgRight(),
                palette.menuWindowBgRight(),
                palette.menuWindowBgLeft()
        );
        renderBackdrop(x, y, w, h, 8f * scale);
        LayoutRender2D.roundedStroke(x, y, w, h, 8f * scale, 0.1f * scale, palette.menuWindowStroke());
    }
}
