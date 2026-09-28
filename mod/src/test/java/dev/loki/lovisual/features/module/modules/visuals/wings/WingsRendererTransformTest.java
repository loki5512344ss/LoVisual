package dev.loki.lovisual.features.module.modules.visuals.wings;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression test for the matrix bug that made {@link WingsRenderer} render nothing:
 * {@code transformPosition} must agree with JOML's own column-major position
 * transform (Vector3f.mulPosition), including the translation component - a
 * row/column mix-up here previously dropped the player's world-space
 * translation entirely, collapsing every wing vertex toward the world origin.
 */
final class WingsRendererTransformTest {

    @Test
    void matchesJomlPositionTransformIncludingTranslation() {
        Matrix4f mat = new Matrix4f()
                .translate(100.5f, 64.0f, -200.25f)
                .rotateY((float) Math.toRadians(37f))
                .rotateX((float) Math.toRadians(12f))
                .scale(1.4f);

        float lx = 0.86f, ly = -0.46f, lz = 0f;

        Vec3 actual = WingsRenderer.transformPosition(mat, lx, ly, lz);

        Vector3f expected = new Vector3f(lx, ly, lz).mulPosition(mat);

        assertEquals(expected.x, (float) actual.x, 1e-4f);
        assertEquals(expected.y, (float) actual.y, 1e-4f);
        assertEquals(expected.z, (float) actual.z, 1e-4f);
    }

    @Test
    void translationIsActuallyApplied() {
        Matrix4f mat = new Matrix4f().translate(10f, 20f, 30f);

        Vec3 origin = WingsRenderer.transformPosition(mat, 0f, 0f, 0f);

        // Before the fix this returned (0, 0, 0): the translation column was
        // never read, so a vertex at the local origin never moved to the
        // player's world position.
        assertEquals(10f, (float) origin.x, 1e-4f);
        assertEquals(20f, (float) origin.y, 1e-4f);
        assertEquals(30f, (float) origin.z, 1e-4f);
    }
}
