package dev.loki.lovisual.render.engine.svg.backend;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;
import dev.loki.lovisual.render.engine.svg.model.SvgColorMode;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.engine.svg.model.SvgStyleUtil;

/**
 * Геометрия SVG → quads (триангуляция). Вынесено из гиганта SvgMeshBackend.
 */
final class SvgGeometry {
    private SvgGeometry() {}

    static SvgMeshBackend.CompiledGeometry getOrCompile(SvgMeshBackend.SvgDoc doc, double width, double height, SvgRenderOptions options) {
        int w16 = quantize(width, 16.0);
        int h16 = quantize(height, 16.0);
        int alpha1024 = quantize(options.alpha(), 1024.0);
        int flatness1024 = quantize(options.curveFlatness(), 1024.0);
        int gradientAngle1024 = quantize(options.gradientAngleDegrees(), 1024.0);
        SvgMeshBackend.GeometryKey key = new SvgMeshBackend.GeometryKey(doc.cacheKey(), w16, h16, options.colorMode(), options.overrideArgb(), options.gradientStartArgb(), options.gradientEndArgb(), gradientAngle1024, alpha1024, flatness1024);
        synchronized (SvgMeshBackend.GEOMETRY_CACHE) {
            SvgMeshBackend.CompiledGeometry cached = SvgMeshBackend.GEOMETRY_CACHE.get(key);
            if (cached != null) { cached.hits++; return cached; }
        }
        SvgMeshBackend.CompiledGeometry compiled = compileGeometry(doc, width, height, options);
        synchronized (SvgMeshBackend.GEOMETRY_CACHE) {
            SvgMeshBackend.CompiledGeometry raced = SvgMeshBackend.GEOMETRY_CACHE.get(key);
            if (raced != null) { raced.hits++; return raced; }
            compiled.hits++; SvgMeshBackend.GEOMETRY_CACHE.put(key, compiled);
        }
        return compiled;
    }

    static int quantize(double value, double scale) { return (int) Math.round(value * scale); }

    static SvgMeshBackend.CompiledGeometry compileGeometry(SvgMeshBackend.SvgDoc doc, double width, double height, SvgRenderOptions options) {
        ArrayList<SvgMeshBackend.QuadCmd> quads = new ArrayList<>(512);
        SvgMeshBackend.QuadSink sink = (x, y, w, h, argb) -> quads.add(new SvgMeshBackend.QuadCmd(x, y, w, h, argb));
        AffineTransform view = new AffineTransform();
        view.scale(width / doc.vw(), height / doc.vh());
        view.translate(-doc.vx(), -doc.vy());
        for (SvgMeshBackend.SvgShape shape : doc.shapes()) {
            AffineTransform tx = new AffineTransform(view);
            tx.concatenate(shape.transform());
            drawShape(shape, tx, width, height, options, sink);
        }
        return new SvgMeshBackend.CompiledGeometry(quads);
    }

    static void drawShape(SvgMeshBackend.SvgShape shape, AffineTransform tx, double viewportW, double viewportH, SvgRenderOptions options, SvgMeshBackend.QuadSink sink) {
        Shape transformed = tx.createTransformedShape(shape.path());
        int fill = SvgStyleUtil.resolvePaint(shape.style().fill, shape.style().fillOpacity, shape.style().groupOpacity, options);
        if (((fill >>> 24) & 0xFF) > 0) {
            if (options.colorMode() == SvgColorMode.GRADIENT_LINEAR) fillShapeGradient(transformed, shape.windingRule(), fill, options.curveFlatness(), viewportW, viewportH, options, sink);
            else fillShape(transformed, shape.windingRule(), fill, options.curveFlatness(), sink);
        }
        int stroke = SvgStyleUtil.resolvePaint(shape.style().stroke, shape.style().strokeOpacity, shape.style().groupOpacity, options);
        if (((stroke >>> 24) & 0xFF) <= 0) return;
        double scale = estimateScale(tx);
        float strokeWidth = Math.max(0.0f, (float) (shape.style().strokeWidth * scale));
        if (strokeWidth <= 0.0f) return;
        BasicStroke bs = new BasicStroke(strokeWidth, shape.style().lineCap, shape.style().lineJoin, shape.style().miterLimit);
        Shape stroked = bs.createStrokedShape(transformed);
        if (options.colorMode() == SvgColorMode.GRADIENT_LINEAR) fillShapeGradient(stroked, Path2D.WIND_NON_ZERO, stroke, options.curveFlatness(), viewportW, viewportH, options, sink);
        else fillShape(stroked, Path2D.WIND_NON_ZERO, stroke, options.curveFlatness(), sink);
    }

