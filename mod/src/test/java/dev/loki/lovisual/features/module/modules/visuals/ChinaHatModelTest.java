package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.render.helpers.player.HeadAnchor;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import dev.loki.lovisual.features.module.modules.visuals.hats.ChinaHatModel;

public final class ChinaHatModelTest {

    private static final double EPS = 1e-3;

    private static HeadAnchor atOrigin() {
        return HeadAnchor.world(Vec3.ZERO);
    }

    @Test
    void apexSitsOnTopOfCone() {
        ChinaHatModel.Layout layout = ChinaHatModel.layout(0.5f, 0.22f, 24, 0f, 0f, 0f, atOrigin());

        assertEquals(0.0, layout.apex().x, EPS);
        assertEquals(0.22, layout.apex().y, EPS);
        assertEquals(0.0, layout.apex().z, EPS);
    }

    @Test
    void rimRingIsFlatAtBaseRadius() {
        int segments = 24;
        float radius = 0.5f;
        ChinaHatModel.Layout layout = ChinaHatModel.layout(radius, 0.22f, segments, 0f, 0f, 0f, atOrigin());

        assertEquals(segments, layout.rim().length);
        for (Vec3 rim : layout.rim()) {
            assertEquals(0.0, rim.y, EPS);
            double distanceFromAxis = Math.sqrt(rim.x * rim.x + rim.z * rim.z);
            assertEquals(radius, distanceFromAxis, EPS);
        }
    }

    @Test
    void rimVerticesAreEvenlySpaced() {
        int segments = 4;
        ChinaHatModel.Layout layout = ChinaHatModel.layout(1.0f, 0.22f, segments, 0f, 0f, 0f, atOrigin());

        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % segments;
            double side = layout.rim()[i].distanceTo(layout.rim()[next]);
            assertEquals(Math.sqrt(2.0), side, EPS);
        }
    }

    @Test
    void yawRotationSpinsRimAroundApex() {
        float radius = 0.5f;
        ChinaHatModel.Layout before = ChinaHatModel.layout(radius, 0.22f, 24, 0f, 0f, 0f, atOrigin());
        ChinaHatModel.Layout after = ChinaHatModel.layout(radius, 0.22f, 24, 90f, 0f, 0f, atOrigin());

        assertEquals(0.22, after.apex().y, EPS);
        assertEquals(before.rim()[0].distanceTo(after.apex()),
                after.rim()[6].distanceTo(after.apex()), 0.15);
    }

    @Test
    void zeroSegmentsProducesEmptyRim() {
        ChinaHatModel.Layout layout = ChinaHatModel.layout(0.5f, 0.22f, 0, 0f, 0f, 0f, atOrigin());

        assertEquals(0, layout.rim().length);
    }

    @Test
    void buildIsNoOpWithoutMesh() {
        assertDoesNotThrow(() -> ChinaHatModel.build(null, null, atOrigin(), 40, 0.5f, 0.22f,
                0f, 0f, 0f, (i, l, a) -> 0, (i, l, a) -> 0, 1.0f));
    }
}
