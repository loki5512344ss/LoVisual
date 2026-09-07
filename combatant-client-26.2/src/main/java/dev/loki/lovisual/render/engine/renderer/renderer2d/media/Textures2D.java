/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.renderer2d.media;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.command.UiShapeCommand;
import dev.loki.lovisual.render.engine.command.UiTextureDrawCommand;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.renderer2d.Batch2D;
import dev.loki.lovisual.render.engine.renderer.ui.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.ui.UiRenderDispatcher;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiPaint;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiShape;
import dev.loki.lovisual.render.engine.renderer.ui.draw.UiStroke;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.clampRoundedRadius;

/** Texture quads: plain, MSDF-font and rounded-corner variants. */
public final class Textures2D {
    private Textures2D() {
    }

    public static void textureQuad(Renderer2D r, GpuTextureView samplerView, GpuSampler sampler,
                                   double x, double y, double w, double h, int argb) {
        if (r.isTextured()) {
            throw new IllegalStateException("Batched texture quad drawing is supported only on Renderer2D.COLOR.");
        }
        if (samplerView == null || sampler == null) return;
        if (w == 0.0 || h == 0.0) return;

        UiRenderDispatcher.record(new UiTextureDrawCommand(samplerView, sampler,
                UiShape.rect(x, y, w, h), UiPaint.solid(argb), 0f, 0f, 1f, 1f, false));
        DrawBatch batch = Batch2D.open(r, UiBatchType.TEXTURED, samplerView, sampler);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        mesh.ensureQuadCapacity();

        int[] c = Batch2D.rgba(argb);
        int i1 = mesh.vec2(x, y).raw2(0.0, 0.0).color(c[0], c[1], c[2], c[3]).next();
        int i2 = mesh.vec2(x, y + h).raw2(0.0, 1.0).color(c[0], c[1], c[2], c[3]).next();
        int i3 = mesh.vec2(x + w, y + h).raw2(1.0, 1.0).color(c[0], c[1], c[2], c[3]).next();
        int i4 = mesh.vec2(x + w, y).raw2(1.0, 0.0).color(c[0], c[1], c[2], c[3]).next();
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    public static void msdfTextureQuad(Renderer2D r, GpuTextureView samplerView, GpuSampler sampler,
                                       double x, double y, double w, double h,
                                       int topLeft, int topRight, int bottomRight, int bottomLeft,
                                       float pxRange, int atlasWidth, int atlasHeight) {
        if (r.isTextured()) {
            throw new IllegalStateException("Batched MSDF texture quad drawing is supported only on Renderer2D.COLOR.");
        }
        if (samplerView == null || sampler == null) return;
        if (w == 0.0 || h == 0.0) return;
        if (pxRange <= 0.0f || atlasWidth <= 0 || atlasHeight <= 0) return;

        UiRenderDispatcher.record(new UiTextureDrawCommand(samplerView, sampler,
                UiShape.rect(x, y, w, h), UiPaint.solid(topLeft), 0f, 0f, 1f, 1f, false));
        DrawBatch batch = Batch2D.openMsdf(r, samplerView, sampler, pxRange, atlasWidth, atlasHeight);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        mesh.ensureQuadCapacity();

        int i1 = msdfVertex(mesh, x, y, 0.0, 0.0, topLeft);
        int i2 = msdfVertex(mesh, x, y + h, 0.0, 1.0, bottomLeft);
        int i3 = msdfVertex(mesh, x + w, y + h, 1.0, 1.0, bottomRight);
        int i4 = msdfVertex(mesh, x + w, y, 1.0, 0.0, topRight);
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    public static void texQuad(Renderer2D r, double x, double y, double width, double height,
                               double texX1, double texY1, double texX2, double texY2,
                               int argb) {
        UiRenderDispatcher.record(new UiShapeCommand(UiShape.rect(x, y, width, height),
                UiPaint.solid(argb), UiStroke.NONE, true));
        MeshBuilder texturedTriangles = r.texturedMesh();
        if (texturedTriangles == null) return;
        texturedTriangles.alpha = r.getAlpha();
        texturedTriangles.ensureQuadCapacity();

        int[] c = Batch2D.rgba(argb);
        int i1 = texturedTriangles.vec2(x, y).raw2(texX1, texY1).color(c[0], c[1], c[2], c[3]).next();
        int i2 = texturedTriangles.vec2(x, y + height).raw2(texX1, texY2).color(c[0], c[1], c[2], c[3]).next();
        int i3 = texturedTriangles.vec2(x + width, y + height).raw2(texX2, texY2).color(c[0], c[1], c[2], c[3]).next();
        int i4 = texturedTriangles.vec2(x + width, y).raw2(texX2, texY1).color(c[0], c[1], c[2], c[3]).next();
        texturedTriangles.quad(i1, i2, i3, i4);
    }

    public static void roundedTexRect(Renderer2D r, double x, double y, double w, double h,
                                      float radius, float softness,
                                      double texX1, double texY1, double texX2, double texY2,
                                      int argb, GpuTextureView samplerView, GpuSampler sampler) {
        roundedTex(r, x, y, w, h, radius, texX1, texY1, texX2, texY2, argb, samplerView, sampler,
                UiBatchType.ROUNDED_TEXTURED, false);
    }

    public static void roundedTexMaskRect(Renderer2D r, double x, double y, double w, double h,
                                          float radius, float softness,
                                          double texX1, double texY1, double texX2, double texY2,
                                          int argb, GpuTextureView samplerView, GpuSampler sampler) {
        roundedTex(r, x, y, w, h, radius, texX1, texY1, texX2, texY2, argb, samplerView, sampler,
                UiBatchType.ROUNDED_TEXTURED_MASK, true);
    }

    private static void roundedTex(Renderer2D r, double x, double y, double w, double h,
                                   float radius,
                                   double texX1, double texY1, double texX2, double texY2,
                                   int argb, GpuTextureView samplerView, GpuSampler sampler,
                                   UiBatchType type, boolean mask) {
        if (!r.isTextured()) {
            throw new IllegalStateException("Renderer2D is not configured for textures.");
        }
        if (samplerView == null || sampler == null) return;

        UiRenderDispatcher.record(new UiTextureDrawCommand(samplerView, sampler,
                UiShape.roundedRect(x, y, w, h, radius), UiPaint.solid(argb),
                (float) texX1, (float) texY1, (float) texX2, (float) texY2, mask));
        DrawBatch batch = Batch2D.open(r, type, samplerView, sampler);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        mesh.ensureQuadCapacity();

        int[] c = Batch2D.rgba(argb);
        float clampedRadius = clampRoundedRadius(radius, w, h);
        int i1 = roundedTexVertex(mesh, x, y, texX1, texY1, c, x, y, w, h, clampedRadius);
        int i2 = roundedTexVertex(mesh, x, y + h, texX1, texY2, c, x, y, w, h, clampedRadius);
        int i3 = roundedTexVertex(mesh, x + w, y + h, texX2, texY2, c, x, y, w, h, clampedRadius);
        int i4 = roundedTexVertex(mesh, x + w, y, texX2, texY1, c, x, y, w, h, clampedRadius);
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    private static int msdfVertex(MeshBuilder mesh, double px, double py, double u, double v, int argb) {
        int[] c = Batch2D.rgba(argb);
        return mesh.vec2(px, py).raw2(u, v).color(c[0], c[1], c[2], c[3]).next();
    }

    private static int roundedTexVertex(MeshBuilder mesh, double px, double py, double u, double v,
                                        int[] c, double rx, double ry, double w, double h, float radius) {
        return mesh.vec2(px, py).raw2(u, v).local2(px, py).color(c[0], c[1], c[2], c[3])
                .vec4(rx, ry, w, h).vec4(radius, 0.0f, 0f, 0f).next();
    }
}
