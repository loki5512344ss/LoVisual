/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer;

import dev.loki.lovisual.render.engine.renderer.ui.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.UiBatchType;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.clampRoundedRadius;

final class Renderer2DRectsGlow {
    private Renderer2DRectsGlow() {
    }

    static void roundedRectGlow(Renderer2D r, double x, double y, double w, double h,
                                float radius, float softness, float glow, int argb) {
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.ROUNDED_GLOW, null, null);
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
        float clampedRadius = clampRoundedRadius(radius, w, h);

        double expand = glow;
        double gx = x - expand;
        double gy = y - expand;
        double gw = w + expand * 2.0f;
        double gh = h + expand * 2.0f;

        int i1 = mesh.vec2(gx, gy).local2(gx, gy).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, glow, 0f).next();
        int i2 = mesh.vec2(gx, gy + gh).local2(gx, gy + gh).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, glow, 0f).next();
        int i3 = mesh.vec2(gx + gw, gy + gh).local2(gx + gw, gy + gh).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, glow, 0f).next();
        int i4 = mesh.vec2(gx + gw, gy).local2(gx + gw, gy).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, glow, 0f).next();
        mesh.quad(i1, i2, i3, i4);

        r.endAutoBatch(auto);
    }

    static void roundedRectShadow(Renderer2D r, double x, double y, double w, double h,
                                  float radius, float softness, float spread, int argb) {
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.ROUNDED_SHADOW, null, null);
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
        float clampedRadius = clampRoundedRadius(radius, w, h);

        double expandX = 2.0f;
        double expandY = spread + 2.0f;
        double gx = x - expandX;
        double gw = w + expandX * 2.0f;
        double gy = y;
        double gh = h + expandY;

        int i1 = mesh.vec2(gx, gy).local2(gx, gy).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, spread, 0f).next();
        int i2 = mesh.vec2(gx, gy + gh).local2(gx, gy + gh).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, spread, 0f).next();
        int i3 = mesh.vec2(gx + gw, gy + gh).local2(gx + gw, gy + gh).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, spread, 0f).next();
        int i4 = mesh.vec2(gx + gw, gy).local2(gx + gw, gy).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, spread, 0f).next();
        mesh.quad(i1, i2, i3, i4);

        r.endAutoBatch(auto);
    }

    static void roundedRectSoftShadow(Renderer2D r, double x, double y, double w, double h,
                                      float radius, float blur, float innerAlpha, int argb) {
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.ROUNDED_SOFT_SHADOW, null, null);
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

        double expand = blur * 2.0;
        double sx = x - expand;
        double sy = y - expand;
        double sw = w + expand * 2.0;
        double sh = h + expand * 2.0;

        int i1 = mesh.vec2(sx, sy).local2(sx, sy).color(red, green, blue, a).vec4(x, y, w, h).vec4(radius, blur, innerAlpha, 0f).next();
        int i2 = mesh.vec2(sx, sy + sh).local2(sx, sy + sh).color(red, green, blue, a).vec4(x, y, w, h).vec4(radius, blur, innerAlpha, 0f).next();
        int i3 = mesh.vec2(sx + sw, sy + sh).local2(sx + sw, sy + sh).color(red, green, blue, a).vec4(x, y, w, h).vec4(radius, blur, innerAlpha, 0f).next();
        int i4 = mesh.vec2(sx + sw, sy).local2(sx + sw, sy).color(red, green, blue, a).vec4(x, y, w, h).vec4(radius, blur, innerAlpha, 0f).next();
        mesh.quad(i1, i2, i3, i4);

        r.endAutoBatch(auto);
    }

    static void radialGlowMasked(Renderer2D r, double x, double y, double w, double h,
                                float radius, float softness,
                                float glowRadius, float cx, float cy,
                                int argb) {
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.RADIAL_GLOW, null, null);
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
        float clampedRadius = clampRoundedRadius(radius, w, h);

        int i1 = mesh.vec2(x, y).local2(x, y).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, glowRadius, 0.0f).vec4(cx, cy, 0f, 0f).next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, glowRadius, 0.0f).vec4(cx, cy, 0f, 0f).next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, glowRadius, 0.0f).vec4(cx, cy, 0f, 0f).next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, glowRadius, 0.0f).vec4(cx, cy, 0f, 0f).next();
        mesh.quad(i1, i2, i3, i4);

        r.endAutoBatch(auto);
    }
}