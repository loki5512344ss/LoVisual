package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.look;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;

import java.util.EnumMap;
import java.util.Map;

/** Pill row in the Themes header that switches between {@link ThemesPage}s. */
public final class ThemesPageSwitch {
    private final Map<ThemesPage, float[]> rects = new EnumMap<>(ThemesPage.class);
    private final Map<ThemesPage, Float> anim = new EnumMap<>(ThemesPage.class);
    private ThemesPage page = ThemesPage.THEMES;

    public ThemesPage page() {
        return page;
    }

    public void render(float menuX, float menuY, float menuW, float s) {
        SettingsGuiPalette palette = SettingsGuiPalette.current();
        float size = 7.5f * s;
        float h = 14f * s;
        float total = 0f;
        for (ThemesPage p : ThemesPage.values()) total += width(p, size, s) + 4f * s;
        float x = menuX + menuW - total - 6f * s;
        float y = menuY + 7f * s;
        for (ThemesPage p : ThemesPage.values()) {
            float w = width(p, size, s);
            rects.put(p, new float[]{x, y, w, h});
            float a = anim.merge(p, p == page ? 1f : 0f, (old, t) -> AnimationUtility.approach(old, t, 0.2f));
            int fill = SettingsGuiPalette.mix(palette.panelPillBase(), palette.panelPillActive(), a);
            LayoutRender2D.roundedQuad(x, y, w, h, h * 0.5f, fill, fill, fill, fill);
            int text = SettingsGuiPalette.mix(palette.panelMuted(), palette.panelText(), a);
            float th = ClickGuiRenderer.textHeight(ClickGuiRenderer.getInterMedium(), size);
            ClickGuiRenderer.drawText(ClickGuiRenderer.getInterMedium(), p.title(), x + 7f * s, y + (h - th) / 2f, size, text, false);
            x += w + 4f * s;
        }
    }

    /** Switches page when a pill was hit; returns true if the click was on the row. */
    public boolean click(float mx, float my, int button) {
        if (button != 0) return false;
        for (Map.Entry<ThemesPage, float[]> e : rects.entrySet()) {
            float[] r = e.getValue();
            if (ClickGuiMath.insideRect(mx, my, r[0], r[1], r[2], r[3])) {
                page = e.getKey();
                return true;
            }
        }
        return false;
    }

    private static float width(ThemesPage p, float size, float s) {
        return ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterMedium(), p.title(), size) + 14f * s;
    }
}
