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
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiStroke;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.util.Mth;

import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.normalizeDegrees;

final class Renderer2DCircles {
    private Renderer2DCircles() {
    }

    static void circle(Renderer2D r, double cx, double cy, double radius, float softness, int argb) {
        r.shape(UiShape.circle(cx, cy, radius), UiPaint.solid(argb));
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.CIRCLE, null, null);
        if (batch == null) {
            r.endAutoBatch(auto);
            return;
        }
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = r.alpha;

        mesh.ensureQuadCapacity();

        int a = (argb >>> 24) & 0xFF;
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;

        double outerRadius = radius + Math.max(0.0f, softness);
        double x = cx - outerRadius;
        double y = cy - outerRadius;
        double w = outerRadius * 2.0;
        double h = outerRadius * 2.0;

        int i1 = mesh.vec2(x, y).local2(x, y).color(red, green, blue, a).vec4(x, y, w, h).vec4((float) radius, softness, 0f, 0f).next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(red, green, blue, a).vec4(x, y, w, h).vec4((float) radius, softness, 0f, 0f).next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(red, green, blue, a).vec4(x, y, w, h).vec4((float) radius, softness, 0f, 0f).next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(red, green, blue, a).vec4(x, y, w, h).vec4((float) radius, softness, 0f, 0f).next();
        mesh.quad(i1, i2, i3, i4);

        r.endAutoBatch(auto);
    }

    static void circleStroke(Renderer2D r, double cx, double cy, double radius, double thickness, float softness, int argb) {
        r.shapeStroke(UiShape.circle(cx, cy, radius), UiPaint.solid(argb), UiStroke.of(thickness));
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.CIRCLE, null, null);
        if (batch == null) {
            r.endAutoBatch(auto);
            return;
        }
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = r.alpha;

        mesh.ensureQuadCapacity();

        int a = (argb >>> 24) & 0xFF;
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;

        float stroke = (float) Math.max(0.0, thickness);
        double outerRadius = radius + stroke * 0.5 + softness;
        double x = cx - outerRadius;
        double y = cy - outerRadius;
        double w = outerRadius * 2.0;
        double h = outerRadius * 2.0;

        int i1 = mesh.vec2(x, y).local2(x, y).color(red, green, blue, a).vec4(x, y, w, h).vec4((float) radius, softness, stroke, 0f).next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(red, green, blue, a).vec4(x, y, w, h).vec4((float) radius, softness, stroke, 0f).next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(red, green, blue, a).vec4(x, y, w, h).vec4((float) radius, softness, stroke, 0f).next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(red, green, blue, a).vec4(x, y, w, h).vec4((float) radius, softness, stroke, 0f).next();
        mesh.quad(i1, i2, i3, i4);

        r.endAutoBatch(auto);
    }

    static void arcStrokeQuad(Renderer2D r, double cx, double cy, double radius, double thickness,
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

        r.shapeStroke(UiShape.arc(cx, cy, radius, start, end), UiPaint.corners(cTopLeft, cTopRight, cBottomRight, cBottomLeft), UiStroke.of(thickness));
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.ARC, null, null);
        if (batch == null) {
            r.endAutoBatch(auto);
            return;
        }
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = r.alpha;

        mesh.ensureQuadCapacity();

        float stroke = (float) Math.max(0.0, thickness);
        double outerRadius = radius + stroke * 0.5 + softness;
        double x = cx - outerRadius;
        double y = cy - outerRadius;
        double w = outerRadius * 2.0;
        double h = outerRadius * 2.0;

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
                .vec4((float) radius, softness, stroke, start).vec4(end, caps ? 1f : 0f, 0f, 0f).next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(blR, blG, blB, blA).vec4(x, y, w, h)
                .vec4((float) radius, softness, stroke, start).vec4(end, caps ? 1f : 0f, 0f, 0f).next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(brR, brG, brB, brA).vec4(x, y, w, h)
                .vec4((float) radius, softness, stroke, start).vec4(end, caps ? 1f : 0f, 0f, 0f).next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(trR, trG, trB, trA).vec4(x, y, w, h)
                .vec4((float) radius, softness, stroke, start).vec4(end, caps ? 1f : 0f, 0f, 0f).next();
        mesh.quad(i1, i2, i3, i4);

        r.endAutoBatch(auto);
    }
}