    static double estimateScale(AffineTransform tx) {
        double sx = Math.hypot(tx.getScaleX(), tx.getShearX());
        double sy = Math.hypot(tx.getScaleY(), tx.getShearY());
        if (sx <= SvgMeshBackend.EPS && sy <= SvgMeshBackend.EPS) return 1.0;
        if (sx <= SvgMeshBackend.EPS) return sy;
        if (sy <= SvgMeshBackend.EPS) return sx;
        return 0.5 * (sx + sy);
    }

    static void fillShape(Shape shape, int winding, int argb, float flatness, SvgMeshBackend.QuadSink sink) { fillShapeInternal(shape, winding, argb, flatness, 1.0, 1.0, null, sink); }

    static void fillShapeGradient(Shape shape, int winding, int alphaMask, float flatness, double viewportW, double viewportH, SvgRenderOptions options, SvgMeshBackend.QuadSink sink) { fillShapeInternal(shape, winding, alphaMask, flatness, viewportW, viewportH, options, sink); }

    static void fillShapeInternal(Shape shape, int winding, int argb, float flatness, double viewportW, double viewportH, SvgRenderOptions gradientOptions, SvgMeshBackend.QuadSink sink) {
        ArrayList<SvgMeshBackend.Edge> edges = new ArrayList<>(128);
        SvgMeshBackend.Bounds b = collectEdges(shape.getPathIterator(null, Math.max(0.05, flatness)), edges);
        if (edges.isEmpty()) return;
        int yMin = (int) Math.floor(b.minY());
        int yMax = (int) Math.ceil(b.maxY());
        if (yMax <= yMin) return;
        ArrayList<SvgMeshBackend.Crossing> xs = new ArrayList<>(64);
        boolean evenOdd = winding == Path2D.WIND_EVEN_ODD;
        for (int y = yMin; y < yMax; y++) {
            double scanY = y + 0.5;
            xs.clear();
            for (SvgMeshBackend.Edge e : edges) {
                if (Math.abs(e.y2() - e.y1()) <= SvgMeshBackend.EPS) continue;
                boolean up = e.y1() <= scanY && e.y2() > scanY;
                boolean down = e.y2() <= scanY && e.y1() > scanY;
                if (!up && !down) continue;
                double x = e.x1() + (scanY - e.y1()) * (e.x2() - e.x1()) / (e.y2() - e.y1());
                xs.add(new SvgMeshBackend.Crossing(x, e.wind()));
            }
            if (xs.isEmpty()) continue;
            xs.sort((a, c) -> Double.compare(a.x(), c.x()));
            if (evenOdd) {
                for (int i = 0; i + 1 < xs.size(); i += 2) {
                    double x0 = xs.get(i).x(); double x1 = xs.get(i + 1).x();
                    if (x1 - x0 > SvgMeshBackend.EPS) sink.quad(x0, y, x1 - x0, 1.0, gradientOptions == null ? argb : gradientColor(gradientOptions, argb, x0 + (x1 - x0) * 0.5, scanY, viewportW, viewportH));
                }
                continue;
            }
            int wind = 0; double start = 0.0;
            for (SvgMeshBackend.Crossing c : xs) {
                int prev = wind; wind += c.wind();
                if (prev == 0 && wind != 0) start = c.x();
                else if (prev != 0 && wind == 0) {
                    if (c.x() - start > SvgMeshBackend.EPS) sink.quad(start, y, c.x() - start, 1.0, gradientOptions == null ? argb : gradientColor(gradientOptions, argb, start + (c.x() - start) * 0.5, scanY, viewportW, viewportH));
                }
            }
        }
    }

