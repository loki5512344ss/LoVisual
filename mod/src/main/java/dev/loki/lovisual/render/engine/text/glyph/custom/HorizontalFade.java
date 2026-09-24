package dev.loki.lovisual.render.engine.text.glyph.custom;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.text.font.Font;
import dev.loki.lovisual.render.engine.text.glyph.GlyphFont;

/**
 * Горизонтальный fade-градиент текста: альфа каждого края глифа плавно
 * гаснет у границ клипа [clipLeft, clipRight] с шириной зоны fade.
 */
public final class HorizontalFade {
    private HorizontalFade() {
    }

    public static Font.GlyphGradient gradient(GlyphFont font,
                                              RenderColor color,
                                              double clipLeft,
                                              double clipRight,
                                              double fadeLeft,
                                              double fadeRight,
                                              double glyphScale) {
        int argb = ((color.a & 0xFF) << 24)
                | ((color.r & 0xFF) << 16)
                | ((color.g & 0xFF) << 8)
                | (color.b & 0xFF);
        double leftFade = Math.max(0.0, fadeLeft);
        double rightFade = Math.max(0.0, fadeRight);

        return (index, cp, glyphX, out) -> {
            double glyphAdvance = glyphAdvance(font, cp) * glyphScale;
            double leftAlpha = fadeAlphaAt(glyphX, clipLeft, clipRight, leftFade, rightFade);
            double rightAlpha = fadeAlphaAt(glyphX + glyphAdvance, clipLeft, clipRight, leftFade, rightFade);
            out[0] = scaleArgbAlpha(argb, leftAlpha);
            out[1] = scaleArgbAlpha(argb, rightAlpha);
        };
    }

    private static double glyphAdvance(GlyphFont font, int codePoint) {
        String glyph = new String(Character.toChars(codePoint));
        return font.getWidth(glyph, glyph.length());
    }

    private static double fadeAlphaAt(double x,
                                      double clipLeft,
                                      double clipRight,
                                      double fadeLeft,
                                      double fadeRight) {
        if (clipRight <= clipLeft) return 1.0;
        if (x <= clipLeft || x >= clipRight) return 0.0;

        double alpha = 1.0;
        if (fadeLeft > 0.0 && x < clipLeft + fadeLeft) {
            alpha = Math.min(alpha, (x - clipLeft) / fadeLeft);
        }
        if (fadeRight > 0.0 && x > clipRight - fadeRight) {
            alpha = Math.min(alpha, (clipRight - x) / fadeRight);
        }
        return clamp01(alpha);
    }

    private static int scaleArgbAlpha(int argb, double factor) {
        int a = (argb >>> 24) & 0xFF;
        int scaled = (int) Math.round(a * clamp01(factor));
        return (argb & 0x00FFFFFF) | ((scaled & 0xFF) << 24);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
