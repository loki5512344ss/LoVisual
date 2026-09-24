package dev.loki.lovisual.render.helpers.clip;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.draw.box.UiBoxShape;
import dev.loki.lovisual.render.engine.renderer.ui.draw.box.UiChamferRadii;
import dev.loki.lovisual.render.engine.renderer.ui.draw.box.UiRect;
import dev.loki.lovisual.render.engine.renderer.ui.draw.corner.UiCornerKind;
import dev.loki.lovisual.render.engine.renderer.ui.draw.corner.UiCornerMode;
import dev.loki.lovisual.render.engine.renderer.ui.draw.corner.UiCornerRadii;
import dev.loki.lovisual.render.engine.renderer.ui.draw.corner.UiCornerSpec;
import dev.loki.lovisual.render.engine.renderer.ui.draw.shape.UiShape;

/**
 * Geometry rasterization for shape-clip masks. Builds vertex arrays from
 * rounded / chamfered rects, ellipses, polygons and flexible box shapes,
 * then emits them through {@link Renderer2D#COLOR}. Also drives the
 * batched / auto-flush sequence required by the render pipeline when
 * painting a mask into the stencil buffer.
 * Sibling folder sharing the parent's package.
 */
final class ClipGeometry {

    static final int MASK_COLOR = 0xFFFFFFFF;
    private static final double[] POINTS = new double[512];

    private ClipGeometry() { }

    /** Rasterize {@code shape} into the stencil write buffer; handles batched and auto-flush modes. */
    static void renderMaskShape(UiShape shape) {
        boolean auto = !Renderer2D.isBatching();
        if (auto) Renderer2D.COLOR.begin();
        emitMaskShape(Renderer2D.COLOR, shape);
        if (auto) Renderer2D.COLOR.render();
        else Renderer2D.flushBatch(Renderer2D.FlushReason.SCISSOR);
    }

    private static void emitMaskShape(Renderer2D r, UiShape shape) {
        UiRect b = shape.bounds();
        switch (shape.kind()) {
            case RECT -> emitRectMask(r, shape, b);
            case CIRCLE -> emitCircleMask(r, b, shape.radius());
            case POLYGON -> emitPolygonMask(r, shape);
            case FLEXIBLE_BOX -> emitBoxMask(r, shape.box(), b);
            default -> r.quad(b.x(), b.y(), b.width(), b.height(), MASK_COLOR);
        }
    }

    private static void emitRectMask(Renderer2D r, UiShape shape, UiRect b) {
        if (shape.cornerMode() == UiCornerMode.ROUNDED) { r.polygon(POINTS, buildRoundedRectPolygon(b, shape.roundedRadii()), MASK_COLOR); return; }
        if (shape.cornerMode() == UiCornerMode.CHAMFERED) { r.polygon(POINTS, buildChamferedRectPolygon(b, shape.chamferRadii()), MASK_COLOR); return; }
        r.quad(b.x(), b.y(), b.width(), b.height(), MASK_COLOR);
    }

    private static void emitCircleMask(Renderer2D r, UiRect b, float radius) {
        float cx = b.x() + b.width() * 0.5f, cy = b.y() + b.height() * 0.5f;
        float rad = radius > 0.0001f ? radius : Math.min(b.width(), b.height()) * 0.5f;
        r.polygon(POINTS, buildEllipsePolygon(cx, cy, rad, rad, 40), MASK_COLOR);
    }

    private static void emitPolygonMask(Renderer2D r, UiShape shape) {
        double[] pts = shape.points();
        int count = Math.min(shape.pointCount(), pts != null ? pts.length / 2 : 0);
        if (!shape.closed() || count < 3) { UiRect b = shape.bounds(); r.quad(b.x(), b.y(), b.width(), b.height(), MASK_COLOR); return; }
        r.polygon(pts, count, MASK_COLOR);
    }

    private static void emitBoxMask(Renderer2D r, UiBoxShape box, UiRect fb) {
        if (box == null || !box.hasOnlyStraightEdges()) { r.quad(fb.x(), fb.y(), fb.width(), fb.height(), MASK_COLOR); return; }
        int count = buildBoxPolygon(box);
        if (count >= 3) r.polygon(POINTS, count, MASK_COLOR);
        else r.quad(fb.x(), fb.y(), fb.width(), fb.height(), MASK_COLOR);
    }

    // ---- Polygon builders --------------------------------------------------------------------

    private static int buildRoundedRectPolygon(UiRect b, UiCornerRadii radii) {
        resetPoints();
        float x = b.x(), y = b.y(), w = b.width(), h = b.height();
        float maxR = Math.max(0f, Math.min(w, h) * 0.5f);
        float tl = Math.min(radii.topLeft(), maxR), tr = Math.min(radii.topRight(), maxR);
        float br = Math.min(radii.bottomRight(), maxR), bl = Math.min(radii.bottomLeft(), maxR);
        int n = 0;
        n = appendArc(n, x + tl, y + tl, tl, tl, 180.0, 270.0);
        n = appendArc(n, x + w - tr, y + tr, tr, tr, 270.0, 360.0);
        n = appendArc(n, x + w - br, y + h - br, br, br, 0.0, 90.0);
        n = appendArc(n, x + bl, y + h - bl, bl, bl, 90.0, 180.0);
        return n;
    }

