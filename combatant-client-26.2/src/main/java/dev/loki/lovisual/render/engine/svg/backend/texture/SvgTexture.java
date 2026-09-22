package dev.loki.lovisual.render.engine.svg.backend;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import dev.loki.lovisual.render.engine.texture.Texture;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.svg.model.SvgColorMode;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.engine.svg.model.SvgStyleUtil;
import dev.loki.lovisual.util.logging.DebugLog;

/**
 * Растеризация SVG → GPU texture. Вынесено из гиганта SvgMeshBackend.
 */
final class SvgTexture {
    private SvgTexture() {}

    static @Nullable SvgMeshBackend.CompiledTexture getOrRasterize(SvgMeshBackend.SvgDoc doc, double width, double height, SvgRenderOptions options) {
        double logicalW = Math.abs(width);
        double logicalH = Math.abs(height);
        if (logicalW <= SvgMeshBackend.EPS || logicalH <= SvgMeshBackend.EPS) return null;
        SvgMeshBackend.PhysicalSize physical = physicalDrawSize(logicalW, logicalH);
        int scale = rasterScaleFor(physical.width(), physical.height(), options.rasterScale());
        int texW = quantizeTextureDimension(Math.max(1, (int) Math.ceil(physical.width() * scale)));
        int texH = quantizeTextureDimension(Math.max(1, (int) Math.ceil(physical.height() * scale)));
        if (texW <= 0 || texH <= 0) return null;
        int gradientAngle1024 = options.colorMode() == SvgColorMode.GRADIENT_LINEAR ? quantize(options.gradientAngleDegrees(), 1024.0) : 0;
        int alpha1024 = quantize(options.alpha(), 1024.0);
        SvgMeshBackend.TextureKey key = new SvgMeshBackend.TextureKey(doc.cacheKey(), texW, texH, options.colorMode(), scale, options.colorMode() == SvgColorMode.GRADIENT_LINEAR ? options.gradientStartArgb() : 0, options.colorMode() == SvgColorMode.GRADIENT_LINEAR ? options.gradientEndArgb() : 0, gradientAngle1024, options.colorMode() == SvgColorMode.GRADIENT_LINEAR ? alpha1024 : 0);
        synchronized (SvgMeshBackend.TEXTURE_CACHE) {
            SvgMeshBackend.CompiledTexture cached = SvgMeshBackend.TEXTURE_CACHE.get(key);
            if (cached != null) {
                SvgMeshBackend.traceOnce("svg-texture-cache-hit:" + key, "[LoVisual][SVG] Texture cache hit: doc=%s tex=%dx%d scale=%d", doc.cacheKey(), texW, texH, scale);
                return cached;
            }
        }
        SvgMeshBackend.traceOnce("svg-texture-rasterize:" + key, "[LoVisual][SVG] Rasterizing AA texture: doc=%s tex=%dx%d logical=%.2fx%.2f physical=%.2fx%.2f scale=%d", doc.cacheKey(), texW, texH, logicalW, logicalH, physical.width(), physical.height(), scale);
        SvgMeshBackend.CompiledTexture compiled = rasterizeTexture(doc, texW, texH, options, scale);
        if (compiled == null) return null;
        synchronized (SvgMeshBackend.TEXTURE_CACHE) {
            SvgMeshBackend.CompiledTexture raced = SvgMeshBackend.TEXTURE_CACHE.get(key);
            if (raced != null) { compiled.close(); return raced; }
            SvgMeshBackend.TEXTURE_CACHE.put(key, compiled);
        }
        return compiled;
    }

    static SvgMeshBackend.PhysicalSize physicalDrawSize(double logicalW, double logicalH) {
        ViewportContext viewport = ViewportContext.current();
        if (viewport == null || viewport.width() <= SvgMeshBackend.EPS || viewport.height() <= SvgMeshBackend.EPS) return new SvgMeshBackend.PhysicalSize(logicalW, logicalH);
        double scaleX = viewport.framebufferWidth() / Math.max(SvgMeshBackend.EPS, Math.abs(viewport.width()));
        double scaleY = viewport.framebufferHeight() / Math.max(SvgMeshBackend.EPS, Math.abs(viewport.height()));
        if (!Double.isFinite(scaleX) || scaleX <= SvgMeshBackend.EPS) scaleX = 1.0;
        if (!Double.isFinite(scaleY) || scaleY <= SvgMeshBackend.EPS) scaleY = 1.0;
        return new SvgMeshBackend.PhysicalSize(logicalW * scaleX, logicalH * scaleY);
    }

