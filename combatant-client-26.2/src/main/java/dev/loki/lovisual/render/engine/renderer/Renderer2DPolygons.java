/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer;

import dev.loki.lovisual.render.engine.command.UiShapeCommand;
import dev.loki.lovisual.render.engine.renderer.ui.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiBoxPathBuilder;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiBoxShape;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiPaint;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiPaintKind;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiPathCap;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiPathJoin;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiShape;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiStroke;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.*;

final class Renderer2DPolygons {
    private Renderer2DPolygons() {
    }

    static void box(Renderer2D r, UiBoxShape box, UiPaint paint, UiStroke stroke, boolean fill) {
        if (box == null || paint == null) return;
        UiStroke safeStroke = stroke == null ? UiStroke.NONE : stroke;
        Renderer2D.recordUi(new UiShapeCommand(UiShape.box(box), paint, safeStroke, fill));
        renderFlexibleBoxFallback(r, box, paint, safeStroke, fill);
    }

    static void renderFlexibleBoxFallback(Renderer2D r, UiBoxShape box, UiPaint paint, UiStroke stroke, boolean fill) {
        if (r.textured || box == null || paint == null) return;
        double[] rectShapeTmp = r.rectShapeTmp;
        int count = UiBoxPathBuilder.write(box, rectShapeTmp, rectShapeTmp.length / 2);
        if (count < 3) return;

        if (fill) {
            switch (paint.kind()) {
                case LINEAR_GRADIENT ->
                        polygonGradient(r, rectShapeTmp, count, box.bounds().x(), box.bounds().y(), box.bounds().width(), box.bounds().height(),
                                paint.topLeft(), paint.topRight(), paint.angleDeg(), paint.offsetPx());
                default -> polygon(r, rectShapeTmp, count, paint.solidColor());
            }
        } else if (stroke != null && stroke.enabled()) {
            if (paint.kind() == UiPaintKind.LINEAR_GRADIENT) {
                polylineLinearGradient(r, rectShapeTmp, count, stroke.thickness(), true,
                        box.bounds().x(), box.bounds().y(), box.bounds().width(), box.bounds().height(),
                        paint.topLeft(), paint.topRight(), paint.angleDeg(), paint.offsetPx(),
                        stroke.join() == UiPathJoin.ROUND || stroke.cap() == UiPathCap.ROUND);
            } else {
                polyline(r, rectShapeTmp, count, stroke.thickness(), true, paint.solidColor(),
                        stroke.join() == UiPathJoin.ROUND || stroke.cap() == UiPathCap.ROUND);
            }
        }
    }

