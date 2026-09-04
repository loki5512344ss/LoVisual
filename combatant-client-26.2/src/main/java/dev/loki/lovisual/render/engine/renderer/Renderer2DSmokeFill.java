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
import net.minecraft.util.Mth;

final class Renderer2DSmokeFill {
    private Renderer2DSmokeFill() {
    }

    static void roundedSmokeFill(Renderer2D r, double x, double y, double w, double h,
                                 float radius, float softness,
                                 float fillRatio,
                                 boolean fromRight,
                                 int firstArgb,
                                 int secondArgb,
                                 int thirdArgb,
                                 float time,
                                 float smokeScale,
                                 float smokeMix,
                                 int octaves,
                                 float flowX,
                                 float flowY,
                                 float intensity) {
        if (w <= 0.0 || h <= 0.0) return;
        float fill = Mth.clamp(fillRatio, 0.0f, 1.0f);
        if (fill <= 0.0001f) return;

        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.ROUNDED_FILL_SMOKE, null, null);
        if (batch == null) {
            r.endAutoBatch(auto);
            return;
        }
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = r.alpha;

        mesh.ensureQuadCapacity();

        int a0 = (firstArgb >>> 24) & 0xFF;
        int r0 = (firstArgb >>> 16) & 0xFF;
        int g0 = (firstArgb >>> 8) & 0xFF;
        int b0 = firstArgb & 0xFF;

        float r1 = ((secondArgb >>> 16) & 0xFF) / 255.0f;
        float g1 = ((secondArgb >>> 8) & 0xFF) / 255.0f;
        float b1 = (secondArgb & 0xFF) / 255.0f;
        float a1 = ((secondArgb >>> 24) & 0xFF) / 255.0f;

        float r2 = ((thirdArgb >>> 16) & 0xFF) / 255.0f;
        float g2 = ((thirdArgb >>> 8) & 0xFF) / 255.0f;
        float b2 = (thirdArgb & 0xFF) / 255.0f;
        float a2 = ((thirdArgb >>> 24) & 0xFF) / 255.0f;

        float fromRightFlag = fromRight ? 1.0f : 0.0f;
        float safeScale = Math.max(0.1f, smokeScale);
        float safeMix = Mth.clamp(smokeMix, 0.0f, 1.0f);
        float safeOctaves = Mth.clamp(octaves, 1, 8);
        float safeIntensity = Math.max(0.0f, intensity);

        int i1 = mesh.vec2(x, y).local2(x, y).color(r0, g0, b0, a0)
                .vec4((float) x, (float) y, (float) w, (float) h)
                .vec4(radius, softness, fill, fromRightFlag)
                .vec4(r1, g1, b1, a1)
                .vec4(r2, g2, b2, a2)
                .vec4(time, safeScale, safeMix, safeOctaves)
                .vec4(flowX, flowY, safeIntensity, 0.0f)
                .next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(r0, g0, b0, a0)
                .vec4((float) x, (float) y, (float) w, (float) h)
                .vec4(radius, softness, fill, fromRightFlag)
                .vec4(r1, g1, b1, a1)
                .vec4(r2, g2, b2, a2)
                .vec4(time, safeScale, safeMix, safeOctaves)
                .vec4(flowX, flowY, safeIntensity, 0.0f)
                .next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(r0, g0, b0, a0)
                .vec4((float) x, (float) y, (float) w, (float) h)
                .vec4(radius, softness, fill, fromRightFlag)
                .vec4(r1, g1, b1, a1)
                .vec4(r2, g2, b2, a2)
                .vec4(time, safeScale, safeMix, safeOctaves)
                .vec4(flowX, flowY, safeIntensity, 0.0f)
                .next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(r0, g0, b0, a0)
                .vec4((float) x, (float) y, (float) w, (float) h)
                .vec4(radius, softness, fill, fromRightFlag)
                .vec4(r1, g1, b1, a1)
                .vec4(r2, g2, b2, a2)
                .vec4(time, safeScale, safeMix, safeOctaves)
                .vec4(flowX, flowY, safeIntensity, 0.0f)
                .next();
        mesh.quad(i1, i2, i3, i4);

        r.endAutoBatch(auto);
    }
}