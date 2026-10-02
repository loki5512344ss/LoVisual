package dev.loki.lovisual.features.module.modules.visuals.models.mesh.blockbench;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Map;

/**
 * Reads the free-form mesh elements of a Blockbench {@code .bbmodel} into triangles (position, uv, normal per
 * vertex). Each element's own rotation about its origin is baked in, and the result is moved into entity model
 * space (y grows downwards) at {@code unit} blocks per Blockbench unit.
 */
public final class BbModelParser {
    private static final float DEFAULT_UV_SIZE = 64.0f;
    private static final int FLOATS_PER_VERTEX = 8;

    private BbModelParser() {
    }

    public static float[] parse(String json, float unit) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        float uvWidth = DEFAULT_UV_SIZE;
        float uvHeight = DEFAULT_UV_SIZE;
        if (root.has("resolution")) {
            JsonObject resolution = root.getAsJsonObject("resolution");
            uvWidth = resolution.get("width").getAsFloat();
            uvHeight = resolution.get("height").getAsFloat();
        }
        FloatList out = new FloatList();
        for (JsonElement entry : root.getAsJsonArray("elements")) {
            JsonObject element = entry.getAsJsonObject();
            if (!element.has("vertices") || !element.has("faces")) continue;
            float[] origin = triple(element, "origin");
            float[] rotation = triple(element, "rotation");
            JsonObject vertices = element.getAsJsonObject("vertices");
            for (Map.Entry<String, JsonElement> face : element.getAsJsonObject("faces").entrySet()) {
                JsonObject data = face.getValue().getAsJsonObject();
                JsonArray order = data.getAsJsonArray("vertices");
                JsonObject uv = data.getAsJsonObject("uv");
                // a quad is two triangles around its first corner; larger faces fan the same way
                for (int corner = 2; corner < order.size(); corner++) {
                    String[] ids = {order.get(0).getAsString(), order.get(corner - 1).getAsString(), order.get(corner).getAsString()};
                    triangle(out, vertices, uv, origin, rotation, uvWidth, uvHeight, unit, ids);
                }
            }
        }
        return out.toArray();
    }

    private static void triangle(FloatList out, JsonObject vertices, JsonObject uv, float[] origin, float[] rotation,
                                 float uvWidth, float uvHeight, float unit, String[] ids) {
        float[][] points = new float[3][];
        for (int i = 0; i < 3; i++) {
            points[i] = point(vertices, origin, rotation, ids[i], unit);
            if (points[i] == null) return;
        }
        float[] normal = normal(points[0], points[1], points[2]);
        for (int i = 0; i < 3; i++) {
            float[] texture = uvOf(uv, ids[i], uvWidth, uvHeight);
            out.add(points[i][0]).add(points[i][1]).add(points[i][2]);
            out.add(texture[0]).add(texture[1]);
            out.add(normal[0]).add(normal[1]).add(normal[2]);
        }
    }

    private static float[] point(JsonObject vertices, float[] origin, float[] rotation, String id, float unit) {
        if (!vertices.has(id)) return null;
        JsonArray raw = vertices.getAsJsonArray(id);
        float[] rotated = rotate(raw.get(0).getAsFloat() - origin[0], raw.get(1).getAsFloat() - origin[1],
                raw.get(2).getAsFloat() - origin[2], rotation);
        // Blockbench has y growing upwards, entity model space has it growing down
        return new float[]{(rotated[0] + origin[0]) * unit, -(rotated[1] + origin[1]) * unit, (rotated[2] + origin[2]) * unit};
    }

    /** X, then Y, then Z: the order Blockbench applies an element's own rotation in. */
    static float[] rotate(float x, float y, float z, float[] rotationDegrees) {
        double rx = Math.toRadians(rotationDegrees[0]);
        double ry = Math.toRadians(rotationDegrees[1]);
        double rz = Math.toRadians(rotationDegrees[2]);
        float y1 = (float) (y * Math.cos(rx) - z * Math.sin(rx));
        float z1 = (float) (y * Math.sin(rx) + z * Math.cos(rx));
        float x2 = (float) (x * Math.cos(ry) + z1 * Math.sin(ry));
        float z2 = (float) (-x * Math.sin(ry) + z1 * Math.cos(ry));
        float x3 = (float) (x2 * Math.cos(rz) - y1 * Math.sin(rz));
        float y3 = (float) (x2 * Math.sin(rz) + y1 * Math.cos(rz));
        return new float[]{x3, y3, z2};
    }

    private static float[] uvOf(JsonObject uv, String id, float uvWidth, float uvHeight) {
        if (uv == null || !uv.has(id)) return new float[]{0.0f, 0.0f};
        JsonArray raw = uv.getAsJsonArray(id);
        return new float[]{raw.get(0).getAsFloat() / uvWidth, raw.get(1).getAsFloat() / uvHeight};
    }

    static float[] normal(float[] a, float[] b, float[] c) {
        float ux = b[0] - a[0];
        float uy = b[1] - a[1];
        float uz = b[2] - a[2];
        float vx = c[0] - a[0];
        float vy = c[1] - a[1];
        float vz = c[2] - a[2];
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length < 1.0e-6f) return new float[]{0.0f, -1.0f, 0.0f};
        return new float[]{nx / length, ny / length, nz / length};
    }

    private static float[] triple(JsonObject element, String key) {
        if (!element.has(key)) return new float[]{0.0f, 0.0f, 0.0f};
        JsonArray raw = element.getAsJsonArray(key);
        return new float[]{raw.get(0).getAsFloat(), raw.get(1).getAsFloat(), raw.get(2).getAsFloat()};
    }

    /** Growable float buffer, so a thousand-triangle mesh is not boxed into Float objects. */
    private static final class FloatList {
        private float[] data = new float[FLOATS_PER_VERTEX * 3 * 256];
        private int size;

        FloatList add(float value) {
            if (size == data.length) data = java.util.Arrays.copyOf(data, size * 2);
            data[size++] = value;
            return this;
        }

        float[] toArray() {
            return java.util.Arrays.copyOf(data, size);
        }
    }
}
