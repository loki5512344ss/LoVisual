package dev.loki.lovisual.render.engine.renderer.facade.shape;

import dev.loki.lovisual.render.engine.renderer.facade.Renderer2DPath;
import dev.loki.lovisual.render.engine.renderer.renderer2d.path.Circles2D;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiMeshGeometry.*;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.renderer2d.Rects2D;
import dev.loki.lovisual.render.engine.renderer.renderer2d.path.Lines2D;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import dev.loki.lovisual.render.engine.renderer.renderer2d.media.Textures2D;
import dev.loki.lovisual.render.engine.svg.backend.SvgMeshBackend;
import dev.loki.lovisual.render.engine.svg.parser.SvgRegistry;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import java.nio.file.Path;

/** Circle, line/quad and texture primitives of the Renderer2D facade. */
public class Renderer2DShapes extends Renderer2DPath {

    protected Renderer2DShapes(boolean textured) {
        super(textured);
    }

    public void circle(double cx, double cy, double radius, int argb) {
        circle(cx, cy, radius, 1.1f, argb);
    }

    public void circle(double cx, double cy, double radius, float softness, int argb) {
        Circles2D.circle(self(), cx, cy, radius, softness, argb);
    }

    public void circleSoftShadow(double cx, double cy, double radius, float blur, float innerAlpha, int argb) {
        if (radius <= 0.0) return;
        double size = radius * 2.0;
        roundedRectSoftShadow(cx - radius, cy - radius, size, size, (float) radius, blur, innerAlpha, argb);
    }

    public void circleStroke(double cx, double cy, double radius, double thickness, int argb) {
        circleStroke(cx, cy, radius, thickness, 1.1f, argb);
    }

    public void circleStroke(double cx, double cy, double radius, double thickness, float softness, int argb) {
        Circles2D.circleStroke(self(), cx, cy, radius, thickness, softness, argb);
    }

    public void arcStroke(double cx, double cy, double radius, double thickness,
                          float startAngleDeg, float endAngleDeg, int argb) {
        arcStroke(cx, cy, radius, thickness, startAngleDeg, endAngleDeg, 1.1f, argb);
    }

    public void arcStroke(double cx, double cy, double radius, double thickness,
                          float startAngleDeg, float endAngleDeg, float softness, int argb) {
        arcStrokeQuad(cx, cy, radius, thickness, startAngleDeg, endAngleDeg, softness, true,
                argb, argb, argb, argb);
    }

    public void arcStrokeFlat(double cx, double cy, double radius, double thickness,
                              float startAngleDeg, float endAngleDeg, float softness, int argb) {
        arcStrokeQuad(cx, cy, radius, thickness, startAngleDeg, endAngleDeg, softness, false,
                argb, argb, argb, argb);
    }

    public void arcStrokeFlat(double cx, double cy, double radius, double thickness,
                              float startAngleDeg, float endAngleDeg, int argb) {
        arcStrokeFlat(cx, cy, radius, thickness, startAngleDeg, endAngleDeg, 0.0f, argb);
    }

    public void arcStrokeGradient(double cx, double cy, double radius, double thickness,
                                  float startAngleDeg, float endAngleDeg, float softness,
                                  int startArgb, int endArgb, float angleDeg) {
        arcStrokeGradient(cx, cy, radius, thickness, startAngleDeg, endAngleDeg, softness,
                startArgb, endArgb, angleDeg, 0.0f);
    }

    public void arcStrokeGradient(double cx, double cy, double radius, double thickness,
                                  float startAngleDeg, float endAngleDeg,
                                  int startArgb, int endArgb, float angleDeg, float offsetPx) {
        arcStrokeGradient(cx, cy, radius, thickness, startAngleDeg, endAngleDeg, 0.0f,
                startArgb, endArgb, angleDeg, offsetPx);
    }

    public void arcStrokeGradient(double cx, double cy, double radius, double thickness,
                                  float startAngleDeg, float endAngleDeg, float softness,
                                  int startArgb, int endArgb, float angleDeg, float offsetPx) {
        double outerRadius = radius + Math.max(0.0, thickness) * 0.5 + softness;
        computeLinearGradientColors((float) (outerRadius * 2.0), (float) (outerRadius * 2.0),
                startArgb, endArgb, angleDeg, offsetPx, gradientTmp);
        arcStrokeQuad(cx, cy, radius, thickness, startAngleDeg, endAngleDeg, softness, true,
                gradientTmp[0], gradientTmp[1], gradientTmp[2], gradientTmp[3]);
    }

