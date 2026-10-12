package dev.loki.lovisual.render.engine.optimize.entity;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link ItemBounds} caching — same extents supplier + same transform
 * hits the cache, a different transform or a reset recomputes.
 */
public final class ItemBoundsTest {

    @AfterEach
    void reset() {
        ItemBounds.reset();
    }

    @Test
    void configureAndReset() {
        assertFalse(ItemBounds.enabled());
        ItemBounds.configure(true);
        assertTrue(ItemBounds.enabled());
        ItemBounds.reset();
        assertFalse(ItemBounds.enabled());
    }

    @Test
    void cachesPerExtentsAndTransform() {
        ItemBounds.configure(true);
        AtomicInteger computes = new AtomicInteger();
        Supplier<Vector3fc[]> extents = () -> new Vector3fc[]{new Vector3f(), new Vector3f(1, 1, 1)};
        Supplier<AABB> compute = () -> {
            computes.incrementAndGet();
            return new AABB(0, 0, 0, 1, 1, 1);
        };

        AABB first = ItemBounds.get(extents, ItemTransform.NO_TRANSFORM, compute);
        AABB second = ItemBounds.get(extents, ItemTransform.NO_TRANSFORM, compute);
        assertSame(first, second);
        assertEquals(1, computes.get(), "second lookup must not recompute");
    }

    @Test
    void differentTransformRecomputes() {
        ItemBounds.configure(true);
        AtomicInteger computes = new AtomicInteger();
        Supplier<Vector3fc[]> extents = () -> new Vector3fc[]{new Vector3f(), new Vector3f(1, 1, 1)};
        Supplier<AABB> compute = () -> {
            computes.incrementAndGet();
            return new AABB(0, 0, 0, 1, 1, 1);
        };

        ItemBounds.get(extents, ItemTransform.NO_TRANSFORM, compute);
        ItemTransform other = new ItemTransform(new Vector3f(1, 0, 0), new Vector3f(), new Vector3f(1, 1, 1));
        ItemBounds.get(extents, other, compute);
        assertEquals(2, computes.get(), "a new transform must invalidate the entry");
    }

    @Test
    void resetClearsCache() {
        ItemBounds.configure(true);
        AtomicInteger computes = new AtomicInteger();
        Supplier<Vector3fc[]> extents = () -> new Vector3fc[]{new Vector3f(), new Vector3f(1, 1, 1)};
        Supplier<AABB> compute = () -> {
            computes.incrementAndGet();
            return new AABB(0, 0, 0, 1, 1, 1);
        };

        ItemBounds.get(extents, ItemTransform.NO_TRANSFORM, compute);
        ItemBounds.reset();
        ItemBounds.configure(true);
        ItemBounds.get(extents, ItemTransform.NO_TRANSFORM, compute);
        assertEquals(2, computes.get(), "reset must drop the cached entry");
    }
}
