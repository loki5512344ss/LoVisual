package dev.loki.lovisual.features.module.modules.visuals.effects.hits.blood;

import dev.loki.lovisual.features.module.modules.visuals.effects.hits.blood.BloodParticles.Drop;
import dev.loki.lovisual.features.module.modules.visuals.effects.hits.blood.BloodParticles.Splat;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.world.phys.Vec3;

import static dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspRender.addVertex;

/** Builds the triangles of a teardrop (drop) and a ragged puddle (splat), plus color helpers. */
final class BloodGeometry {
    private static final int RING = 6;
    private static final int SPLAT_SEGMENTS = 14;

    private BloodGeometry() {
    }

    /** Teardrop: a 6-sided ring with a front cap and a speed-stretched, fading tail. */
    static void drop(MeshBuilder mesh, Vec3 p, Drop d) {
        double speed = d.vel.length();
        Vec3 dir = speed < 1.0E-4 ? new Vec3(0, -1, 0) : d.vel.scale(1.0 / speed);
        float r = d.size * 0.5f;
        float head = r * 0.9f;
        float tail = r * (1.6f + (float) Math.min(speed, 0.9) * 5.5f);
        Vec3 up = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 s1 = dir.cross(up).normalize();
        Vec3 s2 = dir.cross(s1).normalize();
        Vec3 front = p.add(dir.scale(head));
        Vec3 back = p.subtract(dir.scale(tail));
        Vec3 center = p.add(dir.scale(head * 0.15));
        for (int i = 0; i < RING; i++) {
            int j = (i + 1) % RING;
            Vec3 n1 = ringNormal(s1, s2, i);
            Vec3 n2 = ringNormal(s1, s2, j);
            Vec3 n = n1.add(n2).normalize();
            int cf = lit(d.color, n.add(dir.scale(0.6)).normalize(), d.light, true);
            int cb = lit(d.color, n.add(dir.scale(-0.3)).normalize(), d.light, false);
            tri(mesh, front, center.add(n1.scale(r)), center.add(n2.scale(r)), cf, cf, cf);
            tri(mesh, center.add(n1.scale(r)), back, center.add(n2.scale(r)), cb, withAlpha(shade(cb, 0.8f), 150), cb);
        }
    }

    /** Puddle: grows quickly, then fades and darkens as it dries. Returns false when finished. */
    static boolean splat(MeshBuilder mesh, Splat s, long nowMs, float lifeMs) {
        long age = nowMs - s.startMs();
        float life = age / lifeMs;
        float fade = 1.0f - smooth((life - 0.7f) / 0.3f);
        if (life >= 1.0f || fade <= 0.01f) return false;
        float grow = 1.0f - (float) Math.pow(1.0f - Math.min(1f, age / 260f), 3);
        float dry = smooth(life / 0.8f);
        float radius = s.radius() * (0.35f + 0.65f * grow);
        int base = shade(s.color(), s.light() * (1.0f - 0.45f * dry));
        int centerCol = withAlpha(shade(base, 0.72f), Math.round(235 * fade));
        int edgeCol = withAlpha(base, Math.round(205 * fade));
        int rimCol = withAlpha(base, 0);
        Vec3 c = new Vec3(s.pos().x, s.pos().y + 0.006 + s.layer() * 0.0004, s.pos().z);
        for (int i = 0; i < SPLAT_SEGMENTS; i++) {
            int j = (i + 1) % SPLAT_SEGMENTS;
            float r1 = radius * (0.72f + noise(s.seed(), i) * 0.5f);
            float r2 = radius * (0.72f + noise(s.seed(), j) * 0.5f);
            double a1 = i * Math.PI * 2.0 / SPLAT_SEGMENTS;
            double a2 = j * Math.PI * 2.0 / SPLAT_SEGMENTS;
            Vec3 p1 = c.add(Math.cos(a1) * r1, 0, Math.sin(a1) * r1);
            Vec3 p2 = c.add(Math.cos(a2) * r2, 0, Math.sin(a2) * r2);
            Vec3 o1 = c.add(Math.cos(a1) * r1 * 1.18, 0, Math.sin(a1) * r1 * 1.18);
            Vec3 o2 = c.add(Math.cos(a2) * r2 * 1.18, 0, Math.sin(a2) * r2 * 1.18);
            tri(mesh, c, p1, p2, centerCol, edgeCol, edgeCol);
            tri(mesh, p1, o1, o2, edgeCol, rimCol, rimCol);
            tri(mesh, p1, o2, p2, edgeCol, rimCol, edgeCol);
        }
        return true;
    }

    private static Vec3 ringNormal(Vec3 s1, Vec3 s2, int i) {
        double a = i * Math.PI * 2.0 / RING;
        return s1.scale(Math.cos(a)).add(s2.scale(Math.sin(a)));
    }

    private static void tri(MeshBuilder m, Vec3 a, Vec3 b, Vec3 c, int ca, int cb, int cc) {
        m.ensureTriCapacity();
        m.triangle(addVertex(m, a.x, a.y, a.z, ca), addVertex(m, b.x, b.y, b.z, cb), addVertex(m, c.x, c.y, c.z, cc));
    }

    private static int lit(int color, Vec3 normal, float light, boolean highlight) {
        double dot = Math.max(0.0, normal.x * 0.35 + normal.y * 0.85 + normal.z * 0.25);
        int c = shade(color, (float) (0.45 + 0.75 * dot) * light);
        return highlight && dot > 0.8 ? mix(c, 0xFFFF9A9A, (float) ((dot - 0.8) / 0.2) * 0.45f * light) : c;
    }

    static int shade(int color, float k) {
        int r = Math.min(255, Math.round(((color >> 16) & 255) * k));
        int g = Math.min(255, Math.round(((color >> 8) & 255) * k));
        int b = Math.min(255, Math.round((color & 255) * k));
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    static int mix(int first, int second, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        int r = Math.round(((first >> 16) & 255) * (1 - k) + ((second >> 16) & 255) * k);
        int g = Math.round(((first >> 8) & 255) * (1 - k) + ((second >> 8) & 255) * k);
        int b = Math.round((first & 255) * (1 - k) + (second & 255) * k);
        return (first & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    static float smooth(float v) {
        float t = Math.max(0f, Math.min(1f, v));
        return t * t * (3f - 2f * t);
    }

    /** Deterministic per-puddle noise in [0, 1]. */
    static float noise(long seed, int salt) {
        long v = seed + 0x9E3779B97F4A7C15L * (salt + 1L);
        v = (v ^ (v >>> 30)) * 0xBF58476D1CE4E5B9L;
        v = (v ^ (v >>> 27)) * 0x94D049BB133111EBL;
        return ((v ^ (v >>> 31)) >>> 40) / 16777215.0f;
    }
}
