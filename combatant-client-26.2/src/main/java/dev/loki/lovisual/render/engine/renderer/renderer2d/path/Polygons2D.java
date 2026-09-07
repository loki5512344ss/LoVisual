/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.renderer2d.path;

import dev.loki.lovisual.render.engine.command.UiShapeCommand;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.renderer2d.Batch2D;
import dev.loki.lovisual.render.engine.renderer.ui.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.ui.UiRenderDispatcher;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiBoxPathBuilder;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiBoxShape;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiPaint;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiPaintKind;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiPathCap;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiPathJoin;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiShape;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiStroke;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.appendPolygon;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.appendPolygonGradient;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.appendPolyline;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.appendPolylineGradient;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.appendPolylineLinearGradient;

/** Filled polygons, polylines and flexible box shapes. */
public final class Polygons2D {
    private Polygons2D() {
    }

    public static void box(Renderer2D r, UiBoxShape box, UiPaint paint, UiStroke stroke, boolean fill) {
        if (box == null || paint == null) return;
        UiStroke safeStroke = stroke == null ? UiStroke.NONE : stroke;
        UiRenderDispatcher.record(new UiShapeCommand(UiShape.box(box), paint, safeStroke, fill));
        renderFlexibleBoxFallback(r, box, paint, safeStroke, fill);
    }

    private static void renderFlexibleBoxFallback(Renderer2D r, UiBoxShape box, UiPaint paint, UiStroke stroke, boolean fill) {
        if (r.isTextured() || box == null || paint == null) return;
        double[] shape = Batch2D.RECT_SHAPE;
        int count = UiBoxPathBuilder.write(box, shape, shape.length / 2);
        if (count < 3) return;

        if (fill) {
            if (paint.kind() == UiPaintKind.LINEAR_GRADIENT) {
                polygonGradient(r, shape, count, box.bounds().x(), box.bounds().y(),
                        box.bounds().width(), box.bounds().height(),
                        paint.topLeft(), paint.topRight(), paint.angleDeg(), paint.offsetPx());
            } else {
                polygon(r, shape, count, paint.solidColor());
            }
        } else if (stroke.enabled()) {
            boolean round = stroke.join() == UiPathJoin.ROUND || stroke.cap() == UiPathCap.ROUND;
            if (paint.kind() == UiPaintKind.LINEAR_GRADIENT) {
                polylineLinearGradient(r, shape, count, stroke.thickness(), true,
                        box.bounds().x(), box.bounds().y(), box.bounds().width(), box.bounds().height(),
                        paint.topLeft(), paint.topRight(), paint.angleDeg(), paint.offsetPx(), round);
            } else {
                polyline(r, shape, count, stroke.thickness(), true, paint.solidColor(), round);
            }
        }
    }

    public static void polygon(Renderer2D r, double[] points, int pointCount, int argb) {
        if (r.isTextured()) {
            throw new IllegalStateException("Polygon drawing is supported only on Renderer2D.COLOR.");
        }
        int safeCount = Math.min(pointCount, points != null ? points.length / 2 : 0);
        safeCount = Math.min(safeCount, Math.min(Batch2D.POLY_INDEX.length, Batch2D.POLY_VERTEX.length));
        if (points == null || safeCount < 3) return;

        r.path(UiShape.polyline(points, safeCount, true), UiPaint.solid(argb), UiStroke.NONE, true);
        DrawBatch batch = Batch2D.open(r, UiBatchType.COLORED);
        if (batch == null) return;
        appendPolygon(batch.mesh, points, safeCount, Batch2D.POLY_INDEX, Batch2D.POLY_VERTEX, argb);
        Batch2D.close();
    }

