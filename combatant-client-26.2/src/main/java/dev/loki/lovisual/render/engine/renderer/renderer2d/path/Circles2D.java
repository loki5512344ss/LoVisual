/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.renderer2d.path;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.renderer2d.Batch2D;
import dev.loki.lovisual.render.engine.renderer.ui.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiPaint;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiShape;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiStroke;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.util.Mth;

import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.normalizeDegrees;

/** Circles, rings, arcs and the animated Orbiz ring effect. */
public final class Circles2D {
    private Circles2D() {
    }

    public static void circle(Renderer2D r, double cx, double cy, double radius, float softness, int argb) {
        r.shape(UiShape.circle(cx, cy, radius), UiPaint.solid(argb));
        DrawBatch batch = Batch2D.open(r, UiBatchType.CIRCLE);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        mesh.ensureQuadCapacity();

        int[] c = Batch2D.rgba(argb);
        double outerRadius = radius + Math.max(0.0f, softness);
        double x = cx - outerRadius;
        double y = cy - outerRadius;
        double size = outerRadius * 2.0;

        int i1 = circleVertex(mesh, x, y, c, x, y, size, (float) radius, softness, 0f);
        int i2 = circleVertex(mesh, x, y + size, c, x, y, size, (float) radius, softness, 0f);
        int i3 = circleVertex(mesh, x + size, y + size, c, x, y, size, (float) radius, softness, 0f);
        int i4 = circleVertex(mesh, x + size, y, c, x, y, size, (float) radius, softness, 0f);
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    public static void circleStroke(Renderer2D r, double cx, double cy, double radius,
                                    double thickness, float softness, int argb) {
        r.shapeStroke(UiShape.circle(cx, cy, radius), UiPaint.solid(argb), UiStroke.of(thickness));
        DrawBatch batch = Batch2D.open(r, UiBatchType.CIRCLE);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        mesh.ensureQuadCapacity();

        int[] c = Batch2D.rgba(argb);
        float stroke = (float) Math.max(0.0, thickness);
        double outerRadius = radius + stroke * 0.5 + softness;
        double x = cx - outerRadius;
        double y = cy - outerRadius;
        double size = outerRadius * 2.0;

        int i1 = circleVertex(mesh, x, y, c, x, y, size, (float) radius, softness, stroke);
        int i2 = circleVertex(mesh, x, y + size, c, x, y, size, (float) radius, softness, stroke);
        int i3 = circleVertex(mesh, x + size, y + size, c, x, y, size, (float) radius, softness, stroke);
        int i4 = circleVertex(mesh, x + size, y, c, x, y, size, (float) radius, softness, stroke);
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    public static void arcStrokeQuad(Renderer2D r, double cx, double cy, double radius, double thickness,
                                     float startAngleDeg, float endAngleDeg, float softness, boolean caps,
                                     int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        float start = normalizeDegrees(startAngleDeg);
        float end = normalizeDegrees(endAngleDeg);
        float sweep = end - start;
        if (sweep <= 0.0f) {
            sweep += 360.0f;
        }
        if (sweep <= 0.001f) {
            return;
        }

        r.shapeStroke(UiShape.arc(cx, cy, radius, start, end),
                UiPaint.corners(cTopLeft, cTopRight, cBottomRight, cBottomLeft), UiStroke.of(thickness));
        DrawBatch batch = Batch2D.open(r, UiBatchType.ARC);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        mesh.ensureQuadCapacity();

        float stroke = (float) Math.max(0.0, thickness);
        double outerRadius = radius + stroke * 0.5 + softness;
        double x = cx - outerRadius;
        double y = cy - outerRadius;
        double size = outerRadius * 2.0;

        int i1 = arcVertex(mesh, x, y, cTopLeft, x, y, size, (float) radius, softness, stroke, start, end, caps);
        int i2 = arcVertex(mesh, x, y + size, cBottomLeft, x, y, size, (float) radius, softness, stroke, start, end, caps);
        int i3 = arcVertex(mesh, x + size, y + size, cBottomRight, x, y, size, (float) radius, softness, stroke, start, end, caps);
        int i4 = arcVertex(mesh, x + size, y, cTopRight, x, y, size, (float) radius, softness, stroke, start, end, caps);
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    public static void orbizRing(Renderer2D r, double cx, double cy,
                                 double radius, double thickness,
                                 float startAngleDeg, float sweepAngleDeg,
                                 float softness, float glowRadius, float glowStrength,
                                 int primaryArgb, int secondaryArgb,
                                 int colorMode, float angularOffset, float angularCycles,
                                 float trackAlpha, float bodyAlpha, float headBoost,
                                 float trackGlow, boolean caps) {
        if (r.isTextured()) {
            throw new IllegalStateException("Orbiz ring drawing is supported only on Renderer2D.COLOR.");
        }
        if (radius <= 0.0 || thickness <= 0.0) return;
        if (((primaryArgb >>> 24) & 0xFF) <= 0 && ((secondaryArgb >>> 24) & 0xFF) <= 0) return;

        DrawBatch batch = Batch2D.open(r, UiBatchType.ORBIZ_RING);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        mesh.ensureQuadCapacity();

        int[] primary = Batch2D.rgba(primaryArgb);
        float[] secondary = Batch2D.rgbaNorm(secondaryArgb);

        float stroke = (float) Math.max(0.0, thickness);
        float ringRadius = (float) Math.max(0.0, radius);
        float soft = Math.max(0.0f, softness);
        float glow = Math.max(0.0f, glowRadius);
        float expand = stroke * 0.5f + soft + glow + 1.0f;
        double outerRadius = radius + expand;
        double x = cx - outerRadius;
        double y = cy - outerRadius;
        double size = outerRadius * 2.0;
        double ringX = cx - radius;
        double ringY = cy - radius;
        double ringSize = radius * 2.0;

        float start = normalizeDegrees(startAngleDeg);
        float sweep = Mth.clamp(sweepAngleDeg, 0.0f, 360.0f);
        float offset = angularOffset - (float) Math.floor(angularOffset);
        float cycles = Math.max(0.001f, angularCycles);
        float mode = Math.max(0, colorMode);
        float track = Mth.clamp(trackAlpha, 0.0f, 1.0f);
        float body = Mth.clamp(bodyAlpha, 0.0f, 1.0f);
        float glowPower = Math.max(0.0f, glowStrength);
        float head = Math.max(0.0f, headBoost);
        float trackGlowAmount = Mth.clamp(trackGlow, 0.0f, 1.0f);

        int i1 = orbizVertex(mesh, x, y, primary, secondary, ringX, ringY, ringSize, ringRadius, soft, stroke,
                start, sweep, glow, glowPower, mode, offset, cycles, caps, track, body, head, trackGlowAmount);
        int i2 = orbizVertex(mesh, x, y + size, primary, secondary, ringX, ringY, ringSize, ringRadius, soft, stroke,
                start, sweep, glow, glowPower, mode, offset, cycles, caps, track, body, head, trackGlowAmount);
        int i3 = orbizVertex(mesh, x + size, y + size, primary, secondary, ringX, ringY, ringSize, ringRadius, soft, stroke,
                start, sweep, glow, glowPower, mode, offset, cycles, caps, track, body, head, trackGlowAmount);
        int i4 = orbizVertex(mesh, x + size, y, primary, secondary, ringX, ringY, ringSize, ringRadius, soft, stroke,
                start, sweep, glow, glowPower, mode, offset, cycles, caps, track, body, head, trackGlowAmount);
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    private static int circleVertex(MeshBuilder mesh, double px, double py, int[] c,
                                    double rx, double ry, double size,
                                    float radius, float softness, float stroke) {
        return mesh.vec2(px, py).local2(px, py).color(c[0], c[1], c[2], c[3])
                .vec4(rx, ry, size, size)
                .vec4(radius, softness, stroke, 0f)
                .next();
    }

    private static int arcVertex(MeshBuilder mesh, double px, double py, int argb,
                                 double rx, double ry, double size,
                                 float radius, float softness, float stroke,
                                 float start, float end, boolean caps) {
        int[] c = Batch2D.rgba(argb);
        return mesh.vec2(px, py).local2(px, py).color(c[0], c[1], c[2], c[3])
                .vec4(rx, ry, size, size)
                .vec4(radius, softness, stroke, start)
                .vec4(end, caps ? 1f : 0f, 0f, 0f)
                .next();
    }

    private static int orbizVertex(MeshBuilder mesh, double px, double py,
                                   int[] primary, float[] secondary,
                                   double ringX, double ringY, double ringSize, float ringRadius,
                                   float soft, float stroke, float start,
                                   float sweep, float glow, float glowPower, float mode,
                                   float offset, float cycles, boolean caps,
                                   float track, float body, float head, float trackGlowAmount) {
        return mesh.vec2(px, py).local2(px, py).color(primary[0], primary[1], primary[2], primary[3])
                .vec4(ringX, ringY, ringSize, ringSize)
                .vec4(ringRadius, soft, stroke, start)
                .vec4(sweep, glow, glowPower, mode)
                .vec4(secondary[0], secondary[1], secondary[2], secondary[3])
                .vec4(offset, cycles, 0.0f, caps ? 1.0f : 0.0f)
                .vec4(track, body, head, trackGlowAmount)
                .next();
    }
}
