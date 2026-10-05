package dev.loki.lovisual.features.module.modules.visuals.items.glint.style;

/** Tileable procedural noise for the glint styles; every cell lookup wraps with the grid period. */
public final class GlintNoise {
    private GlintNoise() {
    }

    /** Integer hash of a wrapped grid cell, 0..1. */
    public static float hash(int x, int y, int period, int seed) {
        int h = Math.floorMod(x, period) * 0x1F3D5B79 + Math.floorMod(y, period) * 0x2545F491 + seed * 0x6C8E9CF5;
        h ^= h >>> 16;
        h *= 0x7FEB352D;
        h ^= h >>> 15;
        h *= 0x846CA68B;
        h ^= h >>> 16;
        return (h >>> 8) * (1.0f / (1 << 24));
    }

    public static float smooth(float t) {
        return t * t * (3.0f - 2.0f * t);
    }

    public static float wave(float radians) {
        return 0.5f + 0.5f * (float) Math.cos(radians);
    }

    /** Bilinear value noise on a {@code cells} x {@code cells} wrapping grid. */
    public static float value(float u, float v, int cells, int seed) {
        float x = u * cells;
        float y = v * cells;
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        float fx = smooth(x - ix);
        float fy = smooth(y - iy);
        float top = lerp(fx, hash(ix, iy, cells, seed), hash(ix + 1, iy, cells, seed));
        float bottom = lerp(fx, hash(ix, iy + 1, cells, seed), hash(ix + 1, iy + 1, cells, seed));
        return lerp(fy, top, bottom);
    }

    /** Fractal sum of value noise, each octave doubling the grid and halving the weight; result 0..1. */
    public static float fbm(float u, float v, int octaves, int baseCells, int seed) {
        float sum = 0.0f;
        float weight = 1.0f;
        float total = 0.0f;
        int cells = baseCells;
        for (int i = 0; i < octaves; i++) {
            sum += value(u, v, cells, seed + i) * weight;
            total += weight;
            weight *= 0.5f;
            cells <<= 1;
        }
        return sum / total;
    }

    /** Nearest-point glitter: one jittered point per cell with a soft round falloff; result 0..1. */
    public static float sparkles(float u, float v, int cells, float radius, float density, int seed) {
        float x = u * cells;
        float y = v * cells;
        int cx = (int) Math.floor(x);
        int cy = (int) Math.floor(y);
        float best = 0.0f;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = cx + dx;
                int ny = cy + dy;
                if (hash(nx, ny, cells, seed) > density) continue;
                float px = nx + 0.15f + hash(nx, ny, cells, seed + 101) * 0.7f;
                float py = ny + 0.15f + hash(nx, ny, cells, seed + 202) * 0.7f;
                float distance = (float) Math.hypot((x - px) / radius, (y - py) / radius);
                float glow = 1.0f - distance;
                if (glow > 0.0f) best = Math.max(best, glow * glow);
            }
        }
        return best;
    }

    public static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }
}