    public static void polygonGradient(Renderer2D r, double[] points, int pointCount,
                                       double x, double y, double width, double height,
                                       int startArgb, int endArgb, float angleDeg, float offsetPx) {
        if (r.isTextured()) {
            throw new IllegalStateException("Polygon drawing is supported only on Renderer2D.COLOR.");
        }
        if (points == null || pointCount < 3) return;

        r.path(UiShape.polyline(points, pointCount, true),
                UiPaint.linear(startArgb, endArgb, angleDeg, offsetPx), UiStroke.NONE, true);
        DrawBatch batch = Batch2D.open(r, UiBatchType.COLORED);
        if (batch == null) return;
        appendPolygonGradient(batch.mesh, points, pointCount, Batch2D.POLY_INDEX, Batch2D.POLY_VERTEX,
                x, y, width, height, startArgb, endArgb, angleDeg, offsetPx);
        Batch2D.close();
    }

    public static void polygonStroke(Renderer2D r, double[] points, int pointCount,
                                     double thickness, boolean closed, int argb) {
        polyline(r, points, pointCount, thickness, closed, argb, false);
    }

    public static void polyline(Renderer2D r, double[] points, int pointCount,
                                double thickness, boolean closed, int argb) {
        polyline(r, points, pointCount, thickness, closed, argb, false);
    }

    public static void polyline(Renderer2D r, double[] points, int pointCount,
                                double thickness, boolean closed, int argb, boolean roundCapsAndJoins) {
        if (r.isTextured()) {
            throw new IllegalStateException("Polyline drawing is supported only on Renderer2D.COLOR.");
        }
        if (points == null || pointCount < 2) return;
        double t = Math.max(0.0, thickness);
        if (t <= 0.0) return;

        UiStroke stroke = roundCapsAndJoins ? UiStroke.of(t).withRoundCapsAndJoins() : UiStroke.of(t);
        r.path(UiShape.polyline(points, pointCount, closed), UiPaint.solid(argb), stroke, false);
        DrawBatch batch = Batch2D.open(r, UiBatchType.COLORED);
        if (batch == null) return;
        appendPolyline(batch.mesh, points, pointCount, t, closed, argb, roundCapsAndJoins);
        Batch2D.close();
    }

    public static void polylineGradient(Renderer2D r, double[] points, int pointCount,
                                        double thickness, boolean closed,
                                        int startArgb, int endArgb, boolean roundCapsAndJoins) {
        if (r.isTextured()) {
            throw new IllegalStateException("Polyline drawing is supported only on Renderer2D.COLOR.");
        }
        if (points == null || pointCount < 2) return;
        double t = Math.max(0.0, thickness);
        if (t <= 0.0) return;

        UiStroke stroke = roundCapsAndJoins ? UiStroke.of(t).withRoundCapsAndJoins() : UiStroke.of(t);
        r.path(UiShape.polyline(points, pointCount, closed),
                UiPaint.corners(startArgb, endArgb, endArgb, startArgb), stroke, false);
        DrawBatch batch = Batch2D.open(r, UiBatchType.COLORED);
        if (batch == null) return;
        appendPolylineGradient(batch.mesh, points, pointCount, t, closed, startArgb, endArgb, roundCapsAndJoins);
        Batch2D.close();
    }

    public static void polylineLinearGradient(Renderer2D r, double[] points, int pointCount,
                                              double thickness, boolean closed,
                                              double x, double y, double width, double height,
                                              int startArgb, int endArgb, float angleDeg, float offsetPx,
                                              boolean roundCapsAndJoins) {
        if (r.isTextured()) {
            throw new IllegalStateException("Polyline drawing is supported only on Renderer2D.COLOR.");
        }
        if (points == null || pointCount < 2) return;
        double t = Math.max(0.0, thickness);
        if (t <= 0.0) return;

        UiStroke stroke = roundCapsAndJoins ? UiStroke.of(t).withRoundCapsAndJoins() : UiStroke.of(t);
        r.path(UiShape.polyline(points, pointCount, closed),
                UiPaint.linear(startArgb, endArgb, angleDeg, offsetPx), stroke, false);
        DrawBatch batch = Batch2D.open(r, UiBatchType.COLORED);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        appendPolylineLinearGradient(mesh, points, pointCount, t, closed,
                x, y, width, height, startArgb, endArgb, angleDeg, offsetPx, roundCapsAndJoins);
        Batch2D.close();
    }
}
