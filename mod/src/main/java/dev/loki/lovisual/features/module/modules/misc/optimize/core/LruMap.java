package dev.loki.lovisual.features.module.modules.misc.optimize.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Access-ordered LRU map with a size cap. Generic, no Minecraft dependencies,
 * unit-testable as-is.
 *
 * <p>Intended for the {@code ClientLanguage.getVisualOrder} LRU cache.
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class LruMap<K, V> extends LinkedHashMap<K, V> {

    private final int maxSize;

    public LruMap(int maxSize) {
        super(256, 0.75f, true);
        this.maxSize = maxSize;
    }

    public int maxSize() {
        return maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}
