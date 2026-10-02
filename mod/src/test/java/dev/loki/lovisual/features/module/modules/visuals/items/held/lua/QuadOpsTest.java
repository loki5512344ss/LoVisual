package dev.loki.lovisual.features.module.modules.visuals.items.held.lua;

import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.ext.QuadOps;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.ext.QuadOps.Op;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.ext.QuadOps.Type;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuadOpsTest {

    @Test
    void rotateZ90AroundPivotMovesVertex() {
        float[] v = {1f, 0.5f, 0.3f};
        QuadOps.apply(new Op(Type.ROT_Z, 0, 0, 90f, 0.5f, 0.5f, 0f), v);
        assertEquals(0.5f, v[0], 1e-5f);
        assertEquals(1.0f, v[1], 1e-5f);
        assertEquals(0.3f, v[2], 1e-5f);
    }

    @Test
    void scaleAroundPivotKeepsPivotFixed() {
        float[] v = {0.5f, 0.5f, 0.5f};
        QuadOps.apply(new Op(Type.SCALE, 0, 3, 2f, 0.5f, 0.5f, 0.5f), v);
        assertArrayEquals(new float[]{0.5f, 0.5f, 0.5f}, v, 1e-6f);
        float[] w = {1f, 0.5f, 0.5f};
        QuadOps.apply(new Op(Type.SCALE, 0, 3, 2f, 0.5f, 0.5f, 0.5f), w);
        assertEquals(1.5f, w[0], 1e-6f);
    }

    @Test
    void moveAndRangeCoverage() {
        Op op = new Op(Type.MOVE_Y, 2, 4, 0.25f, 0, 0, 0);
        assertFalse(op.covers(1));
        assertTrue(op.covers(2));
        assertTrue(op.covers(4));
        assertFalse(op.covers(5));
        float[] v = {0f, 0f, 0f};
        QuadOps.apply(op, v);
        assertEquals(0.25f, v[1], 1e-6f);
    }
}