    static void polygon(Renderer2D r, double[] points, int pointCount, int argb) {
        if (r.textured) {
            throw new IllegalStateException("Polygon drawing is supported only on Renderer2D.COLOR.");
        }
        int safeCount = Math.min(pointCount, points != null ? points.length / 2 : 0);
        safeCount = Math.min(safeCount, Math.min(r.polygonIndexTmp.length, r.polygonVertexTmp.length));
        if (points == null || safeCount < 3) return;

        r.path(UiShape.polyline(points, safeCount, true), UiPaint.solid(argb), UiStroke.NONE, true);
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.COLORED, null, null);
        if (batch == null) {
            r.endAutoBatch(auto);
            return;
        }
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = r.alpha;
        appendPolygon(mesh, points, safeCount, r.polygonIndexTmp, r.polygonVertexTmp, argb);
        r.endAutoBatch(auto);
    }

    static void polygonGradient(Renderer2D r, double[] points, int pointCount,
                                double x, double y, double width, double height,
                                int startArgb, int endArgb, float angleDeg, float offsetPx) {
        if (r.textured) {
            throw new IllegalStateException("Polygon drawing is supported only on Renderer2D.COLOR.");
        }
        if (points == null || pointCount < 3) return;

        r.path(UiShape.polyline(points, pointCount, true), UiPaint.linear(startArgb, endArgb, angleDeg, offsetPx), UiStroke.NONE, true);
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.COLORED, null, null);
        if (batch == null) {
            r.endAutoBatch(auto);
            return;
        }
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = r.alpha;
        appendPolygonGradient(mesh, points, pointCount, r.polygonIndexTmp, r.polygonVertexTmp,
                x, y, width, height, startArgb, endArgb, angleDeg, offsetPx);
        r.endAutoBatch(auto);
    }

    static void polygonStroke(Renderer2D r, double[] points, int pointCount, double thickness, boolean closed, int argb) {
        polyline(r, points, pointCount, thickness, closed, argb, false);
    }

    static void polyline(Renderer2D r, double[] points, int pointCount, double thickness, boolean closed, int argb) {
        polyline(r, points, pointCount, thickness, closed, argb, false);
    }

    static void polyline(Renderer2D r, double[] points, int pointCount, double thickness, boolean closed, int argb, boolean roundCapsAndJoins) {
        if (r.textured) {
            throw new IllegalStateException("Polyline drawing is supported only on Renderer2D.COLOR.");
        }
        if (points == null || pointCount < 2) return;
        double t = Math.max(0.0, thickness);
        if (t <= 0.0) return;

        r.path(UiShape.polyline(points, pointCount, closed), UiPaint.solid(argb),
                roundCapsAndJoins ? UiStroke.of(t).withRoundCapsAndJoins() : UiStroke.of(t), false);
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.COLORED, null, null);
        if (batch == null) {
            r.endAutoBatch(auto);
            return;
        }
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = r.alpha;
        appendPolyline(mesh, points, pointCount, t, closed, argb, roundCapsAndJoins);
        r.endAutoBatch(auto);
    }

    static void polylineGradient(Renderer2D r, double[] points, int pointCount, double thickness, boolean closed,
                                 int startArgb, int endArgb, boolean roundCapsAndJoins) {
        if (r.textured) {
            throw new IllegalStateException("Polyline drawing is supported only on Renderer2D.COLOR.");
        }
        if (points == null || pointCount < 2) return;
        double t = Math.max(0.0, thickness);
        if (t <= 0.0) return;

        r.path(UiShape.polyline(points, pointCount, closed), UiPaint.corners(startArgb, endArgb, endArgb, startArgb),
                roundCapsAndJoins ? UiStroke.of(t).withRoundCapsAndJoins() : UiStroke.of(t), false);
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.COLORED, null, null);
        if (batch == null) {
            r.endAutoBatch(auto);
            return;
        }
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = r.alpha;
        appendPolylineGradient(mesh, points, pointCount, t, closed, startArgb, endArgb, roundCapsAndJoins);
        r.endAutoBatch(auto);
    }

    static void polylineLinearGradient(Renderer2D r, double[] points, int pointCount, double thickness, boolean closed,
                                       double x, double y, double width, double height,
                                       int startArgb, int endArgb, float angleDeg, float offsetPx,
                                       boolean roundCapsAndJoins) {
        if (r.textured) {
            throw new IllegalStateException("Polyline drawing is supported only on Renderer2D.COLOR.");
        }
        if (points == null || pointCount < 2) return;
        double t = Math.max(0.0, thickness);
        if (t <= 0.0) return;

        r.path(UiShape.polyline(points, pointCount, closed), UiPaint.linear(startArgb, endArgb, angleDeg, offsetPx),
                roundCapsAndJoins ? UiStroke.of(t).withRoundCapsAndJoins() : UiStroke.of(t), false);
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.COLORED, null, null);
        if (batch == null) {
            r.endAutoBatch(auto);
            return;
        }
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = r.alpha;
        appendPolylineLinearGradient(mesh, points, pointCount, t, closed,
                x, y, width, height, startArgb, endArgb, angleDeg, offsetPx, roundCapsAndJoins);
        r.endAutoBatch(auto);
    }
}