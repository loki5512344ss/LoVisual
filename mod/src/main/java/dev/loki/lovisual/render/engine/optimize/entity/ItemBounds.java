package dev.loki.lovisual.render.engine.optimize.entity;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3fc;

/**
 * Stage 3 step 5: shared model bounds for single-layer items. Render states
 * are rebuilt every frame, so a cache stored on the state itself dies with
 * it; the extents supplier identity is stable per baked model and works as
 * a long-lived key here.
 */
public enum ItemBounds {
    ;

    private static final Map<Supplier<Vector3fc[]>, Entry> BOUNDS = new IdentityHashMap<>();

    private static volatile boolean enabled;

    public static void configure(boolean on) {
        enabled = on;
    }

    public static void reset() {
        enabled = false;
        BOUNDS.clear();
    }

    public static boolean enabled() {
        return enabled;
    }

    public static AABB get(Supplier<Vector3fc[]> extents, ItemTransform transform, Supplier<AABB> compute) {
        Entry e = BOUNDS.get(extents);
        if (e != null && e.transform() == transform) {
            return e.bounds();
        }
        AABB bounds = compute.get();
        // wholesale clear instead of LRU; models only change on resource reload
        if (BOUNDS.size() > 4096) {
            BOUNDS.clear();
        }
        BOUNDS.put(extents, new Entry(transform, bounds));
        return bounds;
    }

    private record Entry(ItemTransform transform, AABB bounds) {
    }
}
