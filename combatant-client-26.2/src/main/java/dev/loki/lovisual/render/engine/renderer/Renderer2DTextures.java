/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.command.UiShapeCommand;
import dev.loki.lovisual.render.engine.command.UiTextureDrawCommand;
import dev.loki.lovisual.render.engine.renderer.ui.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiPaint;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiShape;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiStroke;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.clampRoundedRadius;

final class Renderer2DTextures {
    private Renderer2DTextures() {
    }

    static void textureQuad(Renderer2D r, GpuTextureView samplerView, GpuSampler sampler,
                            double x, double y, double w, double h, int argb) {
        if (r.textured) {
            throw new IllegalStateException("Batched texture quad drawing is supported only on Renderer2D.COLOR.");
        }
        if (samplerView == null || sampler == null) return;
        if (w == 0.0 || h == 0.0) return;

        Renderer2D.recordUi(new UiTextureDrawCommand(samplerView, sampler, UiShape.rect(x, y, w, h), UiPaint.solid(argb), 0f, 0f, 1f, 1f, false));
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.TEXTURED, samplerView, sampler);
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

        int i1 = mesh.vec2(x, y).raw2(0.0, 0.0).color(red, green, blue, a).next();
        int i2 = mesh.vec2(x, y + h).raw2(0.0, 1.0).color(red, green, blue, a).next();
        int i3 = mesh.vec2(x + w, y + h).raw2(1.0, 1.0).color(red, green, blue, a).next();
        int i4 = mesh.vec2(x + w, y).raw2(1.0, 0.0).color(red, green, blue, a).next();
        mesh.quad(i1, i2, i3, i4);

