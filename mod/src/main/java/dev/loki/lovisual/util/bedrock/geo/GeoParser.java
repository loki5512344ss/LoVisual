package dev.loki.lovisual.util.bedrock.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a Bedrock {@code .geo.json} (formats 1.12 and 1.21) into a bone tree. Pivots, origins and rotations
 * are moved into the entity frame: x is mirrored, and rotations become (-x, -y, z) radians.
 */
public final class GeoParser {
    /** A parsed model: the root bones, every bone by name and the texture size the uv are measured in. */
    public record Model(List<GeoBone> roots, Map<String, GeoBone> bones, int textureWidth, int textureHeight) {
        public void resetPose() {
            for (GeoBone bone : bones.values()) bone.resetPose();
        }
    }

    private GeoParser() {
    }

    public static Model parse(String json) {
        JsonObject geometry = JsonParser.parseString(json).getAsJsonObject()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject description = geometry.getAsJsonObject("description");
        int width = description.has("texture_width") ? description.get("texture_width").getAsInt() : 64;
        int height = description.has("texture_height") ? description.get("texture_height").getAsInt() : 64;

        Map<String, GeoBone> bones = new LinkedHashMap<>();
        Map<String, String> parents = new LinkedHashMap<>();
        for (JsonElement entry : geometry.getAsJsonArray("bones")) {
            JsonObject data = entry.getAsJsonObject();
            String name = data.get("name").getAsString();
            GeoBone bone = new GeoBone(name, pivot(data), rotation(data));
            if (data.has("cubes")) {
                for (JsonElement cube : data.getAsJsonArray("cubes")) bone.cubes.add(cube(cube.getAsJsonObject()));
            }
            bones.put(name, bone);
            if (data.has("parent")) parents.put(name, data.get("parent").getAsString());
        }
        List<GeoBone> roots = new ArrayList<>();
        for (GeoBone bone : bones.values()) {
            GeoBone parent = bones.get(parents.get(bone.name));
            if (parent == null || parent == bone) roots.add(bone);
            else parent.children.add(bone);
        }
        return new Model(roots, bones, width, height);
    }

    private static GeoCube cube(JsonObject data) {
        float[] origin = vec(data, "origin", 0.0f);
        float[] size = vec(data, "size", 0.0f);
        float inflate = data.has("inflate") ? data.get("inflate").getAsFloat() : 0.0f;
        float[] pivot = data.has("pivot") ? vec(data, "pivot", 0.0f)
                : new float[]{origin[0] + size[0] / 2.0f, origin[1] + size[1] / 2.0f, origin[2] + size[2] / 2.0f};
        float[] pivotMirrored = {-pivot[0], pivot[1], pivot[2]};
        return new GeoCube(pivotMirrored, rotation(data), GeoFaces.build(data, origin, size, inflate));
    }

    private static float[] pivot(JsonObject data) {
        float[] pivot = vec(data, "pivot", 0.0f);
        return new float[]{-pivot[0], pivot[1], pivot[2]};
    }

    private static float[] rotation(JsonObject data) {
        float[] degrees = vec(data, "rotation", 0.0f);
        return new float[]{(float) Math.toRadians(-degrees[0]), (float) Math.toRadians(-degrees[1]), (float) Math.toRadians(degrees[2])};
    }

    private static float[] vec(JsonObject data, String key, float fallback) {
        float[] out = {fallback, fallback, fallback};
        if (!data.has(key) || !data.get(key).isJsonArray()) return out;
        JsonArray array = data.getAsJsonArray(key);
        for (int i = 0; i < Math.min(3, array.size()); i++) out[i] = array.get(i).getAsFloat();
        return out;
    }
}
