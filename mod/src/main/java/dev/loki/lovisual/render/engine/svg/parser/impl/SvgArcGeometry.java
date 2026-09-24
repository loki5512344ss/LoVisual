package dev.loki.lovisual.render.engine.svg.parser;

import java.awt.geom.Path2D;

/**
 * Elliptical-arc geometry for {@link SvgPathParser}: converts an SVG arc segment
 * into cubic Bezier approximations appended to the target path. Pure static
 * helpers - no parser cursor state is touched. Package-private helper
 * (impl subfolder, {@code parser} package).
 */
final class SvgArcGeometry {

    static final double EPS = 1e-7;

    private SvgArcGeometry() {
    }

    static void arcTo(Path2D.Double path,
                      double x1, double y1,
                      double x2, double y2,
                      double rx, double ry,
                      double rotDeg,
                      boolean largeArc,
                      boolean sweep) {
        if (Math.abs(x1 - x2) < EPS && Math.abs(y1 - y2) < EPS) return;
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        if (rx < EPS || ry < EPS) {
            path.lineTo(x2, y2);
            return;
        }

        double phi = Math.toRadians(rotDeg % 360.0);
        double cosPhi = Math.cos(phi);
        double sinPhi = Math.sin(phi);

        double dx2 = (x1 - x2) * 0.5;
        double dy2 = (y1 - y2) * 0.5;
        double x1p = cosPhi * dx2 + sinPhi * dy2;
        double y1p = -sinPhi * dx2 + cosPhi * dy2;

        double rxSq = rx * rx;
        double rySq = ry * ry;
        double x1pSq = x1p * x1p;
        double y1pSq = y1p * y1p;
        double lambda = x1pSq / rxSq + y1pSq / rySq;
        if (lambda > 1.0) {
            double s = Math.sqrt(lambda);
            rx *= s;
            ry *= s;
            rxSq = rx * rx;
            rySq = ry * ry;
        }

        double sign = (largeArc == sweep) ? -1.0 : 1.0;
        double num = rxSq * rySq - rxSq * y1pSq - rySq * x1pSq;
        double den = rxSq * y1pSq + rySq * x1pSq;
        double coef = den <= EPS ? 0.0 : sign * Math.sqrt(Math.max(0.0, num / den));

        double cxp = coef * (rx * y1p / ry);
        double cyp = coef * (-ry * x1p / rx);
        double cx = cosPhi * cxp - sinPhi * cyp + (x1 + x2) * 0.5;
        double cy = sinPhi * cxp + cosPhi * cyp + (y1 + y2) * 0.5;

        double ux = (x1p - cxp) / rx;
        double uy = (y1p - cyp) / ry;
        double vx = (-x1p - cxp) / rx;
        double vy = (-y1p - cyp) / ry;
        double theta1 = angleBetween(1.0, 0.0, ux, uy);
        double dTheta = angleBetween(ux, uy, vx, vy);
        if (!sweep && dTheta > 0.0) dTheta -= Math.PI * 2.0;
        else if (sweep && dTheta < 0.0) dTheta += Math.PI * 2.0;

        int segments = (int) Math.ceil(Math.abs(dTheta) / (Math.PI / 2.0));
        double seg = dTheta / segments;
        double t = theta1;
        for (int i = 0; i < segments; i++) {
            double t2 = t + seg;
            appendArcCubic(path, cx, cy, rx, ry, cosPhi, sinPhi, t, t2);
            t = t2;
        }
    }

    private static void appendArcCubic(Path2D.Double path,
                                       double cx, double cy,
                                       double rx, double ry,
                                       double cosPhi, double sinPhi,
                                       double t1, double t2) {
        double dt = t2 - t1;
        double alpha = Math.sin(dt) * (Math.sqrt(4.0 + 3.0 * Math.pow(Math.tan(dt * 0.5), 2.0)) - 1.0) / 3.0;
        double cosT1 = Math.cos(t1);
        double sinT1 = Math.sin(t1);
        double cosT2 = Math.cos(t2);
        double sinT2 = Math.sin(t2);
        double c1x = cosT1 - alpha * sinT1;
        double c1y = sinT1 + alpha * cosT1;
        double c2x = cosT2 + alpha * sinT2;
        double c2y = sinT2 - alpha * cosT2;
        double[] p1 = map(cx, cy, rx, ry, cosPhi, sinPhi, c1x, c1y);
        double[] p2 = map(cx, cy, rx, ry, cosPhi, sinPhi, c2x, c2y);
        double[] p = map(cx, cy, rx, ry, cosPhi, sinPhi, cosT2, sinT2);
        path.curveTo(p1[0], p1[1], p2[0], p2[1], p[0], p[1]);
    }

    private static double[] map(double cx, double cy, double rx, double ry, double cosPhi, double sinPhi, double x, double y) {
        double px = cx + rx * cosPhi * x - ry * sinPhi * y;
        double py = cy + rx * sinPhi * x + ry * cosPhi * y;
        return new double[]{px, py};
    }

    private static double angleBetween(double ux, double uy, double vx, double vy) {
        double dot = ux * vx + uy * vy;
        double len = Math.hypot(ux, uy) * Math.hypot(vx, vy);
        if (len <= EPS) return 0.0;
        double cos = Math.max(-1.0, Math.min(1.0, dot / len));
        double angle = Math.acos(cos);
        double cross = ux * vy - uy * vx;
        return cross < 0 ? -angle : angle;
    }
}
