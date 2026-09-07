/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.renderer2d.path;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.renderer2d.Batch2D;
import dev.loki.lovisual.render.engine.renderer.ui.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiPaint;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiShape;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiStroke;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

/** Immediate-mode 1px lines. */
public final class Lines2D {
    private Lines2D() {
    }

    public static void line(Renderer2D r, double x1, double y1, double x2, double y2, int argb) {
        double[] p = {x1, y1, x2, y2};
        r.path(UiShape.polyline(p, 2, false), UiPaint.solid(argb), UiStroke.of(1.0), false);
        DrawBatch batch = Batch2D.open(r, UiBatchType.LINES);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;

        int[] c = Batch2D.rgba(argb);
        mesh.ensureLineCapacity();
        int i1 = mesh.vec2(x1, y1).color(c[0], c[1], c[2], c[3]).next();
        int i2 = mesh.vec2(x2, y2).color(c[0], c[1], c[2], c[3]).next();
        mesh.line(i1, i2);

        Batch2D.close();
    }

    public static void line(Renderer2D r, double x1, double y1, double x2, double y2, RenderColor color) {
        double[] p = {x1, y1, x2, y2};
        r.path(UiShape.polyline(p, 2, false), UiPaint.solid(color.argb()), UiStroke.of(1.0), false);
        DrawBatch batch = Batch2D.open(r, UiBatchType.LINES);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;

        mesh.ensureLineCapacity();
        int i1 = mesh.vec2(x1, y1).color(color.r, color.g, color.b, color.a).next();
        int i2 = mesh.vec2(x2, y2).color(color.r, color.g, color.b, color.a).next();
        mesh.line(i1, i2);

        Batch2D.close();
    }

    public static void boxLines(Renderer2D r, double x, double y, double width, double height, int argb) {
        r.shapeStroke(UiShape.rect(x, y, width, height), UiPaint.solid(argb), UiStroke.of(1.0));
        DrawBatch batch = Batch2D.open(r, UiBatchType.LINES);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;

        mesh.ensureCapacity(4, 8);
        int[] c = Batch2D.rgba(argb);
        int i1 = mesh.vec2(x, y).color(c[0], c[1], c[2], c[3]).next();
        int i2 = mesh.vec2(x, y + height).color(c[0], c[1], c[2], c[3]).next();
        int i3 = mesh.vec2(x + width, y + height).color(c[0], c[1], c[2], c[3]).next();
        int i4 = mesh.vec2(x + width, y).color(c[0], c[1], c[2], c[3]).next();

        mesh.line(i1, i2);
        mesh.line(i2, i3);
        mesh.line(i3, i4);
        mesh.line(i4, i1);

        Batch2D.close();
    }
}
