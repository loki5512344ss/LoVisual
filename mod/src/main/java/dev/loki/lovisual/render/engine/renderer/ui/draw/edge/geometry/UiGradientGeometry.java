
package dev.loki.lovisual.render.engine.renderer.ui.draw.edge;

public final class UiGradientGeometry {
    private UiGradientGeometry() {}
    public static void computeLinearGradientColors(float width, float height, int startArgb, int endArgb, float angleDeg, float offsetPx, int[] out) {
        float angle = (float) Math.toRadians(angleDeg);
        float dirX = (float) Math.cos(angle);
        float dirY = (float) Math.sin(angle);
        float p0 = 0.0f;
        float p1 = width * dirX;
        float p2 = height * dirY;
        float p3 = width * dirX + height * dirY;
        float minProj = Math.min(Math.min(p0, p1), Math.min(p2, p3));
        float maxProj = Math.max(Math.max(p0, p1), Math.max(p2, p3));
        float range = Math.max(0.0001f, maxProj - minProj);
        float t0 = clamp01((p0 + offsetPx - minProj) / range);
        float t1 = clamp01((p1 + offsetPx - minProj) / range);
        float t2 = clamp01((p3 + offsetPx - minProj) / range);
        float t3 = clamp01((p2 + offsetPx - minProj) / range);
        out[0] = mixArgb(startArgb, endArgb, t0);
        out[1] = mixArgb(startArgb, endArgb, t1);
        out[2] = mixArgb(startArgb, endArgb, t2);
        out[3] = mixArgb(startArgb, endArgb, t3);
    }
    static int linearGradientColorAt(double x, double y, double width, double height, int startArgb, int endArgb, float angleDeg, float offsetPx) {
        float angle = (float) Math.toRadians(angleDeg);
        float dirX = (float) Math.cos(angle);
        float dirY = (float) Math.sin(angle);
        float w = (float) width;
        float h = (float) height;
        float p0 = 0.0f;
        float p1 = w * dirX;
        float p2 = h * dirY;
        float p3 = w * dirX + h * dirY;
        float minProj = Math.min(Math.min(p0, p1), Math.min(p2, p3));
        float maxProj = Math.max(Math.max(p0, p1), Math.max(p2, p3));
        float range = Math.max(0.0001f, maxProj - minProj);
        float p = (float) x * dirX + (float) y * dirY;
        return mixArgb(startArgb, endArgb, clamp01((p + offsetPx - minProj) / range));
    }
    public static float clamp01(float v) {
        if (v < 0.0f) return 0.0f;
        if (v > 1.0f) return 1.0f;
        return v;
    }
    public static float packLiquidGlassPayload(float distortPx, float squirclePower, float blurAlpha) {
        float distort = Math.max(0.0f, Math.min(0.35f, distortPx));
        float smoothness = squirclePower < 1.5f ? 2.0f : Math.max(2.0f, Math.min(15.0f, Math.round(squirclePower)));
        float blurBucket = Math.round(clamp01(blurAlpha) * 100.0f);
        return smoothness * 10000.0f + blurBucket + distort;
    }
    public static float normalizeDegrees(float angleDeg) {
        float out = angleDeg % 360.0f;
        return out < 0.0f ? out + 360.0f : out;
    }
    public static int mixArgb(int start, int end, float t) {
        int sa = (start >>> 24) & 0xFF;
        int sr = (start >>> 16) & 0xFF;
        int sg = (start >>> 8) & 0xFF;
        int sb = start & 0xFF;
        int ea = (end >>> 24) & 0xFF;
        int er = (end >>> 16) & 0xFF;
        int eg = (end >>> 8) & 0xFF;
        int eb = end & 0xFF;
        int a = (int) (sa + (ea - sa) * t + 0.5f);
        int r = (int) (sr + (er - sr) * t + 0.5f);
        int g = (int) (sg + (eg - sg) * t + 0.5f);
        int b = (int) (sb + (eb - sb) * t + 0.5f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    public static void roundedRectAnchor(double[] out, int offset, double x, double y, double width, double height, double radius, double targetX, double targetY) {
        double cx = x + width * 0.5;
        double cy = y + height * 0.5;
        double halfW = Math.abs(width) * 0.5;
        double halfH = Math.abs(height) * 0.5;
        if (halfW <= 0.0 || halfH <= 0.0) { out[offset] = cx; out[offset + 1] = cy; return; }
        double dx = targetX - cx;
        double dy = targetY - cy;
        if (Math.abs(dx) <= 0.0001 && Math.abs(dy) <= 0.0001) { out[offset] = cx; out[offset + 1] = cy; return; }
        double r = Math.max(0.0, Math.min(Math.min(halfW, halfH), radius));
        double innerW = halfW - r;
        double innerH = halfH - r;
        double bestT = Double.POSITIVE_INFINITY;
        if (Math.abs(dx) > 0.0001) {
            double tx = (dx > 0.0 ? halfW : -halfW) / dx;
            double yy = dy * tx;
            if (tx > 0.0 && Math.abs(yy) <= innerH + 0.0001) bestT = Math.min(bestT, tx);
        }
        if (Math.abs(dy) > 0.0001) {
            double ty = (dy > 0.0 ? halfH : -halfH) / dy;
            double xx = dx * ty;
            if (ty > 0.0 && Math.abs(xx) <= innerW + 0.0001) bestT = Math.min(bestT, ty);
        }
        if (r > 0.0) {
            double circleX = dx >= 0.0 ? innerW : -innerW;
            double circleY = dy >= 0.0 ? innerH : -innerH;
            double a = dx * dx + dy * dy;
            double b = -2.0 * (dx * circleX + dy * circleY);
            double c = circleX * circleX + circleY * circleY - r * r;
            double discriminant = b * b - 4.0 * a * c;
            if (a > 0.0001 && discriminant >= 0.0) {
                double sqrt = Math.sqrt(discriminant);
                double t0 = (-b - sqrt) / (2.0 * a);
                double t1 = (-b + sqrt) / (2.0 * a);
                if (t0 > 0.0) bestT = Math.min(bestT, t0);
                if (t1 > 0.0) bestT = Math.min(bestT, t1);
            }
        }
        if (!Double.isFinite(bestT)) {
            double tx = Math.abs(dx) > 0.0001 ? halfW / Math.abs(dx) : Double.POSITIVE_INFINITY;
            double ty = Math.abs(dy) > 0.0001 ? halfH / Math.abs(dy) : Double.POSITIVE_INFINITY;
            bestT = Math.min(tx, ty);
        }
        out[offset] = cx + dx * bestT;
        out[offset + 1] = cy + dy * bestT;
    }
    public static int buildBezier(double[] out, double x1, double y1, double cx1, double cy1, double cx2, double cy2, double x2, double y2, int segments) {
        int count = Math.max(2, Math.min(32, segments + 1));
        for (int i = 0; i < count; i++) {
            double t = (double) i / (count - 1);
            double u = 1.0 - t;
            double x = u * u * u * x1 + 3.0 * u * u * t * cx1 + 3.0 * u * t * t * cx2 + t * t * t * x2;
            double y = u * u * u * y1 + 3.0 * u * u * t * cy1 + 3.0 * u * t * t * cy2 + t * t * t * y2;
            out[i * 2] = x;
            out[i * 2 + 1] = y;
        }
        return count;
    }
}
