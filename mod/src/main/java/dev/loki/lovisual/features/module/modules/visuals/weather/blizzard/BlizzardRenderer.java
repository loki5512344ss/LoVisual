package dev.loki.lovisual.features.module.modules.visuals.weather.blizzard;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Рендер снежинок: биллборд-квады, вытянутые по вектору скорости частицы,
 * с затуханием у камеры и на границе зоны.
 */
final class BlizzardRenderer {

    private BlizzardRenderer() {
    }

    static void render(MeshBuilder mesh, SnowFlakes flakes, Quaternionf camRot,
                       double camX, double camY, double camZ, float radius,
                       int argb, float strength, float sizeMul, float opacity) {
        if (mesh == null || flakes.count == 0 || strength <= 0.001f || opacity <= 0.004f) return;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f).rotate(camRot);
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f).rotate(camRot);
        Vector3f forward = new Vector3f(0.0f, 0.0f, -1.0f).rotate(camRot);
        for (int i = 0; i < flakes.count; i++) {
            double dx = flakes.x[i] - camX;
            double dy = flakes.y[i] - camY;
            double dz = flakes.z[i] - camZ;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist < 0.2f) continue;
            float facing = (float) (dx * forward.x + dy * forward.y + dz * forward.z);
            if (facing <= 0.0f) continue;

            float speedX = flakes.vx[i];
            float speedY = flakes.vy[i];
            float speedZ = flakes.vz[i];
            float along = right.x * speedX + right.y * speedY + right.z * speedZ;
            float across = up.x * speedX + up.y * speedY + up.z * speedZ;
            float alongLen = (float) Math.sqrt(along * along + across * across);
            if (alongLen < 1.0E-4f) {
                along = 0.0f;
                across = -1.0f;
                alongLen = 1.0f;
            }
            along /= alongLen;
            across /= alongLen;
            // Единичный вектор скорости в плоскости биллборда.
            float dirX = right.x * along + up.x * across;
            float dirY = right.y * along + up.y * across;
            float dirZ = right.z * along + up.z * across;
            // Поперёк: forward x dir (оба перпендикулярны друг другу).
            float sideX = forward.y * dirZ - forward.z * dirY;
            float sideY = forward.z * dirX - forward.x * dirZ;
            float sideZ = forward.x * dirY - forward.y * dirX;
            float sideLen = (float) Math.sqrt(sideX * sideX + sideY * sideY + sideZ * sideZ);
            if (sideLen < 1.0E-4f) continue;
            sideX /= sideLen;
            sideY /= sideLen;
            sideZ /= sideLen;

            float speed = (float) Math.sqrt(speedX * speedX + speedY * speedY + speedZ * speedZ);
            float halfWidth = halfWidth(flakes.size[i], sizeMul, dist);
            float halfLength = halfLength(halfWidth, speed, sizeMul);
            float alpha = flakeAlpha(dist, flakes.age[i], flakes.alpha[i], radius, opacity, strength);
            if (alpha <= 0.0f) continue;

            float cx = (float) flakes.x[i];
            float cy = (float) flakes.y[i];
            float cz = (float) flakes.z[i];
            float ax = dirX * halfLength;
            float ay = dirY * halfLength;
            float az = dirZ * halfLength;
            float bx = sideX * halfWidth;
            float by = sideY * halfWidth;
            float bz = sideZ * halfWidth;
            RenderColor color = new RenderColor(((int) (alpha * 255.0f) << 24) | (r << 16) | (g << 8) | b);
            mesh.ensureQuadCapacity();
            int i1 = mesh.vec3(cx - ax - bx, cy - ay - by, cz - az - bz).color(color).next();
            int i2 = mesh.vec3(cx + ax - bx, cy + ay - by, cz + az - bz).color(color).next();
            int i3 = mesh.vec3(cx + ax + bx, cy + ay + by, cz + az + bz).color(color).next();
            int i4 = mesh.vec3(cx - ax + bx, cy - ay + by, cz - az + bz).color(color).next();
            mesh.quad(i1, i2, i3, i4);
        }
    }

    /**
     * Видимость снежинки: невидима у самой линзы, плавно проявляется с расстоянием и возрастом,
     * тает к границе зоны шторма; масштабируется прозрачностью и силой шторма, не бывает полностью непрозрачной.
     * Слишком слабые снежинки возвращают 0, чтобы их не рисовать.
     */
    static float flakeAlpha(float dist, float ageSeconds, float baseAlpha, float radius, float opacity, float strength) {
        float edgeStart = radius * 0.62f;
        float edgeSpan = Math.max(0.001f, radius * 1.05f - edgeStart);
        float edge = 1.0f - clamp01((dist - edgeStart) / edgeSpan);
        float near = clamp01((dist - 0.35f) / 0.5f);
        float ageFade = Math.min(1.0f, ageSeconds / 0.45f);
        float alpha = opacity * strength * baseAlpha * edge * near * ageFade;
        if (alpha <= 0.008f) return 0.0f;
        return Math.min(0.95f, alpha);
    }

    /** Полуширина снежинки; вдали не тоньше доли пикселя, чтобы не пропадала. */
    static float halfWidth(float flakeSize, float sizeMul, float dist) {
        float hw = (float) (0.045 * flakeSize) * sizeMul;
        return Math.max(hw, dist * 0.0016f);
    }

    /** Полудлина штриха: быстрее снежинка, длиннее след, но не больше 0.35 блока. */
    static float halfLength(float halfWidth, float speed, float sizeMul) {
        return halfWidth + Math.min(0.35f, speed * 0.022f * sizeMul);
    }

    private static float clamp01(float v) {
        if (v < 0.0f) return 0.0f;
        return v > 1.0f ? 1.0f : v;
    }
}
