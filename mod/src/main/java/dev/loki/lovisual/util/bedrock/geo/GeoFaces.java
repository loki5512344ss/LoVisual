package dev.loki.lovisual.util.bedrock.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a Bedrock cube (origin, size, inflate, box or per-face uv) into textured quads in the entity frame,
 * where x is mirrored. Every texture runs towards the viewer's right and down as seen from outside the face,
 * so a painted texture reads the same way it does in Blockbench.
 */
final class GeoFaces {
    /** Face order: north, south, east, west, up, down. */
    private static final String[] NAMES = {"north", "south", "east", "west", "up", "down"};
    /** Per face: viewer-right axis, viewer-down axis, outward normal axis (as signed unit vectors). */
    private static final float[][] AXES = {
            {-1, 0, 0, 0, -1, 0, 0, 0, -1},
            {1, 0, 0, 0, -1, 0, 0, 0, 1},
            {0, 0, 1, 0, -1, 0, -1, 0, 0},
            {0, 0, -1, 0, -1, 0, 1, 0, 0},
            {-1, 0, 0, 0, 0, -1, 0, 1, 0},
            {1, 0, 0, 0, 0, -1, 0, -1, 0},
    };

    private GeoFaces() {
    }

    static List<float[]> build(JsonObject cube, float[] origin, float[] size, float inflate) {
        float[] centre = new float[3];
        float[] half = new float[3];
        // x is mirrored into the entity frame
        centre[0] = -(origin[0] + size[0] / 2.0f);
        centre[1] = origin[1] + size[1] / 2.0f;
        centre[2] = origin[2] + size[2] / 2.0f;
        half[0] = size[0] / 2.0f + inflate;
        half[1] = size[1] / 2.0f + inflate;
        half[2] = size[2] / 2.0f + inflate;

        float[][] regions = regions(cube, size);
        // a flat cube has two coincident planes; drawing both would z-fight, and the faces are double sided anyway
        dropTwin(regions, 0, 1, half[2]);
        dropTwin(regions, 2, 3, half[0]);
        dropTwin(regions, 4, 5, half[1]);
        List<float[]> faces = new ArrayList<>();
        for (int face = 0; face < 6; face++) {
            float[] region = regions[face];
            if (region == null) continue;
            float[] axes = AXES[face];
            float[] extent = extent(face, half);
            if (extent[0] <= 0.0f || extent[1] <= 0.0f) continue;
            faces.add(quad(centre, half, axes, extent, region));
        }
        return faces;
    }

    private static void dropTwin(float[][] regions, int keep, int twin, float halfThickness) {
        if (halfThickness > 0.0f) return;
        if (regions[keep] != null) regions[twin] = null;
    }

    /** Half extents of a face along its right and down axes. */
    private static float[] extent(int face, float[] half) {
        return switch (face) {
            case 0, 1 -> new float[]{half[0], half[1]};
            case 2, 3 -> new float[]{half[2], half[1]};
            default -> new float[]{half[0], half[2]};
        };
    }

    private static float[] quad(float[] centre, float[] half, float[] axes, float[] extent, float[] region) {
        float[] out = new float[20];
        float[][] corners = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        int rotation = Math.floorMod((int) region[4] / 90, 4);
        float depth = Math.abs(axes[6]) * half[0] + Math.abs(axes[7]) * half[1] + Math.abs(axes[8]) * half[2];
        for (int i = 0; i < 4; i++) {
            float sx = corners[i][0] * extent[0];
            float sy = corners[i][1] * extent[1];
            for (int a = 0; a < 3; a++) {
                out[i * 5 + a] = centre[a] + axes[a] * sx + axes[3 + a] * sy + axes[6 + a] * depth;
            }
            // uv_rotation turns the texture a quarter turn per step by shifting which corner gets which uv
            float[] uv = corner(region, (i + rotation) % 4);
            out[i * 5 + 3] = uv[0];
            out[i * 5 + 4] = uv[1];
        }
        return out;
    }

    private static float[] corner(float[] region, int index) {
        float u0 = region[0];
        float v0 = region[1];
        float u1 = u0 + region[2];
        float v1 = v0 + region[3];
        return switch (index) {
            case 0 -> new float[]{u0, v0};
            case 1 -> new float[]{u1, v0};
            case 2 -> new float[]{u1, v1};
            default -> new float[]{u0, v1};
        };
    }

    /** Per face {u, v, width, height, rotation} in texture pixels, or null when the cube has no such face. */
    private static float[][] regions(JsonObject cube, float[] size) {
        float[][] out = new float[6][];
        JsonElement uv = cube.get("uv");
        if (uv == null) return out;
        if (uv.isJsonArray()) {
            boxRegions(out, uv.getAsJsonArray(), size, cube.has("mirror") && cube.get("mirror").getAsBoolean());
        } else if (uv.isJsonObject()) {
            JsonObject faces = uv.getAsJsonObject();
            for (int face = 0; face < 6; face++) {
                if (!faces.has(NAMES[face])) continue;
                JsonObject data = faces.getAsJsonObject(NAMES[face]);
                JsonArray at = data.getAsJsonArray("uv");
                JsonArray span = data.has("uv_size") ? data.getAsJsonArray("uv_size") : null;
                float w = span == null ? 0 : span.get(0).getAsFloat();
                float h = span == null ? 0 : span.get(1).getAsFloat();
                float rotation = data.has("uv_rotation") ? data.get("uv_rotation").getAsFloat() : 0;
                out[face] = new float[]{at.get(0).getAsFloat(), at.get(1).getAsFloat(), w, h, rotation};
            }
        }
        return out;
    }

    private static void boxRegions(float[][] out, JsonArray uv, float[] size, boolean mirror) {
        float u = uv.get(0).getAsFloat();
        float v = uv.get(1).getAsFloat();
        float w = size[0];
        float h = size[1];
        float d = size[2];
        float[] east = {u, v + d, d, h, 0};
        float[] west = {u + d + w, v + d, d, h, 0};
        out[0] = new float[]{u + d, v + d, w, h, 0};
        out[1] = new float[]{u + 2 * d + w, v + d, w, h, 0};
        out[2] = mirror ? west : east;
        out[3] = mirror ? east : west;
        out[4] = new float[]{u + d, v, w, d, 0};
        out[5] = new float[]{u + d + w, v + d, w, -d, 0};
    }
}