    private static int buildChamferedRectPolygon(UiRect b, UiChamferRadii radii) {
        resetPoints();
        float x = b.x(), y = b.y(), w = b.width(), h = b.height();
        float tlX = clamp(radii.topLeftX(), 0, w * .5f), tlY = clamp(radii.topLeftY(), 0, h * .5f);
        float trX = clamp(radii.topRightX(), 0, w * .5f), trY = clamp(radii.topRightY(), 0, h * .5f);
        float brX = clamp(radii.bottomRightX(), 0, w * .5f), brY = clamp(radii.bottomRightY(), 0, h * .5f);
        float blX = clamp(radii.bottomLeftX(), 0, w * .5f), blY = clamp(radii.bottomLeftY(), 0, h * .5f);
        int n = 0;
        n = addPoint(n, x + tlX, y); n = addPoint(n, x + w - trX, y);
        n = addPoint(n, x + w, y + trY); n = addPoint(n, x + w, y + h - brY);
        n = addPoint(n, x + w - brX, y + h); n = addPoint(n, x + blX, y + h);
        n = addPoint(n, x, y + h - blY); n = addPoint(n, x, y + tlY);
        return n;
    }

    private static int buildEllipsePolygon(double cx, double cy, double rx, double ry, int requestedSegments) {
        resetPoints();
        int segments = Math.max(12, Math.min(96, requestedSegments)), n = 0;
        for (int i = 0; i < segments; i++) { double a = Math.PI * 2.0 * i / segments; n = addPoint(n, cx + Math.cos(a) * rx, cy + Math.sin(a) * ry); }
        return n;
    }

    private static int buildBoxPolygon(UiBoxShape box) {
        resetPoints();
        UiRect b = box.bounds();
        float x = b.x(), y = b.y(), w = b.width(), h = b.height();
        int n = 0;
        n = appendCorner(n, x, y, w, h, box.topLeft(), 0);
        n = appendCorner(n, x, y, w, h, box.topRight(), 1);
        n = appendCorner(n, x, y, w, h, box.bottomRight(), 2);
        n = appendCorner(n, x, y, w, h, box.bottomLeft(), 3);
        return n;
    }

    private static int appendCorner(int n, float x, float y, float w, float h, UiCornerSpec corner, int index) {
        UiCornerSpec c = corner != null ? corner : UiCornerSpec.SQUARE;
        if (c.kind() == UiCornerKind.ROUNDED || c.kind() == UiCornerKind.CONCAVE_ROUNDED) {
            float rx = Math.min(c.radiusX(), w * 0.5f), ry = Math.min(c.radiusY(), h * 0.5f);
            return switch (index) {
                case 0 -> appendArc(n, x + rx, y + ry, rx, ry, 180.0, 270.0);
                case 1 -> appendArc(n, x + w - rx, y + ry, rx, ry, 270.0, 360.0);
                case 2 -> appendArc(n, x + w - rx, y + h - ry, rx, ry, 0.0, 90.0);
                default -> appendArc(n, x + rx, y + h - ry, rx, ry, 90.0, 180.0);
            };
        }
        if (c.kind() == UiCornerKind.CHAMFERED || c.kind() == UiCornerKind.NOTCHED) {
            float cx = Math.min(c.cutX(), w * 0.5f), cy = Math.min(c.cutY(), h * 0.5f);
            return switch (index) {
                case 0 -> addChamfer(n, x + cx, y, x, y + cy);
                case 1 -> addChamfer(n, x + w - cx, y, x + w, y + cy);
                case 2 -> addChamfer(n, x + w, y + h - cy, x + w - cx, y + h);
                default -> addChamfer(n, x + cx, y + h, x, y + h - cy);
            };
        }
        return switch (index) {
            case 0 -> addPoint(n, x, y); case 1 -> addPoint(n, x + w, y);
            case 2 -> addPoint(n, x + w, y + h); default -> addPoint(n, x, y + h);
        };
    }

    private static int addChamfer(int n, double x0, double y0, double x1, double y1) { n = addPoint(n, x0, y0); return addPoint(n, x1, y1); }

    private static int appendArc(int n, double cx, double cy, double rx, double ry, double startDeg, double endDeg) {
        if (rx <= 0.0001 || ry <= 0.0001) return addPoint(n, cx, cy);
        int segments = Math.max(3, Math.min(12, (int) Math.ceil(Math.max(rx, ry) / 4.0)));
        for (int i = 0; i <= segments; i++) {
            double angle = Math.toRadians(startDeg + (endDeg - startDeg) * i / (double) segments);
            n = addPoint(n, cx + Math.cos(angle) * rx, cy + Math.sin(angle) * ry);
        }
        return n;
    }

    private static int addPoint(int n, double x, double y) {
        if (n > 0 && POINTS[(n - 1) * 2] == x && POINTS[(n - 1) * 2 + 1] == y) return n;
        if ((n + 1) * 2 > POINTS.length) return n;
        POINTS[n * 2] = x; POINTS[n * 2 + 1] = y; return n + 1;
    }

    private static void resetPoints() { /* active count returned by builders; stale values ignored */ }
    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
}
