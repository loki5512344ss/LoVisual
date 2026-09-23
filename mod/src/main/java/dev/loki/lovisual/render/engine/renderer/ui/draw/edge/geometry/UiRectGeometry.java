/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */
package dev.loki.lovisual.render.engine.renderer.ui.draw.edge;

public final class UiRectGeometry {
    private UiRectGeometry() {}
    public static float clampRoundedRadius(float radius, double width, double height) {
        float maxRadius = maxRoundedRadius(width, height);
        if (radius < 0.0f) return 0.0f;
        return Math.min(radius, maxRadius);
    }
    public static float clampChamfer(double chamfer, double width, double height) {
        float max = maxRoundedRadius(width, height);
        if (chamfer < 0.0) return 0.0f;
        return (float) Math.min(chamfer, max);
    }
    public static float clampChamferAxis(double chamfer, double length) {
        double max = Math.abs(length);
        if (chamfer < 0.0 || max <= 0.0) return 0.0f;
        return (float) Math.min(chamfer, max);
    }
    static float maxRoundedRadius(double width, double height) {
        double w = Math.abs(width);
        double h = Math.abs(height);
        if (w <= 0.0 || h <= 0.0) return 0.0f;
        return (float) (Math.min(w, h) * 0.5);
    }
    public static void normalizeCornerRadii(double width, double height, float radiusTL, float radiusTR, float radiusBR, float radiusBL, float[] out) {
        if (out == null || out.length < 4) throw new IllegalArgumentException("Output array must have length >= 4");
        float w = (float) Math.abs(width);
        float h = (float) Math.abs(height);
        if (w <= 0.0f || h <= 0.0f) { out[0]=0.0f; out[1]=0.0f; out[2]=0.0f; out[3]=0.0f; return; }
        float maxRadius = Math.min(w, h) * 0.5f;
        float tl = Math.max(0.0f, Math.min(radiusTL, maxRadius));
        float tr = Math.max(0.0f, Math.min(radiusTR, maxRadius));
        float br = Math.max(0.0f, Math.min(radiusBR, maxRadius));
        float bl = Math.max(0.0f, Math.min(radiusBL, maxRadius));
        float scale = 1.0f;
        float top = tl + tr;
        if (top > w && top > 0.0f) scale = Math.min(scale, w / top);
        float bottom = bl + br;
        if (bottom > w && bottom > 0.0f) scale = Math.min(scale, w / bottom);
        float left = tl + bl;
        if (left > h && left > 0.0f) scale = Math.min(scale, h / left);
        float right = tr + br;
        if (right > h && right > 0.0f) scale = Math.min(scale, h / right);
        if (scale < 1.0f) { tl *= scale; tr *= scale; br *= scale; bl *= scale; }
        out[0]=tl; out[1]=tr; out[2]=br; out[3]=bl;
    }
    public static void normalizeChamfers(double width, double height, float chamferTL, float chamferTR, float chamferBR, float chamferBL, float[] out) {
        if (out == null || out.length < 4) throw new IllegalArgumentException("Output array must have length >= 4");
        float w = (float) Math.abs(width);
        float h = (float) Math.abs(height);
        if (w <= 0.0f || h <= 0.0f) { out[0]=0.0f; out[1]=0.0f; out[2]=0.0f; out[3]=0.0f; return; }
        float max = Math.min(w, h);
        float tl = Math.max(0.0f, Math.min(chamferTL, max));
        float tr = Math.max(0.0f, Math.min(chamferTR, max));
        float br = Math.max(0.0f, Math.min(chamferBR, max));
        float bl = Math.max(0.0f, Math.min(chamferBL, max));
        float scale = 1.0f;
        float top = tl + tr;
        if (top > w && top > 0.0f) scale = Math.min(scale, w / top);
        float bottom = bl + br;
        if (bottom > w && bottom > 0.0f) scale = Math.min(scale, w / bottom);
        float left = tl + bl;
        if (left > h && left > 0.0f) scale = Math.min(scale, h / left);
        float right = tr + br;
        if (right > h && right > 0.0f) scale = Math.min(scale, h / right);
        if (scale < 1.0f) { tl *= scale; tr *= scale; br *= scale; bl *= scale; }
        out[0]=tl; out[1]=tr; out[2]=br; out[3]=bl;
    }
    static int buildChamferedRect(double[] out, double x, double y, double width, double height, double chamfer) {
        double c = Math.max(0.0, Math.min(Math.min(Math.abs(width), Math.abs(height)) * 0.5, chamfer));
        double x2 = x + width;
        double y2 = y + height;
        out[0]=x+c; out[1]=y; out[2]=x2-c; out[3]=y; out[4]=x2; out[5]=y+c; out[6]=x2; out[7]=y2-c;
        out[8]=x2-c; out[9]=y2; out[10]=x+c; out[11]=y2; out[12]=x; out[13]=y2-c; out[14]=x; out[15]=y+c;
        return 8;
    }
    public static int buildNotchedRect(double[] out, double x, double y, double width, double height, double notchWidth, double notchDepth) {
        double nw = Math.max(0.0, Math.min(Math.abs(width) * 0.5, notchWidth));
        double nd = Math.max(0.0, Math.min(Math.abs(height) * 0.5, notchDepth));
        double x2 = x + width;
        double y2 = y + height;
        double cx = x + width * 0.5;
        out[0]=x; out[1]=y; out[2]=cx-nw; out[3]=y; out[4]=cx-nw; out[5]=y+nd; out[6]=cx+nw; out[7]=y+nd;
        out[8]=cx+nw; out[9]=y; out[10]=x2; out[11]=y; out[12]=x2; out[13]=y2; out[14]=x; out[15]=y2;
        return 8;
    }
}
