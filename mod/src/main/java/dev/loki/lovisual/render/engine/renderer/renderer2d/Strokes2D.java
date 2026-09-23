/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.renderer2d;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.batches.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.renderer2d.path.Polygons2D;
import dev.loki.lovisual.render.engine.renderer.ui.draw.paint.UiPaint;
import dev.loki.lovisual.render.engine.renderer.ui.draw.shape.UiShape;
import dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiStroke;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.util.Mth;

import static dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiMeshGeometry.buildRoundedProgressRect;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiMeshGeometry.clampRoundedRadius;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiMeshGeometry.normalizeCornerRadii;

/** Stroked rounded rect family plus the clipped progress bar fill. */
public final class Strokes2D {
    private Strokes2D() {
    }

    public static void roundedRectStroke(Renderer2D r, double x, double y, double w, double h,
                                         float radius, float softness, float thickness, int argb) {
        r.shapeStroke(UiShape.roundedRect(x, y, w, h, radius), UiPaint.solid(argb), UiStroke.of(thickness));
        DrawBatch batch = Batch2D.open(r, UiBatchType.ROUNDED_STROKE);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        mesh.ensureQuadCapacity();

        int a = (argb >>> 24) & 0xFF;
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        float clampedRadius = clampRoundedRadius(radius, w, h);
        float stroke = Math.max(0.0f, thickness);

        int i1 = mesh.vec2(x, y).local2(x, y).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, stroke, 0f).next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, stroke, 0f).next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, stroke, 0f).next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, stroke, 0f).next();
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    public static void roundedRectStrokeAngularGradient(Renderer2D r, double x, double y, double w, double h,
                                                        float radius, float softness, float thickness,
                                                        int startArgb, int endArgb, float offset) {
        r.shapeStroke(UiShape.roundedRect(x, y, w, h, radius), UiPaint.solid(startArgb), UiStroke.of(thickness));
        DrawBatch batch = Batch2D.open(r, UiBatchType.ROUNDED_STROKE_ANGULAR);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        mesh.ensureQuadCapacity();

        int sa = (startArgb >>> 24) & 0xFF;
        int sr = (startArgb >>> 16) & 0xFF;
        int sg = (startArgb >>> 8) & 0xFF;
        int sb = startArgb & 0xFF;
        float[] end = Batch2D.rgbaNorm(endArgb);
        float clampedRadius = clampRoundedRadius(radius, w, h);
        float stroke = Math.max(0.0f, thickness);
        float phase = offset - (float) Math.floor(offset);

        int i1 = mesh.vec2(x, y).local2(x, y).color(sr, sg, sb, sa).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, stroke, phase).vec4(end[0], end[1], end[2], end[3]).next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(sr, sg, sb, sa).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, stroke, phase).vec4(end[0], end[1], end[2], end[3]).next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(sr, sg, sb, sa).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, stroke, phase).vec4(end[0], end[1], end[2], end[3]).next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(sr, sg, sb, sa).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, stroke, phase).vec4(end[0], end[1], end[2], end[3]).next();
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    public static void roundedRectStrokeCorners(Renderer2D r, double x, double y, double w, double h,
                                                float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                                float softness, float thickness, int argb) {
        r.shapeStroke(UiShape.roundedRect(x, y, w, h, radiusTL, radiusTR, radiusBR, radiusBL), UiPaint.solid(argb), UiStroke.of(thickness));
        DrawBatch batch = Batch2D.open(r, UiBatchType.ROUNDED_STROKE_CORNERS);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        mesh.ensureQuadCapacity();

        int a = (argb >>> 24) & 0xFF;
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        float[] radii = Batch2D.CORNER_RADII;
        normalizeCornerRadii(w, h, radiusTL, radiusTR, radiusBR, radiusBL, radii);
        float stroke = Math.max(0.0f, thickness);

        int i1 = mesh.vec2(x, y).local2(x, y).color(red, green, blue, a).vec4(x, y, w, h).vec4(radii[0], radii[1], radii[2], radii[3]).vec4(0.0f, stroke, 0f, 0f).next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(red, green, blue, a).vec4(x, y, w, h).vec4(radii[0], radii[1], radii[2], radii[3]).vec4(0.0f, stroke, 0f, 0f).next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(red, green, blue, a).vec4(x, y, w, h).vec4(radii[0], radii[1], radii[2], radii[3]).vec4(0.0f, stroke, 0f, 0f).next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(red, green, blue, a).vec4(x, y, w, h).vec4(radii[0], radii[1], radii[2], radii[3]).vec4(0.0f, stroke, 0f, 0f).next();
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    public static void roundedRectStrokeGradientQuad(Renderer2D r, double x, double y, double w, double h,
                                                     float radius, float softness, float thickness,
                                                     int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        r.shapeStroke(UiShape.roundedRect(x, y, w, h, radius),
                UiPaint.corners(cTopLeft, cTopRight, cBottomRight, cBottomLeft), UiStroke.of(thickness));
        DrawBatch batch = Batch2D.open(r, UiBatchType.ROUNDED_STROKE);
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
        float clampedRadius = clampRoundedRadius(radius, w, h);
        float stroke = Math.max(0.0f, thickness);

        int i1 = mesh.vec2(x, y).local2(x, y).color(tlR, tlG, tlB, tlA).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, stroke, 0f).next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(blR, blG, blB, blA).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, stroke, 0f).next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(brR, brG, brB, brA).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, stroke, 0f).next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(trR, trG, trB, trA).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, stroke, 0f).next();
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    /**
     * Draws a progress segment clipped by the full rounded bar outline.
     *
     * <p>Unlike drawing a rounded rectangle with the current fill width, this keeps the moving
     * progress edge straight and lets only the fixed outer bar bounds define rounded ends. That
     * prevents small health values from becoming a pill/capsule and prevents the fill from leaking
     * outside the left/right rounded shell when the progress edge crosses an outer corner.</p>
     */
    public static void roundedProgressRectGradient(Renderer2D r, double x, double y, double w, double h,
                                                   float radius, float softness,
                                                   float progress, boolean fromRight,
                                                   int startArgb, int endArgb,
                                                   float angleDeg, float offsetPx) {
        if (w <= 0.0 || h <= 0.0) return;
        float p = Mth.clamp(progress, 0.0f, 1.0f);
        if (p <= 0.0001f) return;
        if (p >= 0.9999f) {
            Rects2D.roundedRectGradient(r, x, y, w, h, radius, softness, startArgb, endArgb, angleDeg, offsetPx);
            return;
        }

        double fillW = Math.max(0.0, Math.min(w, w * p));
        double fillX = fromRight ? x + w - fillW : x;
        double clipX = fromRight ? fillX : x + fillW;
        int count = buildRoundedProgressRect(Batch2D.PROGRESS_SHAPE, Batch2D.RECT_SHAPE, x, y, w, h, radius, clipX, fromRight);
        if (count < 3) return;
        Polygons2D.polygonGradient(r, Batch2D.PROGRESS_SHAPE, count, fillX, y, fillW, h, startArgb, endArgb, angleDeg, offsetPx);
    }
}
