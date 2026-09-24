package dev.loki.lovisual.render.engine.renderer.ui.runtime.core.component;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.component.UiComponentFactory;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNodeSpec;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiProps;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.UiRuntimeException;


import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class UiComponentRegistry {
    private final Map<String, UiComponentFactory> factories = new LinkedHashMap<>();

    public void register(String id, UiComponentFactory factory) {
        if (id == null || id.isBlank()) {
            throw new UiRuntimeException("Component id is blank.");
        }
        if (factory == null) {
            throw new UiRuntimeException("Component factory is null: " + id);
        }
        factories.put(id, factory);
    }

    public UiComponentFactory get(String id) {
        return factories.get(id);
    }

    public UiNodeSpec create(String id, UiProps props) {
        UiComponentFactory factory = get(id);
        if (factory == null) {
            throw new UiRuntimeException("Unknown UI component: " + id);
        }
        return factory.create(props != null ? props : UiProps.EMPTY);
    }

    public Map<String, UiComponentFactory> entries() {
        return Collections.unmodifiableMap(factories);
    }
}
