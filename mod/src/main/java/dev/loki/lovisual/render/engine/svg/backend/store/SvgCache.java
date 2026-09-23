package dev.loki.lovisual.render.engine.svg.backend;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Кэши SVG — документы, геометрия, текстуры, trace.
 */
final class SvgCache {
    private SvgCache() {}
    static void clearAll() {
        synchronized (SvgMeshBackend.CACHE) { SvgMeshBackend.CACHE.clear(); }
        synchronized (SvgMeshBackend.TEXTURE_CACHE) {
            for (SvgMeshBackend.CompiledTexture t : SvgMeshBackend.TEXTURE_CACHE.values()) t.close();
            SvgMeshBackend.TEXTURE_CACHE.clear();
        }
        synchronized (SvgMeshBackend.GEOMETRY_CACHE) {
            for (SvgMeshBackend.CompiledGeometry g : SvgMeshBackend.GEOMETRY_CACHE.values()) g.close();
            SvgMeshBackend.GEOMETRY_CACHE.clear();
        }
        synchronized (SvgMeshBackend.TRACE_ONCE_KEYS) { SvgMeshBackend.TRACE_ONCE_KEYS.clear(); }
    }
}
