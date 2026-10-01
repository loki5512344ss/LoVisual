package dev.loki.lovisual.features.module.modules.visuals.player.jumpcircles.shape;

import java.util.Arrays;

/**
 * Reusable triangle list in the unit ground plane. Each vertex carries a gradient position
 * {@code t} in [0,1] and a {@code shade} in [0,1] (0 = main colour, 1 = darkened outline colour).
 */
public final class ShapeMesh {
    private float[] x = new float[64];
    private float[] z = new float[64];
    private float[] t = new float[64];
    private float[] shade = new float[64];
    private int[] indices = new int[192];
    private int vertexCount;
    private int indexCount;

    public void clear() {
        vertexCount = 0;
        indexCount = 0;
    }

    public int vertex(float px, float pz, float gradient, float vertexShade) {
        if (vertexCount == x.length) {
            int size = vertexCount * 2;
            x = Arrays.copyOf(x, size);
            z = Arrays.copyOf(z, size);
            t = Arrays.copyOf(t, size);
            shade = Arrays.copyOf(shade, size);
        }
        x[vertexCount] = px;
        z[vertexCount] = pz;
        t[vertexCount] = gradient;
        shade[vertexCount] = vertexShade;
        return vertexCount++;
    }

    public void triangle(int a, int b, int c) {
        if (indexCount + 3 > indices.length) {
            indices = Arrays.copyOf(indices, indices.length * 2);
        }
        indices[indexCount++] = a;
        indices[indexCount++] = b;
        indices[indexCount++] = c;
    }

    public int vertexCount() {
        return vertexCount;
    }

    public int indexCount() {
        return indexCount;
    }

    public float x(int i) {
        return x[i];
    }

    public float z(int i) {
        return z[i];
    }

    public float gradient(int i) {
        return t[i];
    }

    public float shade(int i) {
        return shade[i];
    }

    public int index(int i) {
        return indices[i];
    }
}