    static int rasterScaleFor(double width, double height, int requestedScale) {
        int scale = Math.max(1, Math.min(4, requestedScale));
        while (scale > 1) {
            int texW = quantizeTextureDimension(Math.max(1, (int) Math.ceil(width * scale)));
            int texH = quantizeTextureDimension(Math.max(1, (int) Math.ceil(height * scale)));
            long pixels = (long) texW * (long) texH;
            if (texW <= SvgMeshBackend.MAX_TEXTURE_SIDE && texH <= SvgMeshBackend.MAX_TEXTURE_SIDE && pixels <= SvgMeshBackend.MAX_TEXTURE_PIXELS) return scale;
            scale--;
        }
        return 1;
    }

    static int quantizeTextureDimension(int pixels) {
        if (pixels <= 1) return 1;
        int quantum = pixels <= 128 ? SvgMeshBackend.SMALL_TEXTURE_QUANTUM : pixels <= 512 ? SvgMeshBackend.MEDIUM_TEXTURE_QUANTUM : SvgMeshBackend.LARGE_TEXTURE_QUANTUM;
        return ((pixels + quantum - 1) / quantum) * quantum;
    }

    static @Nullable SvgMeshBackend.CompiledTexture rasterizeTexture(SvgMeshBackend.SvgDoc doc, int texW, int texH, SvgRenderOptions options, int scale) {
        try {
            BufferedImage image = new BufferedImage(texW, texH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            try {
                g.setComposite(AlphaComposite.SrcOver);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                AffineTransform view = new AffineTransform();
                view.scale(texW / doc.vw(), texH / doc.vh());
                view.translate(-doc.vx(), -doc.vy());
                SvgRenderOptions rasterOptions = options.withAlpha(1.0f);
                boolean overrideMask = options.colorMode() == SvgColorMode.OVERRIDE;
                for (SvgMeshBackend.SvgShape shape : doc.shapes()) {
                    AffineTransform tx = new AffineTransform(view);
                    tx.concatenate(shape.transform());
                    paintShape(g, shape, tx, rasterOptions, overrideMask, texW, texH);
                }
            } finally { g.dispose(); }
            Texture texture = uploadTexture(image);
            if (texture != null) SvgMeshBackend.traceOnce("svg-texture-upload:" + doc.cacheKey() + ':' + texW + 'x' + texH + ':' + scale, "[LoVisual][SVG] Uploaded GPU texture: doc=%s tex=%dx%d scale=%d", doc.cacheKey(), texW, texH, scale);
            return texture == null ? null : new SvgMeshBackend.CompiledTexture(texture, texW, texH, scale);
        } catch (Throwable t) {
            DebugLog.warn("[LoVisual][SVG] Texture rasterization failed: %s", t.getMessage());
            return null;
        }
    }

    static void paintShape(Graphics2D g, SvgMeshBackend.SvgShape shape, AffineTransform tx, SvgRenderOptions options, boolean overrideMask, int viewportW, int viewportH) {
        Shape transformed = tx.createTransformedShape(shape.path());
        boolean gradient = options.colorMode() == SvgColorMode.GRADIENT_LINEAR;
        int fill = overrideMask || gradient ? resolveMaskPaint(shape.style().fill, shape.style().fillOpacity, shape.style().groupOpacity, options.alpha()) : SvgStyleUtil.resolvePaint(shape.style().fill, shape.style().fillOpacity, shape.style().groupOpacity, options);
        if (((fill >>> 24) & 0xFF) > 0) {
            if (gradient) g.setPaint(linearGradientPaint(options, viewportW, viewportH, fill));
            else g.setColor(new Color(fill, true));
            g.fill(transformed);
        }
        int stroke = overrideMask || gradient ? resolveMaskPaint(shape.style().stroke, shape.style().strokeOpacity, shape.style().groupOpacity, options.alpha()) : SvgStyleUtil.resolvePaint(shape.style().stroke, shape.style().strokeOpacity, shape.style().groupOpacity, options);
        if (((stroke >>> 24) & 0xFF) <= 0) return;
        double scale = SvgGeometry.estimateScale(tx);
        float strokeWidth = Math.max(0.0f, (float) (shape.style().strokeWidth * scale));
        if (strokeWidth <= 0.0f) return;
        BasicStroke bs = new BasicStroke(strokeWidth, shape.style().lineCap, shape.style().lineJoin, shape.style().miterLimit);
        Shape stroked = bs.createStrokedShape(transformed);
        if (gradient) g.setPaint(linearGradientPaint(options, viewportW, viewportH, stroke));
        else g.setColor(new Color(stroke, true));
        g.fill(stroked);
    }

    static int resolveMaskPaint(Integer paint, float channelOpacity, float groupOpacity, float alpha) {
        if (paint == null) return 0;
        int baseA = (paint >>> 24) & 0xFF;
        int outAlpha = SvgMeshBackend.clamp255(Math.round(baseA * SvgMeshBackend.clamp01(channelOpacity) * SvgMeshBackend.clamp01(groupOpacity) * SvgMeshBackend.clamp01(alpha)));
        return (outAlpha << 24) | 0x00FFFFFF;
    }

    static Paint linearGradientPaint(SvgRenderOptions options, int viewportW, int viewportH, int alphaMask) {
        float w = Math.max(1f, viewportW); float h = Math.max(1f, viewportH);
        double angle = Math.toRadians(options.gradientAngleDegrees());
        float dx = (float) Math.cos(angle); float dy = (float) Math.sin(angle);
        float extent = Math.max(1f, Math.abs(dx) * w + Math.abs(dy) * h);
        float cx = w * 0.5f; float cy = h * 0.5f;
        Color start = gradientColor(options.gradientStartArgb(), alphaMask);
        Color end = gradientColor(options.gradientEndArgb(), alphaMask);
        return new LinearGradientPaint(cx - dx * extent * 0.5f, cy - dy * extent * 0.5f, cx + dx * extent * 0.5f, cy + dy * extent * 0.5f, new float[]{0f, 1f}, new Color[]{start, end});
    }

    static Color gradientColor(int argb, int alphaMask) {
        int maskA = (alphaMask >>> 24) & 0xFF; int colorA = (argb >>> 24) & 0xFF;
        int a = SvgMeshBackend.clamp255(Math.round(maskA * (colorA / 255.0f)));
        return new Color((argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, a);
    }

    static int textureTint(SvgRenderOptions options) {
        if (options.colorMode() == SvgColorMode.OVERRIDE) {
            int override = options.overrideArgb();
            int alpha = SvgMeshBackend.clamp255(Math.round(((override >>> 24) & 0xFF) * SvgMeshBackend.clamp01(options.alpha())));
            return (alpha << 24) | (override & 0x00FFFFFF);
        }
        int alpha = SvgMeshBackend.clamp255(Math.round(255.0f * SvgMeshBackend.clamp01(options.alpha())));
        return (alpha << 24) | 0x00FFFFFF;
    }

    static @Nullable Texture uploadTexture(BufferedImage image) {
        int w = image.getWidth(); int h = image.getHeight();
        int[] pixels = new int[w * h];
        image.getRGB(0, 0, w, h, pixels, 0, w);
        bleedTransparentRgb(pixels, w, h);
        ByteBuffer buffer = MemoryUtil.memAlloc(w * h * 4);
        Texture texture = null;
        try {
            for (int argb : pixels) {
                buffer.put((byte) ((argb >>> 16) & 0xFF));
                buffer.put((byte) ((argb >>> 8) & 0xFF));
                buffer.put((byte) (argb & 0xFF));
                buffer.put((byte) ((argb >>> 24) & 0xFF));
            }
            buffer.flip();
            texture = new Texture(w, h, GpuFormat.RGBA8_UNORM, FilterMode.LINEAR, FilterMode.LINEAR, AddressMode.CLAMP_TO_EDGE);
            texture.upload(buffer);
            return texture;
        } catch (Throwable t) {
            if (texture != null) try { texture.close(); } catch (Throwable ignored) {}
            DebugLog.warn("[LoVisual][SVG] Texture upload failed: %s", t.getMessage());
            return null;
        } finally { MemoryUtil.memFree(buffer); }
    }

    static void bleedTransparentRgb(int[] pixels, int width, int height) {
        if (pixels == null || width <= 0 || height <= 0) return;
        for (int pass = 0; pass < 3; pass++) {
            int[] source = pixels.clone(); boolean changed = false;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = y * width + x;
                    if (((source[index] >>> 24) & 0xFF) != 0) continue;
                    int r = 0, g = 0, b = 0, count = 0;
                    for (int oy = -1; oy <= 1; oy++) {
                        int ny = y + oy; if (ny < 0 || ny >= height) continue;
                        for (int ox = -1; ox <= 1; ox++) {
                            if (ox == 0 && oy == 0) continue;
                            int nx = x + ox; if (nx < 0 || nx >= width) continue;
                            int neighbor = source[ny * width + nx];
                            int a = (neighbor >>> 24) & 0xFF; if (a == 0) continue;
                            r += (neighbor >>> 16) & 0xFF; g += (neighbor >>> 8) & 0xFF; b += neighbor & 0xFF; count++;
                        }
                    }
                    if (count > 0) { pixels[index] = ((r / count) << 16) | ((g / count) << 8) | (b / count); changed = true; }
                }
            }
            if (!changed) return;
        }
    }

    static int quantize(double v, double s) { return (int) Math.round(v * s); }
}
