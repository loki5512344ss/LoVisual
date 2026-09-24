package dev.loki.lovisual.render.engine.text.msdf;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.text.font.Font;
import dev.loki.lovisual.render.engine.text.glyph.GlyphFont;
import dev.loki.lovisual.render.engine.text.msdf.atlas.MsdfAtlas;
import dev.loki.lovisual.render.engine.text.msdf.atlas.MsdfGlyph;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import org.jetbrains.annotations.Nullable;

/**
 * Итерация глифов MSDF-атласа и генерация квадов: сплошной цвет,
 * линейный градиент (лево/право), квад-градиент (4 угла) и emitGlyphs.
 */
final class MsdfGlyphDrawer {
    private MsdfGlyphDrawer() {
    }

    static double render(MsdfAtlas atlas, float pixelScale, MeshBuilder mesh,
                         String string, double x, double y, RenderColor color, double scale) {
        double baseline = y + atlas.ascender * pixelScale * scale;
        int length = string.length();
        mesh.ensureCapacity(length * 4, length * 6);

        for (int i = 0; i < length; ) {
            int cp = string.codePointAt(i);
            MsdfGlyph g = glyph(atlas, cp);
            if (g == null) {
                i += Character.charCount(cp);
                continue;
            }
            if (g.visible()) {
                emit(mesh, g, quad(g, baseline, x, pixelScale * scale), color);
            }
            x += g.advance() * pixelScale * scale;
            i += Character.charCount(cp);
        }
        return x;
    }

    static double emitGlyphs(MsdfAtlas atlas, float pixelScale,
                             String string, double x, double y, double scale, GlyphFont.GlyphConsumer consumer) {
        double baseline = y + atlas.ascender * pixelScale * scale;
        int length = string.length();
        for (int i = 0; i < length; ) {
            int cp = string.codePointAt(i);
            MsdfGlyph g = glyph(atlas, cp);
            if (g == null) {
                i += Character.charCount(cp);
                continue;
            }
            if (g.visible()) {
                Quad q = quad(g, baseline, x, pixelScale * scale);
                consumer.accept(q.x0(), q.y0(), q.x1(), q.y1(), g.u0(), g.v0(), g.u1(), g.v1());
            }
            x += g.advance() * pixelScale * scale;
            i += Character.charCount(cp);
        }
        return x;
    }

    static double renderGradient(MsdfAtlas atlas, float pixelScale, MeshBuilder mesh,
                                 String string, double x, double y, double scale, Font.GlyphGradient gradient) {
        // Линейный градиент резолвит цвета даже у невидимых глифов (позиция пера), 1:1 со старым кодом.
        return drawCorners(atlas, pixelScale, mesh, string, x, y, scale, true, (glyphIndex, cp, penX, q, out) -> {
            gradient.colors(glyphIndex, cp, penX, out);
            int left = out[0];
            int right = out[1];
            out[0] = left;
            out[1] = left;
            out[2] = right;
            out[3] = right;
        });
    }

    static double renderQuadGradient(MsdfAtlas atlas, float pixelScale, MeshBuilder mesh,
                                     String string, double x, double y, double scale, Font.GlyphQuadGradient gradient) {
        return drawCorners(atlas, pixelScale, mesh, string, x, y, scale, false,
                (glyphIndex, cp, penX, q, out) -> gradient.colors(glyphIndex, cp, q.x0(), q.y0(), q.x1(), q.y1(), out));
    }

    private interface CornerResolver {
        void resolve(int glyphIndex, int cp, double penX, Quad q, int[] out);
    }

    private static double drawCorners(MsdfAtlas atlas, float pixelScale, MeshBuilder mesh,
                                      String string, double x, double y, double scale,
                                      boolean resolveInvisible, CornerResolver resolver) {
        double baseline = y + atlas.ascender * pixelScale * scale;
        int length = string.length();
        mesh.ensureCapacity(length * 4, length * 6);

        int[] colors = new int[4];
        int glyphIndex = 0;
        for (int i = 0; i < length; ) {
            int cp = string.codePointAt(i);
            MsdfGlyph g = glyph(atlas, cp);
            if (g == null) {
                i += Character.charCount(cp);
                glyphIndex++;
                continue;
            }

            Quad q = quad(g, baseline, x, pixelScale * scale);
            if (g.visible()) {
                resolver.resolve(glyphIndex, cp, x, q, colors);
                emit(mesh, g, q, colors[0], colors[1], colors[2], colors[3]);
            } else if (resolveInvisible) {
                resolver.resolve(glyphIndex, cp, x, q, colors);
            }

            x += g.advance() * pixelScale * scale;
            i += Character.charCount(cp);
            glyphIndex++;
        }
        return x;
    }

    private static @Nullable MsdfGlyph glyph(MsdfAtlas atlas, int codePoint) {
        MsdfGlyph g = atlas.glyphs.get(codePoint);
        return g != null ? g : atlas.fallback;
    }

    private record Quad(double x0, double y0, double x1, double y1) {
    }

    private static Quad quad(MsdfGlyph g, double baseline, double x, double scalePx) {
        return new Quad(
                x + g.left() * scalePx,
                baseline + g.top() * scalePx,
                x + g.right() * scalePx,
                baseline + g.bottom() * scalePx
        );
    }

    private static void emit(MeshBuilder mesh, MsdfGlyph g, Quad q, RenderColor color) {
        int i1 = mesh.vec2(q.x0(), q.y0()).raw2(g.u0(), g.v0()).color(color.r, color.g, color.b, color.a).next();
        int i2 = mesh.vec2(q.x0(), q.y1()).raw2(g.u0(), g.v1()).color(color.r, color.g, color.b, color.a).next();
        int i3 = mesh.vec2(q.x1(), q.y1()).raw2(g.u1(), g.v1()).color(color.r, color.g, color.b, color.a).next();
        int i4 = mesh.vec2(q.x1(), q.y0()).raw2(g.u1(), g.v0()).color(color.r, color.g, color.b, color.a).next();
        mesh.quad(i1, i2, i3, i4);
    }

    private static void emit(MeshBuilder mesh, MsdfGlyph g, Quad q,
                             int topLeft, int bottomLeft, int bottomRight, int topRight) {
        int i1 = mesh.vec2(q.x0(), q.y0()).raw2(g.u0(), g.v0())
                .color(red(topLeft), green(topLeft), blue(topLeft), alpha(topLeft)).next();
        int i2 = mesh.vec2(q.x0(), q.y1()).raw2(g.u0(), g.v1())
                .color(red(bottomLeft), green(bottomLeft), blue(bottomLeft), alpha(bottomLeft)).next();
        int i3 = mesh.vec2(q.x1(), q.y1()).raw2(g.u1(), g.v1())
                .color(red(bottomRight), green(bottomRight), blue(bottomRight), alpha(bottomRight)).next();
        int i4 = mesh.vec2(q.x1(), q.y0()).raw2(g.u1(), g.v0())
                .color(red(topRight), green(topRight), blue(topRight), alpha(topRight)).next();
        mesh.quad(i1, i2, i3, i4);
    }

    private static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    private static int red(int argb) {
        return (argb >>> 16) & 0xFF;
    }

    private static int green(int argb) {
        return (argb >>> 8) & 0xFF;
    }

    private static int blue(int argb) {
        return argb & 0xFF;
    }
}
