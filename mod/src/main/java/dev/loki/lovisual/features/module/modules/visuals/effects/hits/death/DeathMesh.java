package dev.loki.lovisual.features.module.modules.visuals.effects.hits.death;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

/**
 * Рисует часть тела как куб с UV скина. Мировые пайплайны рисуют без отсечения задних граней,
 * поэтому порядок обхода вершин не важен.
 */
final class DeathMesh {

    private static final float TEX = 64.0f;
    /** Яркость граней: +x, -x, +y, -y, +z, -z. */
    private static final float[] SHADE = {0.8f, 0.8f, 1.0f, 0.55f, 0.9f, 0.7f};

    private DeathMesh() {
    }

    static void draw(MeshBuilder mesh, DeathPhysics.Part p, float[] layout, int rgb, float alpha, boolean textured) {
        float w = p.hw, h = p.hh, d = p.hd;
        float u = layout[3], v = layout[4];
        float pw = layout[0], ph = layout[1], pd = layout[2];
        float[][] c = corners(p, w, h, d);
        // порядок углов: 0:-x-y-z 1:+x-y-z 2:+x+y-z 3:-x+y-z 4:-x-y+z 5:+x-y+z 6:+x+y+z 7:-x+y+z
        face(mesh, c, 4, 5, 6, 7, u + pd, v + pd, pw, ph, rgb, alpha, SHADE[4], textured);                 // +z (перед)
        face(mesh, c, 1, 0, 3, 2, u + pd + pw + pd, v + pd, pw, ph, rgb, alpha, SHADE[5], textured);       // -z (спина)
        face(mesh, c, 0, 4, 7, 3, u, v + pd, pd, ph, rgb, alpha, SHADE[1], textured);                      // -x (правая сторона)
        face(mesh, c, 5, 1, 2, 6, u + pd + pw, v + pd, pd, ph, rgb, alpha, SHADE[0], textured);            // +x (левая сторона)
        face(mesh, c, 7, 6, 2, 3, u + pd, v, pw, pd, rgb, alpha, SHADE[2], textured);                      // +y (верх)
        face(mesh, c, 0, 1, 5, 4, u + pd + pw, v, pw, pd, rgb, alpha, SHADE[3], textured);                 // -y (низ)
    }

    /** Углы куба после поворота Эйлера (Y, затем X, затем Z) и переноса в центр части. */
    private static float[][] corners(DeathPhysics.Part p, float w, float h, float d) {
        float[][] out = new float[8][3];
        for (int i = 0; i < 8; i++) {
            float x = (i & 1) == 0 ? -w : w;
            float y = (i & 2) == 0 ? -h : h;
            float z = (i & 4) == 0 ? -d : d;
            // индексация: бит0 = x, бит1 = y, бит2 = z; пересобираем в порядок из draw()
            out[i] = rotate(p, x, y, z);
        }
        return remap(out);
    }

    private static float[][] remap(float[][] bits) {
        // bits[i]: i = x | y<<1 | z<<2. Нужен порядок 0:(-,-,-) 1:(+,-,-) 2:(+,+,-) 3:(-,+,-) 4:(-,-,+) 5:(+,-,+) 6:(+,+,+) 7:(-,+,+)
        return new float[][]{bits[0], bits[1], bits[3], bits[2], bits[4], bits[5], bits[7], bits[6]};
    }

    private static float[] rotate(DeathPhysics.Part p, float x, float y, float z) {
        float cy = (float) Math.cos(p.ry), sy = (float) Math.sin(p.ry);
        float x1 = x * cy + z * sy, z1 = -x * sy + z * cy;
        float cx = (float) Math.cos(p.rx), sx = (float) Math.sin(p.rx);
        float y2 = y * cx - z1 * sx, z2 = y * sx + z1 * cx;
        float cz = (float) Math.cos(p.rz), sz = (float) Math.sin(p.rz);
        float x3 = x1 * cz - y2 * sz, y3 = x1 * sz + y2 * cz;
        return new float[]{p.x + x3, p.y + y3, p.z + z2};
    }

    private static void face(MeshBuilder m, float[][] c, int a, int b, int cc, int dd, float u, float v,
                             float uw, float vh, int rgb, float alpha, float shade, boolean textured) {
        int argb = argb(rgb, alpha, shade);
        float u0 = u / TEX, u1 = (u + uw) / TEX, v0 = v / TEX, v1 = (v + vh) / TEX;
        // a: низ-слева, b: низ-справа, cc: верх-справа, dd: верх-слева (для боковых граней)
        quad(m, c[a], c[b], c[cc], c[dd], u0, v1, u1, v1, u1, v0, u0, v0, argb, textured);
    }

    private static void quad(MeshBuilder m, float[] p1, float[] p2, float[] p3, float[] p4,
                             float ua, float va, float ub, float vb, float uc, float vc, float ud, float vd,
                             int argb, boolean textured) {
        m.ensureQuadCapacity();
        RenderColor col = new RenderColor(argb);
        int i1 = vertex(m, p1, ua, va, col, textured);
        int i2 = vertex(m, p2, ub, vb, col, textured);
        int i3 = vertex(m, p3, uc, vc, col, textured);
        int i4 = vertex(m, p4, ud, vd, col, textured);
        m.quad(i1, i2, i3, i4);
    }

    private static int vertex(MeshBuilder m, float[] p, float u, float v, RenderColor col, boolean textured) {
        var b = m.vec3(p[0], p[1], p[2]);
        if (textured) b = b.vec2(u, v);
        return b.color(col).next();
    }

    private static int argb(int rgb, float alpha, float shade) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0f)));
        int r = Math.round(((rgb >> 16) & 0xFF) * shade);
        int g = Math.round(((rgb >> 8) & 0xFF) * shade);
        int bl = Math.round((rgb & 0xFF) * shade);
        return (a << 24) | (r << 16) | (g << 8) | bl;
    }
}
