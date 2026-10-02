package dev.loki.lovisual.features.module.modules.visuals.models;

import dev.loki.lovisual.features.module.modules.visuals.models.mesh.blockbench.BbModelParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CustomModelsTest {
    private static final String QUAD = "{\"resolution\":{\"width\":32,\"height\":16},\"elements\":[{"
            + "\"origin\":[0,0,0],\"rotation\":[0,0,0],\"vertices\":{\"a\":[0,0,0],\"b\":[16,0,0],\"c\":[16,16,0],\"d\":[0,16,0]},"
            + "\"faces\":{\"f\":{\"vertices\":[\"a\",\"b\",\"c\",\"d\"],\"uv\":{\"a\":[0,0],\"b\":[32,0],\"c\":[32,16],\"d\":[0,16]}}}}]}";

    @Test
    void faceNamesMapToTextureFilesWithNormalAsFallback() {
        assertEquals("verity", CustomModels.faceFile("Normal"));
        assertEquals("veritytalk", CustomModels.faceFile("Talking"));
        assertEquals("verityverybad", CustomModels.faceFile("Very bad"));
        assertEquals("verity", CustomModels.faceFile("nonsense"));
    }

    @Test
    void aQuadBecomesTwoTrianglesWithNormalisedUvAndScaledPositions() {
        float[] out = BbModelParser.parse(QUAD, 0.5f);
        assertEquals(2 * 3 * 8, out.length);
        // first corner is "a" at the origin, second is "b" at x = 16 * 0.5
        assertEquals(0.0f, out[0], 1e-6f);
        assertEquals(8.0f, out[8], 1e-5f);
        // uv of "b" is 32/32 = 1 across, 0 down
        assertEquals(1.0f, out[8 + 3], 1e-6f);
        assertEquals(0.0f, out[8 + 4], 1e-6f);
        // y grows downwards in entity model space, so "c" at y = 16 lands at -8
        assertEquals(-8.0f, out[16 + 1], 1e-5f);
    }

    @Test
    void normalsAreUnitLength() {
        float[] out = BbModelParser.parse(QUAD, 1.0f);
        for (int v = 0; v < out.length; v += 8) {
            float length = (float) Math.sqrt(out[v + 5] * out[v + 5] + out[v + 6] * out[v + 6] + out[v + 7] * out[v + 7]);
            assertEquals(1.0f, length, 1e-4f);
        }
    }

    @Test
    void elementsWithoutFacesAreSkipped() {
        assertEquals(0, BbModelParser.parse("{\"elements\":[{\"origin\":[0,0,0]}]}", 1.0f).length);
    }

    @Test
    void bundledVerityModelParsesToAMeshOfWholeTriangles() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/assets/lovisual/models/verity/verity.bbmodel")) {
            assertNotNull(stream);
            float[] mesh = BbModelParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), 2.2f / 16.0f);
            assertTrue(mesh.length > 0);
            assertEquals(0, mesh.length % (3 * 8));
        }
    }

    @Test
    void bundledChickenPartsExist() {
        for (String part : new String[]{"body", "head", "right_arm", "left_arm", "right_leg", "left_leg"}) {
            for (int material = 0; material < 2; material++) {
                assertNotNull(getClass().getResourceAsStream("/assets/lovisual/models/chicken/mesh_" + part + "_" + material + ".bin"), part);
            }
        }
        assertNotNull(getClass().getResourceAsStream("/assets/lovisual/models/rabbit.png"));
    }
}
