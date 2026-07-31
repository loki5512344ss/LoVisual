/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.postprocess.PostProcessManager;
import dev.loki.lovisual.render.engine.renderer.ui.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiEffectSpec;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiShape;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.client.Minecraft;

import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.clamp01;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.normalizeCornerRadii;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.packLiquidGlassPayload;

final class Renderer2DLiquidGlass {
    private Renderer2DLiquidGlass() {
    }

    static void liquidGlassRectCorners(Renderer2D r, double x, double y, double w, double h,
                                       float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                       float softness,
                                       int tintArgb,
                                       float glassAlpha,
                                       float blurAlpha,
                                       float fresnelPower,
                                       float fresnelAlpha,
                                       float baseAlpha,
                                       float fresnelMix,
                                       float distortPx,
                                       float squirclePower) {
        if (w <= 0.0 || h <= 0.0) return;

        r.effect(UiEffectSpec.liquidGlass(UiShape.roundedRect(x, y, w, h, radiusTL, radiusTR, radiusBR, radiusBL),
                softness, softness, distortPx, tintArgb));
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        RenderTarget fb = mc.gameRenderer.mainRenderTarget();
        if (fb == null) return;
        GpuTextureView src = fb.getColorTextureView();
        if (src == null) return;
        GpuSampler sampler = PostProcessManager.getSampler();
        if (sampler == null) return;

        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreateBlur(UiBatchType.LIQUID_GLASS, src, sampler,
                Renderer2D.DEFAULT_LIQUID_GLASS_BLUR_QUALITY, Renderer2D.LIQUID_GLASS_KAWASE_OFFSET_PX);
        if (batch == null) {
            r.endAutoBatch(auto);
            return;
        }
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = r.alpha;

        mesh.ensureQuadCapacity();

        int a = (tintArgb >>> 24) & 0xFF;
        int red = (tintArgb >>> 16) & 0xFF;
        int green = (tintArgb >>> 8) & 0xFF;
        int blue = tintArgb & 0xFF;

        float clampedGlassAlpha = clamp01(glassAlpha);
        float clampedBlurAlpha = clamp01(blurAlpha);
        int finalA = (int) (a * clampedGlassAlpha);

        float[] cornerRadiiTmp = r.cornerRadiiTmp;
        normalizeCornerRadii(w, h, radiusTL, radiusTR, radiusBR, radiusBL, cornerRadiiTmp);

        float thickness = Math.max(0.0f, softness);
        float mix = clamp01(fresnelMix);
        float fa = clamp01(fresnelAlpha);
        float ba = clamp01(baseAlpha);
        float packedDistort = packLiquidGlassPayload(distortPx, squirclePower, clampedBlurAlpha);

        int i1 = mesh.vec2(x, y).raw2(mix, packedDistort).local2(x, y).color(red, green, blue, finalA)
                .vec4(x, y, w, h)
                .vec4(cornerRadiiTmp[0], cornerRadiiTmp[1], cornerRadiiTmp[2], cornerRadiiTmp[3])
                .vec4(thickness, fresnelPower, fa, ba)
                .next();
        int i2 = mesh.vec2(x, y + h).raw2(mix, packedDistort).local2(x, y + h).color(red, green, blue, finalA)
                .vec4(x, y, w, h)
                .vec4(cornerRadiiTmp[0], cornerRadiiTmp[1], cornerRadiiTmp[2], cornerRadiiTmp[3])
                .vec4(thickness, fresnelPower, fa, ba)
                .next();
        int i3 = mesh.vec2(x + w, y + h).raw2(mix, packedDistort).local2(x + w, y + h).color(red, green, blue, finalA)
                .vec4(x, y, w, h)
                .vec4(cornerRadiiTmp[0], cornerRadiiTmp[1], cornerRadiiTmp[2], cornerRadiiTmp[3])
                .vec4(thickness, fresnelPower, fa, ba)
                .next();
        int i4 = mesh.vec2(x + w, y).raw2(mix, packedDistort).local2(x + w, y).color(red, green, blue, finalA)
                .vec4(x, y, w, h)
                .vec4(cornerRadiiTmp[0], cornerRadiiTmp[1], cornerRadiiTmp[2], cornerRadiiTmp[3])
                .vec4(thickness, fresnelPower, fa, ba)
                .next();

        mesh.quad(i1, i2, i3, i4);

        r.endAutoBatch(auto);
    }
}