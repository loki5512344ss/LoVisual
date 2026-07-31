/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer;

import dev.loki.lovisual.render.engine.renderer.ui.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiPaint;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiShape;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

import java.util.function.Consumer;

import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.appendColoredQuad;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.clampRoundedRadius;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.normalizeCornerRadii;

final class Renderer2DRects {
    private Renderer2DRects() {
    }

    static void quadBatch(Renderer2D r, Consumer<Renderer2D.QuadBatch> consumer) {
        if (r.textured) {
            throw new IllegalStateException("Quad batching is supported only on Renderer2D.COLOR.");
        }
        if (consumer == null) return;

        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.COLORED, null, null);
        if (batch == null) {
            r.endAutoBatch(auto);
            return;
        }

        MeshBuilder mesh = batch.mesh;
        mesh.alpha = r.alpha;
        Renderer2D.QuadBatch quadBatch = new Renderer2D.QuadBatch(mesh);
        try {
            consumer.accept(quadBatch);
        } finally {
            r.endAutoBatch(auto);
        }
    }

    static void quadCorners(Renderer2D r, double x, double y, double width, double height,
                            int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        r.shape(UiShape.rect(x, y, width, height), UiPaint.corners(cTopLeft, cTopRight, cBottomRight, cBottomLeft));
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.COLORED, null, null);
        if (batch == null) {
            r.endAutoBatch(auto);
            return;
        }
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = r.alpha;

        appendColoredQuad(mesh, x, y, width, height, cTopLeft, cTopRight, cBottomRight, cBottomLeft);

        r.endAutoBatch(auto);
    }

    static void roundedRectMaskedQuad(Renderer2D r, double x, double y, double w, double h,
                                      double maskX, double maskY, double maskW, double maskH,
                                      float radius, float softness,
                                      int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        r.shape(UiShape.roundedRect(maskX, maskY, maskW, maskH, radius), UiPaint.corners(cTopLeft, cTopRight, cBottomRight, cBottomLeft));
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.ROUNDED, null, null);
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
        float clampedRadius = clampRoundedRadius(radius, maskW, maskH);

        int i1 = mesh.vec2(x, y).local2(x, y).color(tlR, tlG, tlB, tlA).vec4(maskX, maskY, maskW, maskH).vec4(clampedRadius, 0.0f, 0f, 0f).next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(blR, blG, blB, blA).vec4(maskX, maskY, maskW, maskH).vec4(clampedRadius, 0.0f, 0f, 0f).next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(brR, brG, brB, brA).vec4(maskX, maskY, maskW, maskH).vec4(clampedRadius, 0.0f, 0f, 0f).next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(trR, trG, trB, trA).vec4(maskX, maskY, maskW, maskH).vec4(clampedRadius, 0.0f, 0f, 0f).next();
        mesh.quad(i1, i2, i3, i4);

        r.endAutoBatch(auto);
    }

    static void roundedRectCornersQuad(Renderer2D r, double x, double y, double w, double h,
                                      float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                      float softness,
                                      int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        r.shape(UiShape.roundedRect(x, y, w, h, radiusTL, radiusTR, radiusBR, radiusBL), UiPaint.corners(cTopLeft, cTopRight, cBottomRight, cBottomLeft));
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.ROUNDED_CORNERS, null, null);
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
        normalizeCornerRadii(w, h, radiusTL, radiusTR, radiusBR, radiusBL, cornerRadiiTmp);

        int i1 = mesh.vec2(x, y).local2(x, y).color(tlR, tlG, tlB, tlA).vec4(x, y, w, h).vec4(cornerRadiiTmp[0], cornerRadiiTmp[1], cornerRadiiTmp[2], cornerRadiiTmp[3]).vec4(0.0f, 0f, 0f, 0f).next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(blR, blG, blB, blA).vec4(x, y, w, h).vec4(cornerRadiiTmp[0], cornerRadiiTmp[1], cornerRadiiTmp[2], cornerRadiiTmp[3]).vec4(0.0f, 0f, 0f, 0f).next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(brR, brG, brB, brA).vec4(x, y, w, h).vec4(cornerRadiiTmp[0], cornerRadiiTmp[1], cornerRadiiTmp[2], cornerRadiiTmp[3]).vec4(0.0f, 0f, 0f, 0f).next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(trR, trG, trB, trA).vec4(x, y, w, h).vec4(cornerRadiiTmp[0], cornerRadiiTmp[1], cornerRadiiTmp[2], cornerRadiiTmp[3]).vec4(0.0f, 0f, 0f, 0f).next();
        mesh.quad(i1, i2, i3, i4);

        r.endAutoBatch(auto);
    }
}