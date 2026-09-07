/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.renderer2d;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.renderer2d.path.Polygons2D;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiPaint;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiShape;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiStroke;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.clampChamferAxis;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.computeLinearGradientColors;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.normalizeChamfers;

/** Chamfered (cut-corner) rects and beveled 3D-style plates. */
public final class Chamfered2D {
    private Chamfered2D() {
    }

    public static void chamferedRectQuadAxes(Renderer2D r, double x, double y, double w, double h,
                                             double chamferTLX, double chamferTLY,
                                             double chamferTRX, double chamferTRY,
                                             double chamferBRX, double chamferBRY,
                                             double chamferBLX, double chamferBLY,
                                             int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        r.shape(UiShape.chamferedRectAxes(x, y, w, h, chamferTLX, chamferTLY, chamferTRX, chamferTRY,
                        chamferBRX, chamferBRY, chamferBLX, chamferBLY),
                UiPaint.corners(cTopLeft, cTopRight, cBottomRight, cBottomLeft));
        DrawBatch batch = Batch2D.open(r, UiBatchType.CHAMFERED);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        mesh.ensureQuadCapacity();

        int tlA = (cTopLeft >>> 24) & 0xFF;
        int tlR = (cTopLeft >>> 16) & 0xFF;
        int tlG = (cTopLeft >>> 8) & 0xFF;
        int tlB = cTopLeft & 0xFF;
        int trA = (cTopRight >>> 24) & 0xFF;
        int trR = (cTopRight >>> 16) & 0xFF;
        int trG = (cTopRight >>> 8) & 0xFF;
        int trB = cTopRight & 0xFF;
        int brA = (cBottomRight >>> 24) & 0xFF;
        int brR = (cBottomRight >>> 16) & 0xFF;
        int brG = (cBottomRight >>> 8) & 0xFF;
        int brB = cBottomRight & 0xFF;
        int blA = (cBottomLeft >>> 24) & 0xFF;
        int blR = (cBottomLeft >>> 16) & 0xFF;
        int blG = (cBottomLeft >>> 8) & 0xFF;
        int blB = cBottomLeft & 0xFF;

        float tlX = clampChamferAxis(chamferTLX, w);
        float trX = clampChamferAxis(chamferTRX, w);
        float brX = clampChamferAxis(chamferBRX, w);
        float blX = clampChamferAxis(chamferBLX, w);
        float tlY = clampChamferAxis(chamferTLY, h);
        float trY = clampChamferAxis(chamferTRY, h);
        float brY = clampChamferAxis(chamferBRY, h);
        float blY = clampChamferAxis(chamferBLY, h);

        int i1 = mesh.vec2(x, y).local2(x, y).color(tlR, tlG, tlB, tlA).vec4(x, y, w, h).vec4(tlX, trX, brX, blX).vec4(tlY, trY, brY, blY).next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(blR, blG, blB, blA).vec4(x, y, w, h).vec4(tlX, trX, brX, blX).vec4(tlY, trY, brY, blY).next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(brR, brG, brB, brA).vec4(x, y, w, h).vec4(tlX, trX, brX, blX).vec4(tlY, trY, brY, blY).next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(trR, trG, trB, trA).vec4(x, y, w, h).vec4(tlX, trX, brX, blX).vec4(tlY, trY, brY, blY).next();
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    public static void chamferedRectQuad(Renderer2D r, double x, double y, double w, double h,
                                         double chamferTL, double chamferTR, double chamferBR, double chamferBL,
                                         int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        float[] c = Batch2D.CORNER_RADII;
        normalizeChamfers(w, h, (float) chamferTL, (float) chamferTR, (float) chamferBR, (float) chamferBL, c);
        chamferedRectQuadAxes(r, x, y, w, h,
                c[0], c[0], c[1], c[1], c[2], c[2], c[3], c[3],
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public static void chamferedRectGradient(Renderer2D r, double x, double y, double width, double height,
                                             double chamferTL, double chamferTR, double chamferBR, double chamferBL,
                                             int startArgb, int endArgb, float angleDeg, float offsetPx) {
        int[] g = Batch2D.GRADIENT;
        computeLinearGradientColors((float) width, (float) height, startArgb, endArgb, angleDeg, offsetPx, g);
        chamferedRectQuad(r, x, y, width, height, chamferTL, chamferTR, chamferBR, chamferBL,
                g[0], g[1], g[2], g[3]);
    }

    public static void chamferedRectStrokeQuad(Renderer2D r, double x, double y, double w, double h,
                                               double chamferTL, double chamferTR, double chamferBR, double chamferBL,
                                               double thickness,
                                               int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        r.shapeStroke(UiShape.chamferedRect(x, y, w, h, chamferTL, chamferTR, chamferBR, chamferBL),
                UiPaint.corners(cTopLeft, cTopRight, cBottomRight, cBottomLeft), UiStroke.of(thickness));
        DrawBatch batch = Batch2D.open(r, UiBatchType.CHAMFERED_STROKE);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        mesh.ensureQuadCapacity();

        int tlA = (cTopLeft >>> 24) & 0xFF;
        int tlR = (cTopLeft >>> 16) & 0xFF;
        int tlG = (cTopLeft >>> 8) & 0xFF;
        int tlB = cTopLeft & 0xFF;
        int trA = (cTopRight >>> 24) & 0xFF;
        int trR = (cTopRight >>> 16) & 0xFF;
        int trG = (cTopRight >>> 8) & 0xFF;
        int trB = cTopRight & 0xFF;
        int brA = (cBottomRight >>> 24) & 0xFF;
        int brR = (cBottomRight >>> 16) & 0xFF;
        int brG = (cBottomRight >>> 8) & 0xFF;
        int brB = cBottomRight & 0xFF;
        int blA = (cBottomLeft >>> 24) & 0xFF;
        int blR = (cBottomLeft >>> 16) & 0xFF;
        int blG = (cBottomLeft >>> 8) & 0xFF;
        int blB = cBottomLeft & 0xFF;
        float[] c = Batch2D.CORNER_RADII;
        normalizeChamfers(w, h, (float) chamferTL, (float) chamferTR, (float) chamferBR, (float) chamferBL, c);
        float stroke = (float) Math.max(0.0, thickness);

        int i1 = mesh.vec2(x, y).local2(x, y).color(tlR, tlG, tlB, tlA).vec4(x, y, w, h).vec4(c[0], c[1], c[2], c[3]).vec4(stroke, 0f, 0f, 0f).next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(blR, blG, blB, blA).vec4(x, y, w, h).vec4(c[0], c[1], c[2], c[3]).vec4(stroke, 0f, 0f, 0f).next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(brR, brG, brB, brA).vec4(x, y, w, h).vec4(c[0], c[1], c[2], c[3]).vec4(stroke, 0f, 0f, 0f).next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(trR, trG, trB, trA).vec4(x, y, w, h).vec4(c[0], c[1], c[2], c[3]).vec4(stroke, 0f, 0f, 0f).next();
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    public static void beveledRect(Renderer2D r, double x, double y, double width, double height,
                                   double bevel, int fillArgb, int highlightArgb, int shadowArgb) {
        chamferedRectQuad(r, x, y, width, height, bevel, bevel, bevel, bevel,
                fillArgb, fillArgb, fillArgb, fillArgb);
        bevelEdges(r, x, y, width, height, bevel, highlightArgb, shadowArgb);
    }

    public static void beveledRectGradient(Renderer2D r, double x, double y, double width, double height,
                                           double bevel, int startArgb, int endArgb,
                                           int highlightArgb, int shadowArgb, float angleDeg, float offsetPx) {
        chamferedRectGradient(r, x, y, width, height, bevel, bevel, bevel, bevel,
                startArgb, endArgb, angleDeg, offsetPx);
        bevelEdges(r, x, y, width, height, bevel, highlightArgb, shadowArgb);
    }

    private static void bevelEdges(Renderer2D r, double x, double y, double width, double height,
                                   double bevel, int highlightArgb, int shadowArgb) {
        double t = Math.max(1.0, Math.min(Math.min(Math.abs(width), Math.abs(height)) * 0.25, bevel));
        double[] c = Batch2D.CONNECTOR;
        c[0] = x + bevel;
        c[1] = y;
        c[2] = x + width - bevel;
        c[3] = y;
        c[4] = x + width;
        c[5] = y + bevel;
        Polygons2D.polyline(r, c, 3, t, false, highlightArgb, false);
        c[0] = x + width;
        c[1] = y + height - bevel;
        c[2] = x + width - bevel;
        c[3] = y + height;
        c[4] = x + bevel;
        c[5] = y + height;
        c[6] = x;
        c[7] = y + height - bevel;
        Polygons2D.polyline(r, c, 4, t, false, shadowArgb, false);
    }
}
