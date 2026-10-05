package dev.loki.lovisual.features.module.modules.visuals.knife.model;

import dev.loki.lovisual.features.module.modules.visuals.knife.anim.KnifeClip;
import dev.loki.lovisual.features.module.modules.visuals.knife.anim.types.ClipSource;
import dev.loki.lovisual.util.bedrock.anim.Clip;
import dev.loki.lovisual.util.bedrock.geo.GeoBone;
import dev.loki.lovisual.util.bedrock.geo.GeoCube;
import dev.loki.lovisual.util.bedrock.geo.GeoParser;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A knife view model ready to draw: bones in parent-first order with pivots (pixels, entity frame) and rest
 * rotations, the geometry of every bone baked to quads in blocks, and the model's clips. Special bones are
 * found by name: {@code righthand_pos}, {@code lefthand_pos}, {@code camera}, {@code idle_view} and
 * {@code thirdperson_hand}.
 */
public final class KnifeModel implements ClipSource {
    public static final int FLOATS_PER_VERTEX = 8;
    public static final int VERTICES_PER_FACE = 4;

    private final String[] names;
    private final int[] parents;
    private final float[] pivots;
    private final float[] rest;
    private final float[][] meshes;
    private final Map<String, Integer> index;
    private final Map<String, KnifeClip> clips;

    private KnifeModel(String[] names, int[] parents, float[] pivots, float[] rest, float[][] meshes,
                       Map<String, Integer> index, Map<String, KnifeClip> clips) {
        this.names = names;
        this.parents = parents;
        this.pivots = pivots;
        this.rest = rest;
        this.meshes = meshes;
        this.index = index;
        this.clips = clips;
    }

    public static KnifeModel bake(String geometryJson, String animationJson) {
        GeoParser.Model geo = GeoParser.parse(geometryJson);
        List<GeoBone> ordered = new ArrayList<>();
        for (GeoBone root : geo.roots()) collect(root, ordered);

        int count = ordered.size();
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < count; i++) index.put(ordered.get(i).name, i);
        Map<GeoBone, GeoBone> parentOf = new HashMap<>();
        for (GeoBone bone : ordered) {
            for (GeoBone child : bone.children) parentOf.put(child, bone);
        }

        String[] names = new String[count];
        int[] parents = new int[count];
        float[] pivots = new float[count * 3];
        float[] rest = new float[count * 3];
        float[][] meshes = new float[count][];
        for (int i = 0; i < count; i++) {
            GeoBone bone = ordered.get(i);
            names[i] = bone.name;
            GeoBone parent = parentOf.get(bone);
            parents[i] = parent == null ? -1 : index.get(parent.name);
            System.arraycopy(bone.pivot, 0, pivots, i * 3, 3);
            System.arraycopy(bone.baseRot, 0, rest, i * 3, 3);
            meshes[i] = mesh(bone, geo.textureWidth(), geo.textureHeight());
        }
        Map<String, KnifeClip> clips = new HashMap<>();
        for (Map.Entry<String, Clip> entry : Clip.parseAll(animationJson).entrySet()) {
            clips.put(entry.getKey(), new KnifeClip(entry.getValue(), index));
        }
        return new KnifeModel(names, parents, pivots, rest, meshes, Map.copyOf(index), clips);
    }

    private static void collect(GeoBone bone, List<GeoBone> out) {
        out.add(bone);
        for (GeoBone child : bone.children) collect(child, out);
    }

    /** Quads in blocks, each vertex: position (3), uv (2), normal (3); the cube's own rotation is baked in. */
    private static float[] mesh(GeoBone bone, int textureWidth, int textureHeight) {
        int faces = 0;
        for (GeoCube cube : bone.cubes) faces += cube.faces().size();
        if (faces == 0) return null;
        float[] out = new float[faces * VERTICES_PER_FACE * FLOATS_PER_VERTEX];
        int at = 0;
        Matrix4f matrix = new Matrix4f();
        Vector3f[] corner = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
        Vector3f edgeRight = new Vector3f();
        Vector3f edgeDown = new Vector3f();
        for (GeoCube cube : bone.cubes) {
            cube.transform(matrix.identity());
            for (float[] face : cube.faces()) {
                for (int i = 0; i < 4; i++) {
                    corner[i].set(face[i * 5] / 16.0f, face[i * 5 + 1] / 16.0f, face[i * 5 + 2] / 16.0f);
                    matrix.transformPosition(corner[i]);
                }
                corner[1].sub(corner[0], edgeRight);
                corner[3].sub(corner[0], edgeDown);
                Vector3f normal = edgeDown.cross(edgeRight, new Vector3f());
                if (normal.lengthSquared() > 0.0f) normal.normalize();
                for (int i = 0; i < 4; i++) {
                    out[at++] = corner[i].x;
                    out[at++] = corner[i].y;
                    out[at++] = corner[i].z;
                    out[at++] = face[i * 5 + 3] / textureWidth;
                    out[at++] = face[i * 5 + 4] / textureHeight;
                    out[at++] = normal.x;
                    out[at++] = normal.y;
                    out[at++] = normal.z;
                }
            }
        }
        return out;
    }

    @Override
    public int boneCount() {
        return names.length;
    }

    @Override
    public KnifeClip clip(String name) {
        return clips.get(name);
    }

    @Override
    public void markBranches(List<String> boneNames, boolean[] flags) {
        java.util.Arrays.fill(flags, false);
        for (String name : boneNames) {
            Integer bone = index.get(name);
            if (bone != null) flags[bone] = true;
        }
        for (int i = 0; i < flags.length; i++) {
            if (parents[i] >= 0 && flags[parents[i]]) flags[i] = true;
        }
    }

    public String name(int bone) {
        return names[bone];
    }

    public int parentOf(int bone) {
        return parents[bone];
    }

    public float pivotX(int bone) {
        return pivots[bone * 3];
    }

    public float pivotY(int bone) {
        return pivots[bone * 3 + 1];
    }

    public float pivotZ(int bone) {
        return pivots[bone * 3 + 2];
    }

    public float restX(int bone) {
        return rest[bone * 3];
    }

    public float restY(int bone) {
        return rest[bone * 3 + 1];
    }

    public float restZ(int bone) {
        return rest[bone * 3 + 2];
    }

    /** Quads of one bone, or null when it has no geometry. */
    public float[] meshOf(int bone) {
        return meshes[bone];
    }

    public int bone(String name) {
        return index.getOrDefault(name, -1);
    }

    public int rightHand() {
        return bone("righthand_pos");
    }

    public int leftHand() {
        return bone("lefthand_pos");
    }

    public int camera() {
        return bone("camera");
    }

    /** The bone that sits at the player's eye; falls back to the camera bone like the reference does. */
    public int viewAnchor() {
        int anchor = bone("idle_view");
        return anchor >= 0 ? anchor : camera();
    }

    public int thirdPersonHand() {
        return bone("thirdperson_hand");
    }
}
