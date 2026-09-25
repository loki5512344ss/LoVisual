package dev.loki.lovisual.render.sky;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class SkyboxShaderPassesTest {

    @Test
    void liteMaskKeepsOnlyBaseLayersAndStarGrids() {
        int mask = SkyboxShaderPasses.LITE_SKY_LAYER_MASK;
        for (int bit : new int[] {0, 1, 2, 3, 12, 13, 14, 15}) {
            assertEquals(1, (mask >> bit) & 1, "kept bit " + bit);
        }
        for (int bit : new int[] {4, 5, 6, 7, 8, 9, 10, 11, 16}) {
            assertEquals(0, (mask >> bit) & 1, "cleared noise bit " + bit);
        }
    }
}
