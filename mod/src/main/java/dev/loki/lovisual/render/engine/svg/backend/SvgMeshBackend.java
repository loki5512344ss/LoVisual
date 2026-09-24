package dev.loki.lovisual.render.engine.svg.backend;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import dev.loki.lovisual.render.engine.texture.Texture;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.util.logging.DebugLog;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import dev.loki.lovisual.render.engine.svg.model.SvgColorMode;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.engine.svg.model.SvgStyle;
import dev.loki.lovisual.render.engine.svg.model.SvgStyleUtil;
import dev.loki.lovisual.render.engine.svg.parser.SvgMsdfRegistry;
import dev.loki.lovisual.render.engine.svg.parser.SvgPathParser;

public enum SvgMeshBackend {
    ;
    private static final int MAX_CACHE = 96;
    static final int MAX_TEXTURE_CACHE = 256;
    static final int MAX_GEOMETRY_CACHE = 256;
    static final int MAX_TEXTURE_SIDE = 4096;
    static final int MAX_TEXTURE_PIXELS = 4 * 1024 * 1024;
    static final int SMALL_TEXTURE_QUANTUM = 8;
    static final int MEDIUM_TEXTURE_QUANTUM = 16;
    static final int LARGE_TEXTURE_QUANTUM = 32;
    static final double EPS = 1e-7;
    static final int MAX_TRACE_KEYS = 512;
    static final Set<String> TRACE_ONCE_KEYS = new HashSet<>();
    static final Map<String, SvgDoc> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SvgDoc> eldest) {
            return size() > MAX_CACHE;
        }
    };
    static final Map<GeometryKey, CompiledGeometry> GEOMETRY_CACHE = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<GeometryKey, CompiledGeometry> eldest) {
            if (size() > MAX_GEOMETRY_CACHE) {
                eldest.getValue().close();
                return true;
            }
            return false;
        }
    };
    static final Map<TextureKey, CompiledTexture> TEXTURE_CACHE = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<TextureKey, CompiledTexture> eldest) {
            if (size() > MAX_TEXTURE_CACHE) {
                eldest.getValue().close();
                return true;
            }
            return false;
        }
    };

    public static void clearCache() {
        SvgCache.clearAll();
    }

    public static void draw(Renderer2D renderer, Identifier svg, double x, double y, double w, double h) {
        draw(renderer, svg, x, y, w, h, SvgRenderOptions.DEFAULT);
    }

    public static void draw(Renderer2D renderer, Identifier svg, double x, double y, double w, double h, SvgRenderOptions options) {
        if (renderer == null || svg == null || w == 0.0 || h == 0.0) return;
        SvgRenderOptions resolved = options == null ? SvgRenderOptions.DEFAULT : options;
        if (SvgMsdfRegistry.draw(renderer, svg, x, y, w, h, resolved)) return;
        SvgDoc doc = loadFromResource(svg);
        if (doc == null) return;
        render(renderer, doc, x, y, w, h, resolved);
    }

    public static void draw(Renderer2D renderer, Path svgPath, double x, double y, double w, double h) {
        draw(renderer, svgPath, x, y, w, h, SvgRenderOptions.DEFAULT);
    }

    public static void draw(Renderer2D renderer, Path svgPath, double x, double y, double w, double h, SvgRenderOptions options) {
        if (renderer == null || svgPath == null || w == 0.0 || h == 0.0) return;
        SvgDoc doc = loadFromFile(svgPath);
        if (doc == null) return;
        render(renderer, doc, x, y, w, h, options == null ? SvgRenderOptions.DEFAULT : options);
    }

    private static @Nullable SvgDoc loadFromResource(Identifier id) {
        String key = "res:" + id;
        synchronized (CACHE) {
            SvgDoc cached = CACHE.get(key);
            if (cached != null) return cached;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;
        var opt = mc.getResourceManager().getResource(id);
        if (opt.isEmpty()) return null;
        try (InputStream in = opt.get().open()) {
            SvgDoc parsed = SvgParser.parseSvg(in, key);
            if (parsed == null) return null;
            synchronized (CACHE) {
                CACHE.put(key, parsed);
            }
            return parsed;
        } catch (Exception e) {
            DebugLog.warn("[LoVisual][SVG] Resource load failed %s : %s", id, e.getMessage());
            return null;
        }
    }

    private static @Nullable SvgDoc loadFromFile(Path path) {
        String key = "file:" + path.toAbsolutePath();
        synchronized (CACHE) {
            SvgDoc cached = CACHE.get(key);
            if (cached != null) return cached;
        }
        try (InputStream in = Files.newInputStream(path)) {
            SvgDoc parsed = SvgParser.parseSvg(in, key);
            if (parsed == null) return null;
            synchronized (CACHE) {
                CACHE.put(key, parsed);
            }
            return parsed;
        } catch (IOException e) {
            DebugLog.warn("[LoVisual][SVG] File load failed %s : %s", path, e.getMessage());
            return null;
        }
    }

    private static void render(Renderer2D renderer, SvgDoc doc, double x, double y, double width, double height, SvgRenderOptions options) {
        SvgRender.render(renderer, doc, x, y, width, height, options);
    }

    static int quantize(double value, double scale) {
        return (int) Math.round(value * scale);
    }

    static int mixArgb(int a, int b, float t) {
        float k = clamp01(t);
        int aa = (a >>> 24) & 0xFF; int ar = (a >>> 16) & 0xFF; int ag = (a >>> 8) & 0xFF; int ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF; int br = (b >>> 16) & 0xFF; int bg = (b >>> 8) & 0xFF; int bb = b & 0xFF;
        int oa = Math.round(aa + (ba - aa) * k); int or = Math.round(ar + (br - ar) * k);
        int og = Math.round(ag + (bg - ag) * k); int ob = Math.round(ab + (bb - ab) * k);
        return (clamp255(oa) << 24) | (clamp255(or) << 16) | (clamp255(og) << 8) | clamp255(ob);
    }

    static float clamp01(float v) { if (v <= 0.0f) return 0.0f; if (v >= 1.0f) return 1.0f; return v; }
    static int clamp255(int v) { if (v <= 0) return 0; if (v >= 255) return 255; return v; }

    static void traceOnce(String key, String message, Object... args) {
        if (!DebugLog.isEnabled()) return;

        synchronized (TRACE_ONCE_KEYS) {
            if (TRACE_ONCE_KEYS.size() >= MAX_TRACE_KEYS) {
                TRACE_ONCE_KEYS.clear();
            }
            if (!TRACE_ONCE_KEYS.add(key)) {
                return;
            }
        }

        DebugLog.renderThread(message, args);
    }

    @FunctionalInterface
    interface QuadSink {
        public void quad(double x, double y, double w, double h, int argb);
    }

    record SvgDoc(String cacheKey, double vx, double vy, double vw, double vh, List<SvgShape> shapes) {
    }

    record SvgShape(Path2D.Double path, AffineTransform transform, SvgStyle style, int windingRule) {
    }

    record Edge(double x1, double y1, double x2, double y2, int wind) {
    }

    record Crossing(double x, int wind) {
    }

    record Bounds(double minY, double maxY) {
    }

    record PhysicalSize(double width, double height) {
    }

    record TextureKey(String cacheKey,
                              int widthPx,
                              int heightPx,
                              SvgColorMode colorMode,
                              int rasterScale,
                              int gradientStartArgb,
                              int gradientEndArgb,
                              int gradientAngleQ1024,
                              int alphaQ1024) {
    }

    record GeometryKey(String cacheKey,
                               int widthQ16,
                               int heightQ16,
                               SvgColorMode colorMode,
                               int overrideArgb,
                               int gradientStartArgb,
                               int gradientEndArgb,
                               int gradientAngleQ1024,
                               int alphaQ1024,
                               int flatnessQ1024) {
    }

    record QuadCmd(double x, double y, double w, double h, int argb) {
    }

    record CompiledTexture(Texture texture, int widthPx, int heightPx, int scale) {

        boolean isReady() {
            return texture != null && texture.isReady();
        }

        void close() {
            if (texture != null) {
                try {
                    texture.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    static final class CompiledGeometry {
        final List<QuadCmd> quads;
        int hits;

        CompiledGeometry(List<QuadCmd> quads) {
            this.quads = quads;
        }

        void close() {
        }
    }
}
