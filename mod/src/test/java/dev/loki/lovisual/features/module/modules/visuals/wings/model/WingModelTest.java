package dev.loki.lovisual.features.module.modules.visuals.wings.model;

import dev.loki.lovisual.features.module.modules.visuals.wings.model.anim.BedrockClip;
import dev.loki.lovisual.features.module.modules.visuals.wings.model.anim.ProceduralWings;
import dev.loki.lovisual.features.module.modules.visuals.wings.model.anim.WingAnimator;
import dev.loki.lovisual.util.bedrock.geo.GeoBone;
import dev.loki.lovisual.util.bedrock.geo.GeoCube;
import dev.loki.lovisual.util.bedrock.geo.GeoParser;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class WingModelTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/lovisual");

    private static String read(String relative) throws IOException {
        return Files.readString(ASSETS.resolve(relative));
    }

    private static final String MINI = """
            {"format_version":"1.12.0","minecraft:geometry":[{"description":{"texture_width":16,"texture_height":16},
             "bones":[
              {"name":"root","pivot":[0,10,0]},
              {"name":"arm","parent":"root","pivot":[4,10,0],"rotation":[10,20,30],
               "cubes":[{"origin":[1,0,0],"size":[2,2,2],"uv":[0,0]},
                        {"origin":[0,0,0],"size":[4,4,0],"uv":{"north":{"uv":[0,0],"uv_size":[4,4]},"south":{"uv":[4,0],"uv_size":[4,4]}}}]}
             ]}]}""";

    @Test
    void miniModelBuildsTreeMirrorsXAndFlipsRotation() {
        GeoParser.Model model = GeoParser.parse(MINI);
        assertEquals(16, model.textureWidth());
        assertEquals(1, model.roots().size());
        GeoBone arm = model.bones().get("arm");
        assertEquals(arm, model.roots().get(0).children.get(0));
        assertEquals(-4.0f, arm.pivot[0]);
        assertEquals((float) Math.toRadians(-10), arm.baseRot[0], 1e-6f);
        assertEquals((float) Math.toRadians(-20), arm.baseRot[1], 1e-6f);
        assertEquals((float) Math.toRadians(30), arm.baseRot[2], 1e-6f);

        GeoCube box = arm.cubes.get(0);
        assertEquals(6, box.faces().size());
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        for (float[] face : box.faces()) {
            for (int i = 0; i < 4; i++) {
                minX = Math.min(minX, face[i * 5]);
                maxX = Math.max(maxX, face[i * 5]);
            }
        }
        assertEquals(-3.0f, minX, 1e-5f);
        assertEquals(-1.0f, maxX, 1e-5f);
    }

    @Test
    void boxUvPlacesTheFrontFaceAfterTheSideColumn() {
        GeoCube box = GeoParser.parse(MINI).bones().get("arm").cubes.get(0);
        float[] north = box.faces().get(0);
        // box uv (0,0) with depth 2: front region starts at u = depth, v = depth
        assertEquals(2.0f, Math.min(north[3], Math.min(north[8], Math.min(north[13], north[18]))), 1e-5f);
        assertEquals(2.0f, Math.min(north[4], Math.min(north[9], Math.min(north[14], north[19]))), 1e-5f);
    }

    @Test
    void flatCubesDrawOnlyOneOfTheTwinPlanes() {
        GeoCube flat = GeoParser.parse(MINI).bones().get("arm").cubes.get(1);
        assertEquals(1, flat.faces().size());
    }

    @Test
    void rotationAboutThePivotKeepsThePivotFixed() {
        GeoParser.Model model = GeoParser.parse(MINI);
        GeoBone arm = model.bones().get("arm");
        arm.rot[2] = 1.0f;
        Vector3f pivot = new Vector3f(arm.pivot[0] / 16, arm.pivot[1] / 16, arm.pivot[2] / 16);
        arm.transform(new Matrix4f()).transformPosition(pivot);
        assertEquals(arm.pivot[0] / 16, pivot.x, 1e-5f);
        assertEquals(arm.pivot[1] / 16, pivot.y, 1e-5f);
    }

    @Test
    void everyCatalogModelLoadsWithItsResources() throws IOException {
        for (WingModelDef def : WingModelDef.ALL) {
            GeoParser.Model model = GeoParser.parse(read("wings/" + def.geometry() + ".geo.json"));
            int faces = 0;
            for (GeoBone bone : model.bones().values()) {
                for (GeoCube cube : bone.cubes) {
                    faces += cube.faces().size();
                    for (float[] face : cube.faces()) {
                        for (float value : face) assertTrue(Float.isFinite(value), def.name());
                    }
                }
            }
            assertTrue(faces > 0, def.name());
            assertTrue(Files.exists(ASSETS.resolve("textures/wings/" + def.texture() + ".png")), def.name());
            if (!def.procedural()) assertTrue(Files.exists(ASSETS.resolve("wings/" + def.animation() + ".animation.json")), def.name());
        }
    }

    @Test
    void animationsPoseEveryModelWithoutBlowingUp() throws IOException {
        for (WingModelDef def : WingModelDef.ALL) {
            GeoParser.Model model = GeoParser.parse(read("wings/" + def.geometry() + ".geo.json"));
            WingAnimator animator = animator(def);
            for (float seconds = 0.0f; seconds < 7.0f; seconds += 0.37f) {
                animator.apply(model, seconds, seconds / 7.0f);
                for (GeoBone bone : model.bones().values()) {
                    for (int a = 0; a < 3; a++) {
                        assertTrue(Float.isFinite(bone.rot[a]) && Float.isFinite(bone.offset[a]) && Float.isFinite(bone.scale[a]), def.name());
                    }
                }
            }
        }
    }

    private static WingAnimator animator(WingModelDef def) throws IOException {
        return switch (def.animation()) {
            case "angel" -> ProceduralWings.ANGEL;
            case "demon" -> ProceduralWings.DEMON;
            case "dragon" -> ProceduralWings.DRAGON;
            default -> BedrockClip.parse(read("wings/" + def.animation() + ".animation.json"));
        };
    }

    @Test
    void clipInterpolatesLinearlyAndLoops() {
        GeoParser.Model model = GeoParser.parse(MINI);
        BedrockClip clip = BedrockClip.parse("""
                {"animations":{"animation.x.idle":{"loop":true,"animation_length":2,
                 "bones":{"arm":{"rotation":{"0":[0,0,0],"1":[0,0,10],"2":[0,0,0]}}}}}}""");
        GeoBone arm = model.bones().get("arm");
        clip.apply(model, 0.5f, 0.0f);
        assertEquals(arm.baseRot[2] + (float) Math.toRadians(5), arm.rot[2], 1e-5f);
        clip.apply(model, 1.0f, 0.0f);
        assertEquals(arm.baseRot[2] + (float) Math.toRadians(10), arm.rot[2], 1e-5f);
        clip.apply(model, 2.5f, 0.0f); // wraps to 0.5 s
        assertEquals(arm.baseRot[2] + (float) Math.toRadians(5), arm.rot[2], 1e-5f);
    }

    @Test
    void flatClipRootAndObjectKeyframesAreUnderstood() {
        BedrockClip clip = BedrockClip.parse("""
                {"idle":{"loop":true,"animation_length":3.2,
                 "bones":{"arm":{"position":{"0":{"post":[2,1,0]},"1":[2,1,0]},"scale":[2,2,2]}}}}""");
        GeoParser.Model model = GeoParser.parse(MINI);
        clip.apply(model, 0.3f, 0.0f);
        GeoBone arm = model.bones().get("arm");
        assertEquals(-2.0f, arm.offset[0], 1e-5f);
        assertEquals(1.0f, arm.offset[1], 1e-5f);
        assertEquals(2.0f, arm.scale[0], 1e-5f);
        assertEquals(3.2f, clip.length(), 1e-5f);
    }

    @Test
    void angelShrinksItsRootAndFlapsOverTime() throws IOException {
        GeoParser.Model model = GeoParser.parse(read("wings/angel.geo.json"));
        ProceduralWings.ANGEL.apply(model, 0.0f, 0.0f);
        assertEquals(0.6f, model.roots().get(0).scale[0], 1e-6f);
        GeoBone left = model.bones().get("Left_Wing");
        assertNotNull(left);
        float before = left.rot[0];
        ProceduralWings.ANGEL.apply(model, 0.4f, 0.0f);
        assertFalse(before == left.rot[0]);
    }

    @Test
    void dragonBlendsToItsGlidingPose() throws IOException {
        GeoParser.Model model = GeoParser.parse(read("wings/dragon.geo.json"));
        ProceduralWings.DRAGON.apply(model, 0.0f, 0.0f);
        float standing = model.bones().get("left_wing").rot[2];
        ProceduralWings.DRAGON.apply(model, 0.0f, 1.0f);
        assertFalse(standing == model.bones().get("left_wing").rot[2]);
        assertEquals(1.0f, model.roots().get(0).scale[0], 1e-6f);
    }

    @Test
    void catalogLookupFallsBackToTheFirstModel() {
        assertEquals("Angel", WingModelDef.byName("Angel").name());
        assertEquals("Baby Dragon", WingModelDef.byName("nope").name());
        assertEquals(14, WingModelDef.NAMES.length);
    }
}
