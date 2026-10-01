package dev.loki.lovisual.features.module.modules.visuals.player.jumpcircles.shape;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShapeGeometryTest {
    private final ShapeMesh mesh = new ShapeMesh();

    @Test
    void everyShapeStaysInsideUnitCircle() {
        for (Shape shape : Shape.values()) {
            for (float phase : new float[]{0f, 1.3f, 4.7f}) {
                ShapeGeometry.build(shape, mesh, 32, 0.5f, phase);
                for (int i = 0; i < mesh.vertexCount(); i++) {
                    double r = Math.hypot(mesh.x(i), mesh.z(i));
                    assertTrue(r <= 1.0001, shape + " vertex " + i + " radius " + r);
                }
            }
        }
    }

    @Test
    void trianglesReferenceExistingVerticesAndAttributesAreNormalised() {
        for (Shape shape : Shape.values()) {
            ShapeGeometry.build(shape, mesh, 24, 0.25f, 0.5f);
            assertTrue(mesh.indexCount() > 0 && mesh.indexCount() % 3 == 0, shape.name());
            for (int i = 0; i < mesh.indexCount(); i++) {
                assertTrue(mesh.index(i) >= 0 && mesh.index(i) < mesh.vertexCount(), shape.name());
            }
            for (int i = 0; i < mesh.vertexCount(); i++) {
                assertTrue(mesh.gradient(i) >= 0f && mesh.gradient(i) <= 1f, shape.name());
                assertTrue(mesh.shade(i) >= 0f && mesh.shade(i) <= 1f, shape.name());
            }
        }
    }

    @Test
    void starHasFivePointsOnTheUnitCircle() {
        ShapeGeometry.build(Shape.STAR, mesh, 8, 0.25f, 0f);
        int tips = 0;
        for (int i = 0; i < mesh.vertexCount(); i++) {
            if (Math.hypot(mesh.x(i), mesh.z(i)) > 0.999) tips++;
        }
        assertEquals(6, tips, "5 tips plus the duplicated closing vertex");
        assertEquals(30, mesh.indexCount());
    }

    @Test
    void hexagonAlternatesMainAndDarkShade() {
        ShapeGeometry.build(Shape.HEXAGON, mesh, 8, 0.25f, 0f);
        assertEquals(24, mesh.vertexCount());
        assertEquals(0f, mesh.shade(0));
        assertEquals(ShapeGeometry.EDGE_SHADE, mesh.shade(4));
    }

    @Test
    void segmentCountScalesDetailAndRebuildClearsPreviousShape() {
        ShapeGeometry.build(Shape.PETALS, mesh, 8, 0.25f, 0f);
        int coarse = mesh.vertexCount();
        ShapeGeometry.build(Shape.PETALS, mesh, 16, 0.25f, 0f);
        assertTrue(mesh.vertexCount() > coarse);
        ShapeGeometry.build(Shape.DIAMOND, mesh, 16, 0.25f, 0f);
        assertEquals(5, mesh.vertexCount());
        assertEquals(12, mesh.indexCount());
    }

    @Test
    void wavePhaseMovesTheRingButKeepsVertexCount() {
        ShapeGeometry.build(Shape.WAVE, mesh, 40, 0.2f, 0f);
        int count = mesh.vertexCount();
        float x = mesh.x(1);
        float z = mesh.z(1);
        ShapeGeometry.build(Shape.WAVE, mesh, 40, 0.2f, 1.5f);
        assertEquals(count, mesh.vertexCount());
        assertTrue(Math.abs(x - mesh.x(1)) + Math.abs(z - mesh.z(1)) > 1e-4);
    }
}
