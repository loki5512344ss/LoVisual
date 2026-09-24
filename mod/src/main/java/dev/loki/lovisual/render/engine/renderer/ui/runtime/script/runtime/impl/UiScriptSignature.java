package dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime;

import java.util.Map;

/**
 * FNV-1a style hashing helpers used to fold prop maps into stable {@code long} signatures.
 *
 * <p>Backs the public {@link CachedUiScriptRuntime#signature} / {@code mix} API; callers build
 * tree/layout/data signatures from these without allocating.</p>
 */
final class UiScriptSignature {
    private UiScriptSignature() {
    }

    static long signature(Map<String, ?> values) {
        if (values == null || values.isEmpty()) return 0xcbf29ce484222325L;
        long h = 0xcbf29ce484222325L;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            long e = 0xcbf29ce484222325L;
            e = mix(e, entry.getKey());
            e = mixObject(e, entry.getValue());
            h ^= e;
            h *= 0x100000001b3L;
        }
        return h;
    }

    static long mix(long hash, boolean value) {
        return mix(hash, value ? 1 : 0);
    }

    static long mix(long hash, int value) {
        hash ^= value;
        return hash * 0x100000001b3L;
    }

    static long mix(long hash, float value) {
        return mix(hash, Float.floatToIntBits(value));
    }

    static long mix(long hash, String value) {
        return mix(hash, value != null ? value.hashCode() : 0);
    }

    static long mixObject(long hash, Object value) {
        if (value instanceof Boolean b) return mix(hash, b);
        if (value instanceof Integer i) return mix(hash, i);
        if (value instanceof Float f) return mix(hash, f);
        if (value instanceof Number n) return mix(hash, Float.floatToIntBits(n.floatValue()));
        return mix(hash, value != null ? value.toString() : "");
    }
}
