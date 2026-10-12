package dev.loki.lovisual.render.engine.optimize.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link ItemLod} pure logic — the toggle snapshot and the flat-model
 * flattening rules — with hand-built quads, no render pipeline needed.
 */
public final class ItemLodTest {

    @AfterEach
    void reset() {
        ItemLod.reset();
    }

    @Test
    void configureAndReset() {
        assertFalse(ItemLod.enabled());
        ItemLod.configure(true, 8);
        assertTrue(ItemLod.enabled());
        assertEquals(64f, ItemLod.distanceSq(), 0.001f);
        ItemLod.reset();
        assertFalse(ItemLod.enabled());
        assertEquals(0f, ItemLod.distanceSq(), 0.001f);
    }

    @Test
    void flatKeepsOnlyZFacesOfFlatModel() {
        List<BakedQuad> flatModel = new ArrayList<>();
        flatModel.add(quad(Direction.NORTH, 0f, 0f));   // big front face
        flatModel.add(quad(Direction.SOUTH, 1f, 1f));   // big back face
        for (int i = 0; i < 8; i++) {
            flatModel.add(quad(Direction.UP, 0.94f, 1f)); // thin edge strips
        }

        List<BakedQuad> result = ItemLod.flat(flatModel);
        assertEquals(2, result.size(), "only the two Z faces survive");
        assertSame(flatModel.get(0), result.get(0));
        assertSame(flatModel.get(1), result.get(1));
    }

    @Test
    void flatLeavesThickModelUnchanged() {
        List<BakedQuad> model = new ArrayList<>();
        model.add(quad(Direction.NORTH, 0f, 0f));
        model.add(quad(Direction.SOUTH, 1f, 1f));
        for (int i = 0; i < 6; i++) {
            model.add(quad(Direction.UP, 0.94f, 1f));
        }
        model.add(quad(Direction.UP, 0f, 1f)); // thick non-Z quad — full cube side

        assertSame(model, ItemLod.flat(model), "thick model is returned as-is");
    }

    @Test
    void flatLeavesSmallModelsUnchanged() {
        // A plain baked box is six quads — below the flattening threshold.
        List<BakedQuad> box = new ArrayList<>();
        box.add(quad(Direction.NORTH, 0f, 0f));
        box.add(quad(Direction.SOUTH, 1f, 1f));
        box.add(quad(Direction.UP, 0f, 1f));
        box.add(quad(Direction.DOWN, 0f, 1f));
        box.add(quad(Direction.EAST, 0f, 1f));
        box.add(quad(Direction.WEST, 0f, 1f));

        assertSame(box, ItemLod.flat(box));
    }

    @Test
    void flatCachesPerFirstQuad() {
        List<BakedQuad> model = new ArrayList<>();
        model.add(quad(Direction.NORTH, 0f, 0f));
        model.add(quad(Direction.SOUTH, 1f, 1f));
        for (int i = 0; i < 8; i++) {
            model.add(quad(Direction.UP, 0.94f, 1f));
        }

        List<BakedQuad> first = ItemLod.flat(model);
        List<BakedQuad> second = ItemLod.flat(model);
        assertSame(first, second, "same list instance comes from the cache");
    }

    private static BakedQuad quad(Direction dir, float z0, float z1) {
        return new BakedQuad(
                new Vector3f(0f, 0f, z0), new Vector3f(1f, 0f, z0),
                new Vector3f(1f, 1f, z1), new Vector3f(0f, 1f, z1),
                0L, 0L, 0L, 0L, dir, null);
    }
}
