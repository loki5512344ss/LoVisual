package dev.loki.lovisual.render.engine.rhi.resource.manager;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Lightweight debug ownership tracker. It intentionally does not throw in production paths.
 */
public final class ResourceLeakTracker {
    private final Set<Object> live = Collections.newSetFromMap(new IdentityHashMap<>());

    public void register(Object resource) {
        if (resource != null) live.add(resource);
    }

    public void release(Object resource) {
        if (resource != null) live.remove(resource);
    }

    public int liveCount() {
        return live.size();
    }

    public void clear() {
        live.clear();
    }
}