    static int gradientColor(SvgRenderOptions options, int alphaMask, double x, double y, double viewportW, double viewportH) {
        double angle = Math.toRadians(options.gradientAngleDegrees());
        double dx = Math.cos(angle); double dy = Math.sin(angle);
        double w = Math.max(1.0, Math.abs(viewportW)); double h = Math.max(1.0, Math.abs(viewportH));
        double extent = Math.abs(dx) * w + Math.abs(dy) * h;
        double centered = (x - w * 0.5) * dx + (y - h * 0.5) * dy;
        float t = clamp01((float) (centered / Math.max(1.0, extent) + 0.5));
        int mixed = mixArgb(options.gradientStartArgb(), options.gradientEndArgb(), t);
        int maskA = (alphaMask >>> 24) & 0xFF; int mixedA = (mixed >>> 24) & 0xFF;
        int a = clamp255(Math.round(maskA * (mixedA / 255.0f)));
        return (a << 24) | (mixed & 0x00FFFFFF);
    }

    static int mixArgb(int a, int b, float t) {
        float k = clamp01(t);
        int aa = (a >>> 24) & 0xFF; int ar = (a >>> 16) & 0xFF; int ag = (a >>> 8) & 0xFF; int ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF; int br = (b >>> 16) & 0xFF; int bg = (b >>> 8) & 0xFF; int bb = b & 0xFF;
        int oa = Math.round(aa + (ba - aa) * k); int or = Math.round(ar + (br - ar) * k);
        int og = Math.round(ag + (bg - ag) * k); int ob = Math.round(ab + (bb - ab) * k);
        return (clamp255(oa) << 24) | (clamp255(or) << 16) | (clamp255(og) << 8) | clamp255(ob);
    }

    static SvgMeshBackend.Bounds collectEdges(PathIterator it, List<SvgMeshBackend.Edge> out) {
        double[] seg = new double[6]; double sx = 0.0, sy = 0.0, px = 0.0, py = 0.0;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        while (!it.isDone()) {
            int type = it.currentSegment(seg);
            switch (type) {
                case PathIterator.SEG_MOVETO -> { sx = seg[0]; sy = seg[1]; px = sx; py = sy; minY = Math.min(minY, sy); maxY = Math.max(maxY, sy); }
                case PathIterator.SEG_LINETO -> { addEdge(out, px, py, seg[0], seg[1]); px = seg[0]; py = seg[1]; minY = Math.min(minY, py); maxY = Math.max(maxY, py); }
                case PathIterator.SEG_CLOSE -> { addEdge(out, px, py, sx, sy); px = sx; py = sy; }
                default -> {}
            }
            it.next();
        }
        if (!Double.isFinite(minY) || !Double.isFinite(maxY)) return new SvgMeshBackend.Bounds(0.0, 0.0);
        return new SvgMeshBackend.Bounds(minY, maxY);
    }

    static void addEdge(List<SvgMeshBackend.Edge> out, double x1, double y1, double x2, double y2) {
        if (Math.abs(x1 - x2) <= SvgMeshBackend.EPS && Math.abs(y1 - y2) <= SvgMeshBackend.EPS) return;
        out.add(new SvgMeshBackend.Edge(x1, y1, x2, y2, y1 < y2 ? 1 : -1));
    }

    static float clamp01(float v) { if (v <= 0.0f) return 0.0f; if (v >= 1.0f) return 1.0f; return v; }
    static int clamp255(int v) { if (v <= 0) return 0; if (v >= 255) return 255; return v; }
}
