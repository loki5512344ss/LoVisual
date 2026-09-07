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
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiPaint;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiShape;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

import java.util.function.Consumer;

import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.appendColoredQuad;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.clampRoundedRadius;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.computeLinearGradientColors;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.normalizeCornerRadii;

/** Filled rect family: solid quads, rounded rects, per-corner radii and gradients. */
public final class Rects2D {
    private Rects2D() {
    }

    public static void quadBatch(Renderer2D r, Consumer<Renderer2D.QuadBatch> consumer) {
        if (r.isTextured()) {
            throw new IllegalStateException("Quad batching is supported only on Renderer2D.COLOR.");
        }
        if (consumer == null) return;

        DrawBatch batch = Batch2D.open(r, UiBatchType.COLORED);
        if (batch == null) return;
        try {
            consumer.accept(new Renderer2D.QuadBatch(batch.mesh));
        } finally {
            Batch2D.close();
        }
    }

    public static void quadCorners(Renderer2D r, double x, double y, double width, double height,
                                   int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        r.shape(UiShape.rect(x, y, width, height), UiPaint.corners(cTopLeft, cTopRight, cBottomRight, cBottomLeft));
        DrawBatch batch = Batch2D.open(r, UiBatchType.COLORED);
        if (batch == null) return;
        appendColoredQuad(batch.mesh, x, y, width, height, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
        Batch2D.close();
    }

    public static void roundedRectMaskedQuad(Renderer2D r, double x, double y, double w, double h,
                                             double maskX, double maskY, double maskW, double maskH,
                                             float radius, float softness,
                                             int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        r.shape(UiShape.roundedRect(maskX, maskY, maskW, maskH, radius),
                UiPaint.corners(cTopLeft, cTopRight, cBottomRight, cBottomLeft));
        DrawBatch batch = Batch2D.open(r, UiBatchType.ROUNDED);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        mesh.ensureQuadCapacity();
        float clampedRadius = clampRoundedRadius(radius, maskW, maskH);

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

        int i1 = mesh.vec2(x, y).local2(x, y).color(tlR, tlG, tlB, tlA).vec4(maskX, maskY, maskW, maskH).vec4(clampedRadius, 0.0f, 0f, 0f).next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(blR, blG, blB, blA).vec4(maskX, maskY, maskW, maskH).vec4(clampedRadius, 0.0f, 0f, 0f).next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(brR, brG, brB, brA).vec4(maskX, maskY, maskW, maskH).vec4(clampedRadius, 0.0f, 0f, 0f).next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(trR, trG, trB, trA).vec4(maskX, maskY, maskW, maskH).vec4(clampedRadius, 0.0f, 0f, 0f).next();
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    public static void roundedRectCornersQuad(Renderer2D r, double x, double y, double w, double h,
                                              float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                              float softness,
                                              int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        r.shape(UiShape.roundedRect(x, y, w, h, radiusTL, radiusTR, radiusBR, radiusBL),
                UiPaint.corners(cTopLeft, cTopRight, cBottomRight, cBottomLeft));
        DrawBatch batch = Batch2D.open(r, UiBatchType.ROUNDED_CORNERS);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        mesh.ensureQuadCapacity();
        float[] radii = Batch2D.CORNER_RADII;
        normalizeCornerRadii(w, h, radiusTL, radiusTR, radiusBR, radiusBL, radii);

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

        int i1 = mesh.vec2(x, y).local2(x, y).color(tlR, tlG, tlB, tlA).vec4(x, y, w, h)
                .vec4(radii[0], radii[1], radii[2], radii[3]).vec4(0.0f, 0f, 0f, 0f).next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(blR, blG, blB, blA).vec4(x, y, w, h)
                .vec4(radii[0], radii[1], radii[2], radii[3]).vec4(0.0f, 0f, 0f, 0f).next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(brR, brG, brB, brA).vec4(x, y, w, h)
                .vec4(radii[0], radii[1], radii[2], radii[3]).vec4(0.0f, 0f, 0f, 0f).next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(trR, trG, trB, trA).vec4(x, y, w, h)
                .vec4(radii[0], radii[1], radii[2], radii[3]).vec4(0.0f, 0f, 0f, 0f).next();
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    public static void roundedRectGradientQuad(Renderer2D r, double x, double y, double w, double h,
                                               float radius, float softness,
                                               int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        roundedRectMaskedQuad(r, x, y, w, h, x, y, w, h, radius, 0.0f,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public static void roundedRectGradient(Renderer2D r, double x, double y, double w, double h,
                                           float radius, float softness,
                                           int startArgb, int endArgb, float angleDeg, float offsetPx) {
        int[] g = Batch2D.GRADIENT;
        computeLinearGradientColors((float) w, (float) h, startArgb, endArgb, angleDeg, offsetPx, g);
        roundedRectGradientQuad(r, x, y, w, h, radius, 0.0f, g[0], g[1], g[2], g[3]);
    }
}
