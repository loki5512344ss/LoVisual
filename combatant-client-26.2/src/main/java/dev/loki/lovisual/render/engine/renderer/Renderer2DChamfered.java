/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer;

import dev.loki.lovisual.render.engine.renderer.ui.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiPaint;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiShape;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiStroke;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.clampChamferAxis;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.normalizeChamfers;

final class Renderer2DChamfered {
    private Renderer2DChamfered() {
    }

    static void chamferedRectQuadAxes(Renderer2D r, double x, double y, double w, double h,
                                      double chamferTLX, double chamferTLY,
                                      double chamferTRX, double chamferTRY,
                                      double chamferBRX, double chamferBRY,
                                      double chamferBLX, double chamferBLY,
                                      int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        r.shape(UiShape.chamferedRectAxes(x, y, w, h, chamferTLX, chamferTLY, chamferTRX, chamferTRY, chamferBRX, chamferBRY, chamferBLX, chamferBLY),
                UiPaint.corners(cTopLeft, cTopRight, cBottomRight, cBottomLeft));
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.CHAMFERED, null, null);
        if (batch == null) {
            r.endAutoBatch(auto);
            return;
        }
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = r.alpha;

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

        r.endAutoBatch(auto);
    }

    static void chamferedRectStrokeQuad(Renderer2D r, double x, double y, double w, double h,
                                        double chamferTL, double chamferTR, double chamferBR, double chamferBL,
                                        double thickness,
                                        int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        r.shapeStroke(UiShape.chamferedRect(x, y, w, h, chamferTL, chamferTR, chamferBR, chamferBL),
                UiPaint.corners(cTopLeft, cTopRight, cBottomRight, cBottomLeft), UiStroke.of(thickness));
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.CHAMFERED_STROKE, null, null);
        if (batch == null) {
            r.endAutoBatch(auto);
            return;
        }
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = r.alpha;

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
        float[] cornerRadiiTmp = r.cornerRadiiTmp;
        normalizeChamfers(w, h,
                (float) chamferTL,
                (float) chamferTR,
                (float) chamferBR,
                (float) chamferBL,
                cornerRadiiTmp);
        float stroke = (float) Math.max(0.0, thickness);

        int i1 = mesh.vec2(x, y).local2(x, y).color(tlR, tlG, tlB, tlA).vec4(x, y, w, h).vec4(cornerRadiiTmp[0], cornerRadiiTmp[1], cornerRadiiTmp[2], cornerRadiiTmp[3]).vec4(stroke, 0f, 0f, 0f).next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(blR, blG, blB, blA).vec4(x, y, w, h).vec4(cornerRadiiTmp[0], cornerRadiiTmp[1], cornerRadiiTmp[2], cornerRadiiTmp[3]).vec4(stroke, 0f, 0f, 0f).next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(brR, brG, brB, brA).vec4(x, y, w, h).vec4(cornerRadiiTmp[0], cornerRadiiTmp[1], cornerRadiiTmp[2], cornerRadiiTmp[3]).vec4(stroke, 0f, 0f, 0f).next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(trR, trG, trB, trA).vec4(x, y, w, h).vec4(cornerRadiiTmp[0], cornerRadiiTmp[1], cornerRadiiTmp[2], cornerRadiiTmp[3]).vec4(stroke, 0f, 0f, 0f).next();
        mesh.quad(i1, i2, i3, i4);

        r.endAutoBatch(auto);
    }

    static void beveledRect(Renderer2D r, double x, double y, double width, double height,
                            double bevel, int fillArgb, int highlightArgb, int shadowArgb) {
        r.chamferedRect(x, y, width, height, bevel, fillArgb);
        double t = Math.max(1.0, Math.min(Math.min(Math.abs(width), Math.abs(height)) * 0.25, bevel));
        double[] connectorTmp = r.connectorTmp;
        connectorTmp[0] = x + bevel;
        connectorTmp[1] = y;
        connectorTmp[2] = x + width - bevel;
        connectorTmp[3] = y;
        connectorTmp[4] = x + width;
        connectorTmp[5] = y + bevel;
        Renderer2DPolygons.polyline(r, connectorTmp, 3, t, false, highlightArgb, false);
        connectorTmp[0] = x + width;
        connectorTmp[1] = y + height - bevel;
        connectorTmp[2] = x + width - bevel;
        connectorTmp[3] = y + height;
        connectorTmp[4] = x + bevel;
        connectorTmp[5] = y + height;
        connectorTmp[6] = x;
        connectorTmp[7] = y + height - bevel;
        Renderer2DPolygons.polyline(r, connectorTmp, 4, t, false, shadowArgb, false);
    }

    static void beveledRectGradient(Renderer2D r, double x, double y, double width, double height,
                                    double bevel, int startArgb, int endArgb,
                                    int highlightArgb, int shadowArgb, float angleDeg, float offsetPx) {
        r.chamferedRectGradient(x, y, width, height, bevel, startArgb, endArgb, angleDeg, offsetPx);
        double t = Math.max(1.0, Math.min(Math.min(Math.abs(width), Math.abs(height)) * 0.25, bevel));
        double[] connectorTmp = r.connectorTmp;
        connectorTmp[0] = x + bevel;
        connectorTmp[1] = y;
        connectorTmp[2] = x + width - bevel;
        connectorTmp[3] = y;
        connectorTmp[4] = x + width;
        connectorTmp[5] = y + bevel;
        Renderer2DPolygons.polyline(r, connectorTmp, 3, t, false, highlightArgb, false);
        connectorTmp[0] = x + width;
        connectorTmp[1] = y + height - bevel;
        connectorTmp[2] = x + width - bevel;
        connectorTmp[3] = y + height;
        connectorTmp[4] = x + bevel;
        connectorTmp[5] = y + height;
        connectorTmp[6] = x;
        connectorTmp[7] = y + height - bevel;
        Renderer2DPolygons.polyline(r, connectorTmp, 4, t, false, shadowArgb, false);
    }
}