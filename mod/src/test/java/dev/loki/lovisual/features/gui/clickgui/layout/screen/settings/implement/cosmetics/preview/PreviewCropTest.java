package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.cosmetics.preview;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PreviewCropTest {
    private static float pixelAspect(PreviewCrop c, int w, int h) {
        return (c.u1() - c.u0()) * w / ((c.v1() - c.v0()) * h);
    }

    @Test
    void centeredWindowHasPreviewAspectAndFitsPicture() {
        for (int[] size : new int[][]{{1920, 1080}, {500, 900}, {300, 300}}) {
            PreviewCrop c = PreviewCrop.centered(size[0], size[1]);
            assertEquals(PreviewCrop.ASPECT, pixelAspect(c, size[0], size[1]), 1e-3f);
            assertTrue(c.u0() >= 0f && c.v0() >= 0f && c.u1() <= 1f && c.v1() <= 1f);
        }
    }

    @Test
    void movingStaysInsideThePicture() {
        PreviewCrop c = PreviewCrop.centered(300, 300).zoomed(0.5f, 300, 300).moved(5f, -5f);
        assertEquals(1f, c.u1(), 1e-5f);
        assertEquals(0f, c.v0(), 1e-5f);
    }

    @Test
    void zoomKeepsAspectAndCannotExceedFullWindow() {
        PreviewCrop full = PreviewCrop.centered(1000, 600);
        PreviewCrop out = full.zoomed(4f, 1000, 600);
        assertEquals(full.u1() - full.u0(), out.u1() - out.u0(), 1e-4f);
        PreviewCrop in = full.zoomed(0.5f, 1000, 600);
        assertEquals(PreviewCrop.ASPECT, pixelAspect(in, 1000, 600), 1e-3f);
        assertTrue(in.u1() - in.u0() < full.u1() - full.u0());
    }

    @Test
    void serializeRoundTrips() {
        PreviewCrop c = PreviewCrop.centered(640, 480).zoomed(0.7f, 640, 480);
        PreviewCrop back = PreviewCrop.parse(c.serialize());
        assertNotNull(back);
        assertEquals(c.u0(), back.u0(), 1e-4f);
        assertEquals(c.v1(), back.v1(), 1e-4f);
    }

    @Test
    void parseRejectsGarbage() {
        assertNull(PreviewCrop.parse(null));
        assertNull(PreviewCrop.parse("1 2 3"));
        assertNull(PreviewCrop.parse("a b c d"));
        assertNull(PreviewCrop.parse("0.5 0.5 0.4 0.4"));
        assertNull(PreviewCrop.parse("0 0 1.5 1"));
    }
}
