package dev.loki.lovisual.features.module.modules.visuals.knife;

import dev.loki.lovisual.features.module.modules.visuals.knife.model.KnifeModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class KnifeAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/lovisual");

    @Test
    void indexListsEverySkinWithItsFiles() throws IOException {
        assertEquals(33, KnifeSkin.ALL.size());
        for (KnifeSkin skin : KnifeSkin.ALL) {
            assertTrue(Files.exists(ASSETS.resolve("textures/knife/" + skin.key() + ".png")), skin.key());
            assertTrue(Files.exists(ASSETS.resolve("knife/" + skin.model() + ".geo.json")), skin.key());
            assertTrue(Files.exists(ASSETS.resolve("knife/" + skin.model() + ".anim.json")), skin.key());
        }
        KnifeSkin annihil = KnifeSkin.byKey("annihil");
        assertEquals(2, annihil.variants().size());
        assertEquals("_angel", annihil.variants().get(1).suffix());
        assertEquals(180.0f, annihil.thirdRight().rotation()[0]);
        assertEquals(List.of("blade"), KnifeSkin.byKey("m9holy").holyBones());
        assertEquals("m9", KnifeSkin.byKey("m9holy").model());
    }

    @Test
    void everyModelBakesWithHandsAnchorAndClips() throws IOException {
        for (String model : KnifeSkin.ALL.stream().map(KnifeSkin::model).distinct().toList()) {
            KnifeModel baked = KnifeModel.bake(Files.readString(ASSETS.resolve("knife/" + model + ".geo.json")),
                    Files.readString(ASSETS.resolve("knife/" + model + ".anim.json")));
            assertTrue(baked.boneCount() > 0, model);
            assertNotNull(baked.clip("static_idle"), model);
            assertTrue(baked.viewAnchor() >= 0, model);
            int vertices = 0;
            for (int bone = 0; bone < baked.boneCount(); bone++) {
                float[] mesh = baked.meshOf(bone);
                if (mesh == null) continue;
                assertEquals(0, mesh.length % (KnifeModel.FLOATS_PER_VERTEX * KnifeModel.VERTICES_PER_FACE), model);
                for (float value : mesh) assertTrue(Float.isFinite(value), model);
                vertices += mesh.length;
                assertTrue(baked.parentOf(bone) < bone, model);
            }
            assertTrue(vertices > 0, model);
        }
    }

    @Test
    void hiddenBranchesIncludeDescendants() throws IOException {
        KnifeModel annihil = KnifeModel.bake(Files.readString(ASSETS.resolve("knife/annihil.geo.json")),
                Files.readString(ASSETS.resolve("knife/annihil.anim.json")));
        boolean[] flags = new boolean[annihil.boneCount()];
        annihil.markBranches(List.of("angel"), flags);
        int angel = annihil.bone("angel");
        assertTrue(angel >= 0 && flags[angel]);
        for (int bone = 0; bone < flags.length; bone++) {
            if (annihil.parentOf(bone) == angel) assertTrue(flags[bone]);
        }
        assertFalse(flags[annihil.bone("devil")]);
    }
}
