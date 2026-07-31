/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.ui.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiPaint;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiShape;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiStroke;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

final class Renderer2DLines {
    private Renderer2DLines() {
    }

    static void line(Renderer2D r, double x1, double y1, double x2, double y2, int argb) {
        double[] p = {x1, y1, x2, y2};
        r.path(UiShape.polyline(p, 2, false), UiPaint.solid(argb), UiStroke.of(1.0), false);
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.LINES, null, null);
        if (batch == null) {
            r.endAutoBatch(auto);
            return;
        }
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = r.alpha;

        mesh.ensureLineCapacity();
        int a = (argb >>> 24) & 0xFF;
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        int i1 = mesh.vec2(x1, y1).color(red, green, blue, a).next();
        int i2 = mesh.vec2(x2, y2).color(red, green, blue, a).next();
        mesh.line(i1, i2);

        r.endAutoBatch(auto);
    }

    static void line(Renderer2D r, double x1, double y1, double x2, double y2, RenderColor color) {
        double[] p = {x1, y1, x2, y2};
        r.path(UiShape.polyline(p, 2, false), UiPaint.solid(color.argb()), UiStroke.of(1.0), false);
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.LINES, null, null);
        if (batch == null) {
            r.endAutoBatch(auto);
            return;
        }
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = r.alpha;

        mesh.ensureLineCapacity();
        int i1 = mesh.vec2(x1, y1).color(color.r, color.g, color.b, color.a).next();
        int i2 = mesh.vec2(x2, y2).color(color.r, color.g, color.b, color.a).next();
        mesh.line(i1, i2);

        r.endAutoBatch(auto);
    }

    static void boxLines(Renderer2D r, double x, double y, double width, double height, int argb) {
        r.shapeStroke(UiShape.rect(x, y, width, height), UiPaint.solid(argb), UiStroke.of(1.0));
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.LINES, null, null);
        if (batch == null) {
            r.endAutoBatch(auto);
            return;
        }
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = r.alpha;

        mesh.ensureCapacity(4, 8);

        int a = (argb >>> 24) & 0xFF;
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        int i1 = mesh.vec2(x, y).color(red, green, blue, a).next();
        int i2 = mesh.vec2(x, y + height).color(red, green, blue, a).next();
        int i3 = mesh.vec2(x + width, y + height).color(red, green, blue, a).next();
        int i4 = mesh.vec2(x + width, y).color(red, green, blue, a).next();

        mesh.line(i1, i2);
        mesh.line(i2, i3);
        mesh.line(i3, i4);
        mesh.line(i4, i1);

        r.endAutoBatch(auto);
    }
}