package dev.loki.lovisual.render.engine.optimize.entity;

import com.google.common.collect.MapMaker;
import java.util.List;
import java.util.Map;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import org.joml.Vector3fc;

/**
 * Stage 3 step 5: far-item level of detail. Distant flat items (generated
 * item models with a few large Z faces plus thin edge strips) drop their
 * side geometry — at 8+ blocks the two big faces read the same on screen.
 *
 * <p>The quad list handed to the item renderer is a reused per-render-state
 * buffer, so the flat-list cache is keyed by the model's first quad with
 * weak keys; a model rebuild invalidates the entry for free.
 */
public enum ItemLod {
    ;

    private static final float EDGE = 1f / 16f + 1e-4f;
    private static final Map<BakedQuad, Flat> FLAT = new MapMaker().weakKeys().makeMap();

    private static volatile boolean enabled;
    private static volatile float distanceSq;

    public static void configure(boolean on, int distance) {
        enabled = on;
        distanceSq = (float) distance * (float) distance;
    }

    public static void reset() {
        enabled = false;
        distanceSq = 0f;
        FLAT.clear();
    }

    public static boolean enabled() {
        return enabled;
    }

    public static float distanceSq() {
        return distanceSq;
    }

    /**
     * Returns the model's Z faces only when the whole model is flat (every
     * quad is an Z-axis face or thinner than 1/16 block) and has at least
     * eight quads; anything else is returned unchanged.
     */
    public static List<BakedQuad> flat(List<BakedQuad> quads) {
        if (quads.isEmpty()) {
            return quads;
        }
        Flat cached = FLAT.get(quads.getFirst());
        if (cached == null || cached.sourceSize() != quads.size()) {
            cached = new Flat(quads.size(), compute(quads));
            FLAT.put(quads.getFirst(), cached);
        }
        return cached.quads() == null ? quads : cached.quads();
    }

    private record Flat(int sourceSize, List<BakedQuad> quads) {
    }

    private static List<BakedQuad> compute(List<BakedQuad> quads) {
        if (quads.size() < 8) {
            return null;
        }
        for (BakedQuad q : quads) {
            if (q.direction().getAxis() != Direction.Axis.Z && !thin(q)) {
                return null;
            }
        }
        List<BakedQuad> faces = List.copyOf(
                quads.stream().filter(q -> q.direction().getAxis() == Direction.Axis.Z).toList());
        return faces.isEmpty() ? null : faces;
    }

    private static boolean thin(BakedQuad q) {
        float minZ = Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            Vector3fc p = q.position(i);
            minZ = Math.min(minZ, p.z());
            maxZ = Math.max(maxZ, p.z());
        }
        return maxZ - minZ <= EDGE;
    }
}
