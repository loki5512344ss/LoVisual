package dev.loki.lovisual.render.engine.svg.parser;

import java.awt.geom.Path2D;

/**
 * Curve and arc SVG path commands for {@link SvgPathParser}: cubic / smooth-cubic /
 * quadratic / smooth-quadratic / elliptical-arc handlers. Each reads numbers via the
 * parser cursor and updates the control-point reflection and previous-command state.
 * Linear handlers live in {@link SvgPathCommands}; arc-to-Bezier math in
 * {@link SvgArcGeometry}. Package-private helper (impl subfolder, {@code parser} package).
 */
final class SvgPathCurves {

    private SvgPathCurves() {
    }

    static void parseCubic(SvgPathParser p, Path2D.Double out, boolean rel) {
        while (p.hasNumber()) {
            double x1 = p.readNumber();
            double y1 = p.readNumber();
            double x2 = p.readNumber();
            double y2 = p.readNumber();
            double x = p.readNumber();
            double y = p.readNumber();
            if (Double.isNaN(x1) || Double.isNaN(y1) || Double.isNaN(x2) || Double.isNaN(y2) || Double.isNaN(x) || Double.isNaN(y))
                return;
            if (rel) {
                x1 += p.cx;
                y1 += p.cy;
                x2 += p.cx;
                y2 += p.cy;
                x += p.cx;
                y += p.cy;
            }
            out.curveTo(x1, y1, x2, y2, x, y);
            p.lastCubicX = x2;
            p.lastCubicY = y2;
            p.cx = x;
            p.cy = y;
            p.prevCmd = 'c';
        }
    }

    static void parseSmoothCubic(SvgPathParser p, Path2D.Double out, boolean rel) {
        while (p.hasNumber()) {
            double x2 = p.readNumber();
            double y2 = p.readNumber();
            double x = p.readNumber();
            double y = p.readNumber();
            if (Double.isNaN(x2) || Double.isNaN(y2) || Double.isNaN(x) || Double.isNaN(y)) return;
            if (rel) {
                x2 += p.cx;
                y2 += p.cy;
                x += p.cx;
                y += p.cy;
            }
            double x1 = p.cx;
            double y1 = p.cy;
            if (p.prevCmd == 'c' || p.prevCmd == 's') {
                x1 = 2.0 * p.cx - p.lastCubicX;
                y1 = 2.0 * p.cy - p.lastCubicY;
            }
            out.curveTo(x1, y1, x2, y2, x, y);
            p.lastCubicX = x2;
            p.lastCubicY = y2;
            p.cx = x;
            p.cy = y;
            p.prevCmd = 's';
        }
    }

    static void parseQuadratic(SvgPathParser p, Path2D.Double out, boolean rel) {
        while (p.hasNumber()) {
            double x1 = p.readNumber();
            double y1 = p.readNumber();
            double x = p.readNumber();
            double y = p.readNumber();
            if (Double.isNaN(x1) || Double.isNaN(y1) || Double.isNaN(x) || Double.isNaN(y)) return;
            if (rel) {
                x1 += p.cx;
                y1 += p.cy;
                x += p.cx;
                y += p.cy;
            }
            out.quadTo(x1, y1, x, y);
            p.lastQuadX = x1;
            p.lastQuadY = y1;
            p.cx = x;
            p.cy = y;
            p.prevCmd = 'q';
        }
    }

    static void parseSmoothQuadratic(SvgPathParser p, Path2D.Double out, boolean rel) {
        while (p.hasNumber()) {
            double x = p.readNumber();
            double y = p.readNumber();
            if (Double.isNaN(x) || Double.isNaN(y)) return;
            if (rel) {
                x += p.cx;
                y += p.cy;
            }
            double x1 = p.cx;
            double y1 = p.cy;
            if (p.prevCmd == 'q' || p.prevCmd == 't') {
                x1 = 2.0 * p.cx - p.lastQuadX;
                y1 = 2.0 * p.cy - p.lastQuadY;
            }
            out.quadTo(x1, y1, x, y);
            p.lastQuadX = x1;
            p.lastQuadY = y1;
            p.cx = x;
            p.cy = y;
            p.prevCmd = 't';
        }
    }

    static void parseArc(SvgPathParser p, Path2D.Double out, boolean rel) {
        while (p.hasNumber()) {
            double rx = p.readNumber();
            double ry = p.readNumber();
            double angle = p.readNumber();
            double largeArc = p.readNumber();
            double sweep = p.readNumber();
            double x = p.readNumber();
            double y = p.readNumber();
            if (Double.isNaN(rx) || Double.isNaN(ry) || Double.isNaN(angle) || Double.isNaN(largeArc)
                    || Double.isNaN(sweep) || Double.isNaN(x) || Double.isNaN(y)) return;
            if (rel) {
                x += p.cx;
                y += p.cy;
            }
            SvgArcGeometry.arcTo(out, p.cx, p.cy, x, y, rx, ry, angle, largeArc != 0.0, sweep != 0.0);
            p.cx = x;
            p.cy = y;
            p.prevCmd = 'a';
        }
    }
}
