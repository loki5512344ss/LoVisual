package dev.loki.lovisual.render.engine.renderer.ui.runtime.style.parse;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.style.UiStyle;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.style.parse.UiStyleParser;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.style.theme.UiThemeRegistry;


import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public final class UiStyleCache {
    private final UiStyleParser parser = new UiStyleParser();
    private final Object2ObjectOpenHashMap<String, UiStyle> cache = new Object2ObjectOpenHashMap<>();
    private int hits;
    private int misses;

    public UiStyle resolve(String classString) {
        String key = (classString != null ? classString : "") + "|theme:" + UiThemeRegistry.current().version();
        UiStyle cached = cache.get(key);
        if (cached != null) {
            hits++;
            return cached;
        }
        misses++;
        UiStyle style = parser.parse(classString).style();
        cache.put(key, style);
        cache.trim();
        return style;
    }

    public int hits() {
        return hits;
    }

    public int misses() {
        return misses;
    }

    public void clear() {
        cache.clear();
        hits = 0;
        misses = 0;
    }
}
