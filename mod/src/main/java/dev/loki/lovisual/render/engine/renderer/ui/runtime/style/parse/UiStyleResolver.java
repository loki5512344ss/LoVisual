package dev.loki.lovisual.render.engine.renderer.ui.runtime.style.parse;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.style.UiStyle;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.style.parse.UiStyleCache;


import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNodeSpec;

public final class UiStyleResolver {
    private final UiStyleCache cache;

    public UiStyleResolver(UiStyleCache cache) {
        this.cache = cache != null ? cache : new UiStyleCache();
    }

    public UiStyle resolve(UiNodeSpec spec) {
        if (spec == null) return UiStyle.DEFAULT;
        if (!spec.styleClass().isBlank()) {
            return cache.resolve(spec.styleClass());
        }
        return spec.style();
    }
}
