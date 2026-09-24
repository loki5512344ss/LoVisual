package dev.loki.lovisual.render.engine.renderer.ui.runtime.core;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNodeSpec;


import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record UiDocument(String id, int version, UiNodeSpec root, Map<String, ?> metadata) {
    public UiDocument(String id, int version, UiNodeSpec root, Map<String, ?> metadata) {
        this.id = id != null ? id : "";
        this.version = Math.max(1, version);
        this.root = root != null ? root : UiNodeSpec.root();
        if (metadata == null || metadata.isEmpty()) {
            this.metadata = Map.of();
        } else {
            this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
        }
    }

    public static UiDocument of(String id, UiNodeSpec root) {
        return new UiDocument(id, 1, root, Map.of());
    }
}
