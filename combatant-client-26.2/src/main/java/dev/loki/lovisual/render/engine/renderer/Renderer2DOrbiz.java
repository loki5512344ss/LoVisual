/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer;

import dev.loki.lovisual.render.engine.renderer.ui.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.UiBatchType;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.util.Mth;

import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.normalizeDegrees;

final class Renderer2DOrbiz {
    private Renderer2DOrbiz() {
    }

    static void orbizRing(Renderer2D r, double cx, double cy,
                          double radius, double thickness,
                          float startAngleDeg, float sweepAngleDeg,
                          float softness, float glowRadius, float glowStrength,
                          int primaryArgb, int secondaryArgb,
                          int colorMode, float angularOffset, float angularCycles,
                          float trackAlpha, float bodyAlpha, float headBoost,
                          float trackGlow, boolean caps) {
        if (r.textured) {
            throw new IllegalStateException("Orbiz ring drawing is supported only on Renderer2D.COLOR.");
        }
        if (radius <= 0.0 || thickness <= 0.0) return;
        if (((primaryArgb >>> 24) & 0xFF) <= 0 && ((secondaryArgb >>> 24) & 0xFF) <= 0) return;

        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.ORBIZ_RING, null, null);
        if (batch == null) {
            r.endAutoBatch(auto);
            return;
        }
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = r.alpha;

        mesh.ensureQuadCapacity();

        int a = (primaryArgb >>> 24) & 0xFF;
        int red = (primaryArgb >>> 16) & 0xFF;
        int green = (primaryArgb >>> 8) & 0xFF;
        int blue = primaryArgb & 0xFF;

        float sr = ((secondaryArgb >>> 16) & 0xFF) / 255.0f;
        float sg = ((secondaryArgb >>> 8) & 0xFF) / 255.0f;
        float sb = (secondaryArgb & 0xFF) / 255.0f;
        float sa = ((secondaryArgb >>> 24) & 0xFF) / 255.0f;

        float stroke = (float) Math.max(0.0, thickness);
        float ringRadius = (float) Math.max(0.0, radius);
        float soft = Math.max(0.0f, softness);
        float glow = Math.max(0.0f, glowRadius);
        float expand = stroke * 0.5f + soft + glow + 1.0f;
        double outerRadius = radius + expand;
        double x = cx - outerRadius;
        double y = cy - outerRadius;
        double w = outerRadius * 2.0;
        double h = outerRadius * 2.0;
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

        int i1 = mesh.vec2(x, y).local2(x, y).color(red, green, blue, a)
                .vec4(ringX, ringY, ringSize, ringSize)
                .vec4(ringRadius, soft, stroke, start)
                .vec4(sweep, glow, glowPower, mode)
                .vec4(sr, sg, sb, sa)
                .vec4(offset, cycles, 0.0f, caps ? 1.0f : 0.0f)
                .vec4(track, body, head, trackGlowAmount)
                .next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(red, green, blue, a)
                .vec4(ringX, ringY, ringSize, ringSize)
                .vec4(ringRadius, soft, stroke, start)
                .vec4(sweep, glow, glowPower, mode)
                .vec4(sr, sg, sb, sa)
                .vec4(offset, cycles, 0.0f, caps ? 1.0f : 0.0f)
                .vec4(track, body, head, trackGlowAmount)
                .next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(red, green, blue, a)
                .vec4(ringX, ringY, ringSize, ringSize)
                .vec4(ringRadius, soft, stroke, start)
                .vec4(sweep, glow, glowPower, mode)
                .vec4(sr, sg, sb, sa)
                .vec4(offset, cycles, 0.0f, caps ? 1.0f : 0.0f)
                .vec4(track, body, head, trackGlowAmount)
                .next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(red, green, blue, a)
                .vec4(ringX, ringY, ringSize, ringSize)
                .vec4(ringRadius, soft, stroke, start)
                .vec4(sweep, glow, glowPower, mode)
                .vec4(sr, sg, sb, sa)
                .vec4(offset, cycles, 0.0f, caps ? 1.0f : 0.0f)
                .vec4(track, body, head, trackGlowAmount)
                .next();
        mesh.quad(i1, i2, i3, i4);

        r.endAutoBatch(auto);
    }
}