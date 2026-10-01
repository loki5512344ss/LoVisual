package dev.loki.lovisual.features.gui.mainmenu.hero.paint;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.mainmenu.MainMenuTextUtil;
import dev.loki.lovisual.features.gui.mainmenu.hero.logic.HeroLayout;
import dev.loki.lovisual.features.gui.mainmenu.hero.logic.TextMetrics;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.util.Mth;

/** Small colour and text helpers shared by the hero menu painters. */
public final class HeroPaint {
    private HeroPaint() {
    }

    public static int argb(int rgb, float alpha) {
        return (rgb & 0x00FFFFFF) | (Mth.clamp(Math.round(alpha * 255f), 0, 255) << 24);
    }

    public static int mix(int a, int b, float t) {
        return HudRenderUtil.mixColor(a, b, t);
    }

    public static float easeOutCubic(float x) {
        return 1f - (float) Math.pow(1f - Mth.clamp(x, 0f, 1f), 3f);
    }

    /** Draws text sized in logical pixels with the given weight renderer. */
    public static void text(TextRenderer r, String text, float x, float y, float px, int argb) {
        MainMenuTextUtil.drawText(r, text, x, y, HeroLayout.font(px), argb);
    }

    public static float width(TextRenderer r, String text, float px) {
        return MainMenuTextUtil.measureWidth(r, text, HeroLayout.font(px));
    }

    public static float height(TextRenderer r, float px) {
        return MainMenuTextUtil.measureHeight(r, HeroLayout.font(px));
    }

    /** Text vertically centred inside a box. */
    public static void textCentered(TextRenderer r, String text, float x, float y, float h, float px, int argb) {
        text(r, text, x, y + (h - height(r, px)) * 0.5f, px, argb);
    }

    /** Real line heights of the bold UI font, so the layout adapts to the actual glyph metrics. */
    public static TextMetrics metrics() {
        TextRenderer bold = MainMenuTextUtil.interBold(TextRenderer.get());
        return px -> height(bold, px);
    }

    /** Draws a tinted SVG icon centred on {@code (cx, cy)}. */
    public static void icon(String svg, float cx, float cy, float size, int argb) {
        Renderer2D.COLOR.svg(svg, cx - size * 0.5f, cy - size * 0.5f, size, size, SvgRenderOptions.overrideColor(argb));
    }
}
