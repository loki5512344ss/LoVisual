package dev.loki.lovisual.render.engine.renderer.warp;

/**
 * Screen-space UI warp. Coordinates stay in the same 2D HUD space; only emitted vertices are remapped.
 *
 * <p>Instances are immutable value objects; build them via {@link RenderWarpFactory}
 * ({@code corners}/{@code tilt}/{@code angles}/{@code perspective}) and query them through the
 * {@code map*}/{@code *Corner} accessors used by the renderer.</p>
 */
public final class RenderWarp {
    public static final RenderWarp IDENTITY = new RenderWarp(
            false,
            0.0f, 0.0f, 1.0f, 1.0f,
            0.0f, 0.0f,
            0.0f, 0.0f,
            0.0f, 0.0f,
            0.0f, 0.0f,
            1.0f
    );

    private final boolean active;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final float tlX;
    private final float tlY;
    private final float trX;
    private final float trY;
    private final float brX;
    private final float brY;
    private final float blX;
    private final float blY;
    private final float strength;
    private final float dstTlX;
    private final float dstTlY;
    private final float dstTrX;
    private final float dstTrY;
    private final float dstBrX;
    private final float dstBrY;
    private final float dstBlX;
    private final float dstBlY;
    private final double h00;
    private final double h01;
    private final double h02;
    private final double h10;
    private final double h11;
    private final double h12;
    private final double h20;
    private final double h21;

    RenderWarp(boolean active,
               float x,
               float y,
               float width,
               float height,
               float tlX,
               float tlY,
               float trX,
               float trY,
               float brX,
               float brY,
               float blX,
               float blY,
               float strength) {
        this.active = active;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.tlX = tlX;
        this.tlY = tlY;
        this.trX = trX;
        this.trY = trY;
        this.brX = brX;
        this.brY = brY;
        this.blX = blX;
        this.blY = blY;
        this.strength = strength;
        this.dstTlX = x + tlX * strength;
        this.dstTlY = y + tlY * strength;
        this.dstTrX = x + width + trX * strength;
        this.dstTrY = y + trY * strength;
        this.dstBrX = x + width + brX * strength;
        this.dstBrY = y + height + brY * strength;
        this.dstBlX = x + blX * strength;
        this.dstBlY = y + height + blY * strength;

        double[] matrix = RenderWarpGeometry.computeUnitSquareHomography(
                dstTlX, dstTlY,
                dstTrX, dstTrY,
                dstBrX, dstBrY,
                dstBlX, dstBlY
        );
        this.h00 = matrix[0];
        this.h01 = matrix[1];
        this.h02 = matrix[2];
        this.h10 = matrix[3];
        this.h11 = matrix[4];
        this.h12 = matrix[5];
        this.h20 = matrix[6];
        this.h21 = matrix[7];
    }

    public boolean active() {
        return active;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public RenderWarp withStrength(float multiplier) {
        if (!active) return this;
        return RenderWarpFactory.corners(x, y, width, height, tlX, tlY, trX, trY, brX, brY, blX, blY, strength * multiplier);
    }

    public double mapX(double px, double py) {
        if (!active) return px;
        return map(px, py, true);
    }

    public double mapY(double px, double py) {
        if (!active) return py;
        return map(px, py, false);
    }

    public double interpolationInvW(double px, double py) {
        if (!active) return 1.0;
        double u = (px - x) / width;
        double v = (py - y) / height;
        double denom = h20 * u + h21 * v + 1.0;
        if (Math.abs(denom) <= 0.000001) return 1.0;
        return 1.0 / denom;
    }

    public void map(double px, double py, double[] out) {
        if (out == null || out.length < 2) return;
        if (!active) {
            out[0] = px;
            out[1] = py;
            return;
        }
        mapProjective(px, py, out);
    }

    public void originalCorner(int corner, double[] out) {
        if (out == null || out.length < 2) return;
        switch (corner) {
            case 0 -> {
                out[0] = x;
                out[1] = y;
            }
            case 1 -> {
                out[0] = x + width;
                out[1] = y;
            }
            case 2 -> {
                out[0] = x + width;
                out[1] = y + height;
            }
            case 3 -> {
                out[0] = x;
                out[1] = y + height;
            }
            default -> {
                out[0] = x;
                out[1] = y;
            }
        }
    }

    public void warpedCorner(int corner, double[] out) {
        if (out == null || out.length < 2) return;
        switch (corner) {
            case 0 -> {
                out[0] = dstTlX;
                out[1] = dstTlY;
            }
            case 1 -> {
                out[0] = dstTrX;
                out[1] = dstTrY;
            }
            case 2 -> {
                out[0] = dstBrX;
                out[1] = dstBrY;
            }
            case 3 -> {
                out[0] = dstBlX;
                out[1] = dstBlY;
            }
            default -> {
                out[0] = dstTlX;
                out[1] = dstTlY;
            }
        }
    }

    private double map(double px, double py, boolean xAxis) {
        double[] out = new double[2];
        mapProjective(px, py, out);
        return xAxis ? out[0] : out[1];
    }

    private void mapProjective(double px, double py, double[] out) {
        double u = (px - x) / width;
        double v = (py - y) / height;
        double denom = h20 * u + h21 * v + 1.0;
        if (Math.abs(denom) <= 0.000001) {
            out[0] = px;
            out[1] = py;
            return;
        }
        out[0] = (h00 * u + h01 * v + h02) / denom;
        out[1] = (h10 * u + h11 * v + h12) / denom;
    }
}
