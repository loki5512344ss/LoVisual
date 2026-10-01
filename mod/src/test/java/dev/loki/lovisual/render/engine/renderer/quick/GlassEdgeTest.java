package dev.loki.lovisual.render.engine.renderer.quick;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlassEdgeTest {
    @Test
    void stopsAreWhiteWithBrightEndsAndDimMiddle() {
        int[] c = GlassEdge.stops(1f, 1f);
        for (int argb : c) assertEquals(0xFFFFFF, argb & 0xFFFFFF);
        assertTrue((c[0] >>> 24) > (c[1] >>> 24));
        assertTrue((c[3] >>> 24) > (c[2] >>> 24));
    }

    @Test
    void zeroAlphaIsInvisibleAndInputIsClamped() {
        for (int argb : GlassEdge.stops(0f, 1f)) assertEquals(0, argb >>> 24);
        int[] hot = GlassEdge.stops(5f, 5f);
        int[] max = GlassEdge.stops(1f, 1f);
        assertArrayEquals(max, hot);
    }

    @Test
    void shineRaisesTheHighlights() {
        assertTrue((GlassEdge.stops(1f, 1f)[0] >>> 24) > (GlassEdge.stops(1f, 0f)[0] >>> 24));
    }
}
