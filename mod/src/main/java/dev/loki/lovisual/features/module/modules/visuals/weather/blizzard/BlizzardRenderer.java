package dev.loki.lovisual.features.module.modules.visuals.weather.blizzard;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Рендер снежинок по образцу evo: мягкая «капля» из двух воздушных змеев, вытянутая вдоль скорости,
 * непрозрачный центр и прозрачные края. Снежинки позади камеры отбрасываются.
 */
final class BlizzardRenderer {

    static final float FLAKE_RADIUS = 0.028f;
    static final float MIN_ANGULAR_RADIUS = 0.0024f;
    static final float STREAK_TIME = 0.022f;
    static final float STREAK_FRONT_SHARE = 0.3f;
    static final float FADE_IN_SECONDS = 0.45f;
    static final float NEAR_FADE_START = 0.25f;
    static final float NEAR_FADE_SPAN = 0.65f;
    static final float BEHIND_DOT = -0.35f;
    static final float BEHIND_MIN_DISTANCE = 3.0f;

    private BlizzardRenderer() {
    }

    static void render(MeshBuilder mesh, SnowFlakes flakes, Quaternionf camRot, double camX, double camY, double camZ,
                       int argb, float strength, float intensity, float sizeSetting, float opacity) {
        if (mesh == null || flakes.count == 0 || strength <= 0.001f || opacity <= 0.004f) return;
        int rgb = argb & 0x00FFFFFF;
        Vector3f forward = new Vector3f(0.0f, 0.0f, -1.0f).rotate(camRot);
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f).rotate(camRot);
        for (int i = 0; i < flakes.count; i++) {
            float px = (float) (flakes.x[i] - camX);
            float py = (float) (flakes.y[i] - camY);
            float pz = (float) (flakes.z[i] - camZ);
            float dist = (float) Math.sqrt(px * px + py * py + pz * pz);
            if (dist <= 0.0f) continue;
            float vx = px / dist, vy = py / dist, vz = pz / dist;
            if (isBehind(vx * forward.x + vy * forward.y + vz * forward.z, dist)) continue;
            float alpha = flakeAlpha(dist, flakes.age[i], flakes.alpha[i], opacity, strength, intensity);
            if (alpha <= 0.0f) continue;

            float speed = (float) Math.sqrt(flakes.vx[i] * flakes.vx[i] + flakes.vy[i] * flakes.vy[i]
                    + flakes.vz[i] * flakes.vz[i]);
            float dx, dy, dz;
            if (speed <= 1.0E-4f) {
                dx = up.x;
                dy = up.y;
                dz = up.z;
            } else {
                dx = flakes.vx[i] / speed;
                dy = flakes.vy[i] / speed;
                dz = flakes.vz[i] / speed;
            }
            float sx = dy * vz - dz * vy, sy = dz * vx - dx * vz, sz = dx * vy - dy * vx;
            float sideLen = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (sideLen <= 1.0E-4f) {
                sx = up.y * vz - up.z * vy;
                sy = up.z * vx - up.x * vz;
                sz = up.x * vy - up.y * vx;
                sideLen = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
                if (sideLen <= 1.0E-4f) continue;
            }
            sx /= sideLen;
            sy /= sideLen;
            sz /= sideLen;

            float radius = radius(sizeSetting, flakes.size[i], dist);
            float back = streakLength(radius, speed, flakes.size[i]);
            float front = frontLength(radius, back);
            RenderColor center = new RenderColor(withAlpha(rgb, alpha));
            RenderColor edge = new RenderColor(withAlpha(rgb, 0.0f));
            emit(mesh, px, py, pz, sx * radius, sy * radius, sz * radius, dx * front, dy * front, dz * front,
                    center, edge);
            emit(mesh, px, py, pz, -sx * radius, -sy * radius, -sz * radius, -dx * back, -dy * back, -dz * back,
                    center, edge);
        }
    }

    /** Один змей: центр непрозрачный, сторона, острие и противоположная сторона прозрачные. */
    private static void emit(MeshBuilder mesh, float cx, float cy, float cz, float sx, float sy, float sz,
                             float tx, float ty, float tz, RenderColor center, RenderColor edge) {
        mesh.ensureQuadCapacity();
        int a = mesh.vec3(cx, cy, cz).color(center).next();
        int b = mesh.vec3(cx - sx, cy - sy, cz - sz).color(edge).next();
        int c = mesh.vec3(cx + tx, cy + ty, cz + tz).color(edge).next();
        int d = mesh.vec3(cx + sx, cy + sy, cz + sz).color(edge).next();
        mesh.quad(a, b, c, d);
    }

    /** Снежинки позади камеры не рисуются, кроме совсем близких (не пропадают на краю экрана). */
    static boolean isBehind(float dotWithForward, float dist) {
        return dist > BEHIND_MIN_DISTANCE && dotWithForward < BEHIND_DOT;
    }

    /**
     * Видимость снежинки: невидима вплотную к камере, плавно проявляется с расстоянием и возрастом;
     * масштабируется прозрачностью и долей силы шторма от интенсивности.
     */
    static float flakeAlpha(float dist, float ageSeconds, float baseAlpha, float opacity, float strength,
                            float intensity) {
        float near = clamp01((dist - NEAR_FADE_START) / NEAR_FADE_SPAN);
        float born = Math.min(ageSeconds / FADE_IN_SECONDS, 1.0f);
        float share = clamp01(strength / Math.max(intensity, 0.01f));
        return opacity * share * baseAlpha * near * Math.max(0.0f, born);
    }

    /** Радиус снежинки: вдали не меньше угловых 0.0024, чтобы не пропадала. */
    static float radius(float sizeSetting, float flakeSize, float dist) {
        return Math.max(FLAKE_RADIUS * sizeSetting * flakeSize, dist * MIN_ANGULAR_RADIUS);
    }

    /** Длина хвоста за снежинкой: растёт со скоростью. */
    static float streakLength(float radius, float speed, float flakeSize) {
        return radius + speed * STREAK_TIME * flakeSize;
    }

    /** Длина носа перед снежинкой: короче хвоста, у медленной равна радиусу. */
    static float frontLength(float radius, float back) {
        return back * STREAK_FRONT_SHARE + radius * (1.0f - STREAK_FRONT_SHARE);
    }

    static int withAlpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0f)));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : Math.min(v, 1.0f);
    }
}