        r.endAutoBatch(auto);
    }

    static void msdfTextureQuad(Renderer2D r, GpuTextureView samplerView, GpuSampler sampler,
                                double x, double y, double w, double h,
                                int topLeft, int topRight, int bottomRight, int bottomLeft,
                                float pxRange, int atlasWidth, int atlasHeight) {
        if (r.textured) {
            throw new IllegalStateException("Batched MSDF texture quad drawing is supported only on Renderer2D.COLOR.");
        }
        if (samplerView == null || sampler == null) return;
        if (w == 0.0 || h == 0.0) return;
        if (pxRange <= 0.0f || atlasWidth <= 0 || atlasHeight <= 0) return;

        Renderer2D.recordUi(new UiTextureDrawCommand(samplerView, sampler, UiShape.rect(x, y, w, h), UiPaint.solid(topLeft), 0f, 0f, 1f, 1f, false));
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreateMsdf(samplerView, sampler, pxRange, atlasWidth, atlasHeight);
        if (batch == null) {
            r.endAutoBatch(auto);
            return;
        }
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = r.alpha;

        mesh.ensureQuadCapacity();

        int i1 = mesh.vec2(x, y).raw2(0.0, 0.0).color((topLeft >>> 16) & 0xFF, (topLeft >>> 8) & 0xFF, topLeft & 0xFF, (topLeft >>> 24) & 0xFF).next();
        int i2 = mesh.vec2(x, y + h).raw2(0.0, 1.0).color((bottomLeft >>> 16) & 0xFF, (bottomLeft >>> 8) & 0xFF, bottomLeft & 0xFF, (bottomLeft >>> 24) & 0xFF).next();
        int i3 = mesh.vec2(x + w, y + h).raw2(1.0, 1.0).color((bottomRight >>> 16) & 0xFF, (bottomRight >>> 8) & 0xFF, bottomRight & 0xFF, (bottomRight >>> 24) & 0xFF).next();
        int i4 = mesh.vec2(x + w, y).raw2(1.0, 0.0).color((topRight >>> 16) & 0xFF, (topRight >>> 8) & 0xFF, topRight & 0xFF, (topRight >>> 24) & 0xFF).next();
        mesh.quad(i1, i2, i3, i4);

        r.endAutoBatch(auto);
    }

    static void texQuad(Renderer2D r, double x, double y, double width, double height,
                        double texX1, double texY1, double texX2, double texY2,
                        int argb) {
        Renderer2D.recordUi(new UiShapeCommand(UiShape.rect(x, y, width, height), UiPaint.solid(argb), UiStroke.NONE, true));
        MeshBuilder texturedTriangles = r.texturedTriangles;
        if (texturedTriangles == null) return;
        texturedTriangles.alpha = r.alpha;
        texturedTriangles.ensureQuadCapacity();

        int a = (argb >>> 24) & 0xFF;
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        int i1 = texturedTriangles.vec2(x, y).raw2(texX1, texY1).color(red, green, blue, a).next();
        int i2 = texturedTriangles.vec2(x, y + height).raw2(texX1, texY2).color(red, green, blue, a).next();
        int i3 = texturedTriangles.vec2(x + width, y + height).raw2(texX2, texY2).color(red, green, blue, a).next();
        int i4 = texturedTriangles.vec2(x + width, y).raw2(texX2, texY1).color(red, green, blue, a).next();

        texturedTriangles.quad(i1, i2, i3, i4);
    }

    static void roundedTexRect(Renderer2D r, double x, double y, double w, double h,
                               float radius, float softness,
                               double texX1, double texY1, double texX2, double texY2,
                               int argb, GpuTextureView samplerView, GpuSampler sampler) {
        if (!r.textured) {
            throw new IllegalStateException("Renderer2D is not configured for textures.");
        }
        if (samplerView == null || sampler == null) return;

        Renderer2D.recordUi(new UiTextureDrawCommand(samplerView, sampler, UiShape.roundedRect(x, y, w, h, radius), UiPaint.solid(argb),
                (float) texX1, (float) texY1, (float) texX2, (float) texY2, false));
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.ROUNDED_TEXTURED, samplerView, sampler);
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
        int i1 = mesh.vec2(x, y).raw2(texX1, texY1).local2(x, y).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, 0f, 0f).next();
        int i2 = mesh.vec2(x, y + h).raw2(texX1, texY2).local2(x, y + h).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, 0f, 0f).next();
        int i3 = mesh.vec2(x + w, y + h).raw2(texX2, texY2).local2(x + w, y + h).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, 0f, 0f).next();
        int i4 = mesh.vec2(x + w, y).raw2(texX2, texY1).local2(x + w, y).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, 0f, 0f).next();
        mesh.quad(i1, i2, i3, i4);

        r.endAutoBatch(auto);
    }

    static void roundedTexMaskRect(Renderer2D r, double x, double y, double w, double h,
                                   float radius, float softness,
                                   double texX1, double texY1, double texX2, double texY2,
                                   int argb, GpuTextureView samplerView, GpuSampler sampler) {
        if (!r.textured) {
            throw new IllegalStateException("Renderer2D is not configured for textures.");
        }
        if (samplerView == null || sampler == null) return;

        Renderer2D.recordUi(new UiTextureDrawCommand(samplerView, sampler, UiShape.roundedRect(x, y, w, h, radius), UiPaint.solid(argb),
                (float) texX1, (float) texY1, (float) texX2, (float) texY2, true));
        boolean auto = r.beginAutoBatch();
        DrawBatch batch = Renderer2D.UI_BATCHER.getOrCreate(UiBatchType.ROUNDED_TEXTURED_MASK, samplerView, sampler);
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
        int i1 = mesh.vec2(x, y).raw2(texX1, texY1).local2(x, y).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, 0f, 0f).next();
        int i2 = mesh.vec2(x, y + h).raw2(texX1, texY2).local2(x, y + h).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, 0f, 0f).next();
        int i3 = mesh.vec2(x + w, y + h).raw2(texX2, texY2).local2(x + w, y + h).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, 0f, 0f).next();
        int i4 = mesh.vec2(x + w, y).raw2(texX2, texY1).local2(x + w, y).color(red, green, blue, a).vec4(x, y, w, h).vec4(clampedRadius, 0.0f, 0f, 0f).next();
        mesh.quad(i1, i2, i3, i4);

        r.endAutoBatch(auto);
    }
}