    public void arcStrokeQuad(double cx, double cy, double radius, double thickness,
                              float startAngleDeg, float endAngleDeg, float softness, boolean caps,
                              int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        Circles2D.arcStrokeQuad(self(), cx, cy, radius, thickness, startAngleDeg, endAngleDeg, softness, caps,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void orbizRing(double cx, double cy,
                         double radius, double thickness,
                         float startAngleDeg, float sweepAngleDeg,
                         float softness, float glowRadius, float glowStrength,
                         int primaryArgb, int secondaryArgb,
                         int colorMode, float angularOffset, float angularCycles,
                         float trackAlpha, float bodyAlpha, float headBoost,
                         float trackGlow, boolean caps) {
        Circles2D.orbizRing(self(), cx, cy, radius, thickness, startAngleDeg, sweepAngleDeg,
                softness, glowRadius, glowStrength, primaryArgb, secondaryArgb,
                colorMode, angularOffset, angularCycles, trackAlpha, bodyAlpha, headBoost, trackGlow, caps);
    }

    public void line(double x1, double y1, double x2, double y2, int argb) {
        Lines2D.line(self(), x1, y1, x2, y2, argb);
    }

    public void line(double x1, double y1, double x2, double y2, RenderColor color) {
        Lines2D.line(self(), x1, y1, x2, y2, color);
    }

    public void boxLines(double x, double y, double width, double height, int argb) {
        Lines2D.boxLines(self(), x, y, width, height, argb);
    }

    public void quad(double x, double y, double width, double height, int argb) {
        quad(x, y, width, height, argb, argb, argb, argb);
    }

    public void quad(double x, double y, double width, double height, int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        Rects2D.quadCorners(self(), x, y, width, height, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void quadGradient(double x, double y, double width, double height,
                             int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        quad(x, y, width, height, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void quadGradientLinear(double x, double y, double width, double height,
                                   int startArgb, int endArgb, float angleDeg, float offsetPx) {
        computeLinearGradientColors((float) width, (float) height, startArgb, endArgb, angleDeg, offsetPx, gradientTmp);
        quad(x, y, width, height, gradientTmp[0], gradientTmp[1], gradientTmp[2], gradientTmp[3]);
    }

    public void quadStrokeGradientLinear(double x, double y, double width, double height,
                                         float thickness,
                                         int startArgb, int endArgb, float angleDeg, float offsetPx) {
        roundedRectStrokeGradient(x, y, width, height, 0.0f, 0.0f, thickness, startArgb, endArgb, angleDeg, offsetPx);
    }

    public void svg(String svgName,
                    double x,
                    double y,
                    double width,
                    double height,
                    SvgRenderOptions options) {
        if (textured) {
            throw new IllegalStateException("SVG mesh drawing is supported only on Renderer2D.COLOR.");
        }
        Identifier id = SvgRegistry.resolve(svgName);
        if (id == null) return;
        SvgMeshBackend.draw(self(), id, x, y, width, height, options);
    }

    public void svg(Identifier svgId,
                    double x,
                    double y,
                    double width,
                    double height,
                    SvgRenderOptions options) {
        if (textured) {
            throw new IllegalStateException("SVG mesh drawing is supported only on Renderer2D.COLOR.");
        }
        SvgMeshBackend.draw(self(), svgId, x, y, width, height, options);
    }

    public void svg(Path svgPath,
                    double x,
                    double y,
                    double width,
                    double height,
                    SvgRenderOptions options) {
        if (textured) {
            throw new IllegalStateException("SVG mesh drawing is supported only on Renderer2D.COLOR.");
        }
        SvgMeshBackend.draw(self(), svgPath, x, y, width, height, options);
    }

    public void textureQuad(GpuTextureView samplerView,
                            GpuSampler sampler,
                            double x,
                            double y,
                            double w,
                            double h,
                            int argb) {
        Textures2D.textureQuad(self(), samplerView, sampler, x, y, w, h, argb);
    }

    public void msdfTextureQuad(GpuTextureView samplerView,
                                GpuSampler sampler,
                                double x,
                                double y,
                                double w,
                                double h,
                                int argb,
                                float pxRange,
                                int atlasWidth,
                                int atlasHeight) {
        msdfTextureQuad(samplerView, sampler, x, y, w, h, argb, argb, argb, argb, pxRange, atlasWidth, atlasHeight);
    }

    public void msdfTextureQuad(GpuTextureView samplerView,
                                GpuSampler sampler,
                                double x,
                                double y,
                                double w,
                                double h,
                                int topLeft,
                                int topRight,
                                int bottomRight,
                                int bottomLeft,
                                float pxRange,
                                int atlasWidth,
                                int atlasHeight) {
        Textures2D.msdfTextureQuad(self(), samplerView, sampler, x, y, w, h,
                topLeft, topRight, bottomRight, bottomLeft, pxRange, atlasWidth, atlasHeight);
    }

    public void roundedTexRect(double x, double y, double w, double h,
                               float radius, float softness,
                               double texX1, double texY1, double texX2, double texY2,
                               int argb, GpuTextureView samplerView, GpuSampler sampler) {
        Textures2D.roundedTexRect(self(), x, y, w, h, radius, softness, texX1, texY1, texX2, texY2,
                argb, samplerView, sampler);
    }

    public void roundedTexMaskRect(double x, double y, double w, double h,
                                   float radius, float softness,
                                   double texX1, double texY1, double texX2, double texY2,
                                   int argb, GpuTextureView samplerView, GpuSampler sampler) {
        Textures2D.roundedTexMaskRect(self(), x, y, w, h, radius, softness, texX1, texY1, texX2, texY2,
                argb, samplerView, sampler);
    }

    public void roundedTexRect(double x, double y, double w, double h,
                               float radius, float softness, int argb,
                               Identifier textureId) {
        if (!textured) {
            throw new IllegalStateException("Renderer2D is not configured for textures.");
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || textureId == null) return;
        AbstractTexture tex = mc.getTextureManager().getTexture(textureId);
        if (tex == null) return;
        GpuTextureView view = tex.getTextureView();
        GpuSampler sampler = tex.getSampler();
        if (view == null || sampler == null) return;
        roundedTexRect(x, y, w, h, radius, 0.0f, 0, 0, 1, 1, argb, view, sampler);
    }

    public void roundedTexRect(double x, double y, double w, double h,
                               float radius, int argb,
                               Identifier textureId) {
        roundedTexRect(x, y, w, h, radius, 0.0f, argb, textureId);
    }

    public void roundedTexMaskRect(double x, double y, double w, double h,
                                   float radius, float softness, int argb,
                                   Identifier textureId) {
        if (!textured) {
            throw new IllegalStateException("Renderer2D is not configured for textures.");
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || textureId == null) return;
        AbstractTexture tex = mc.getTextureManager().getTexture(textureId);
        if (tex == null) return;
        GpuTextureView view = tex.getTextureView();
        GpuSampler sampler = tex.getSampler();
        if (view == null || sampler == null) return;
        roundedTexMaskRect(x, y, w, h, radius, 0.0f, 0, 0, 1, 1, argb, view, sampler);
    }

    public void roundedTexMaskRect(double x, double y, double w, double h,
                                   float radius, int argb,
                                   Identifier textureId) {
        roundedTexMaskRect(x, y, w, h, radius, 0.0f, argb, textureId);
    }

    public void roundedTexRect(double x, double y, double w, double h,
                               float radius, float softness,
                               double texX1, double texY1, double texX2, double texY2,
                               int argb, Identifier textureId) {
        if (!textured) {
            throw new IllegalStateException("Renderer2D is not configured for textures.");
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || textureId == null) return;
        AbstractTexture tex = mc.getTextureManager().getTexture(textureId);
        if (tex == null) return;
        GpuTextureView view = tex.getTextureView();
        GpuSampler sampler = tex.getSampler();
        if (view == null || sampler == null) return;
        roundedTexRect(x, y, w, h, radius, 0.0f, texX1, texY1, texX2, texY2, argb, view, sampler);
    }

    public void texQuad(double x, double y, double width, double height,
                        double texX1, double texY1, double texX2, double texY2,
                        int argb) {
        Textures2D.texQuad(self(), x, y, width, height, texX1, texY1, texX2, texY2, argb);
    }

}
