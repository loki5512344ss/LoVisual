package dev.loki.lovisual.features.module.modules.visuals.effects.hits.death;

import java.util.Random;

/** Чистая физика частей тела для режимов смерти (Shatter, Ascend, Explosion, Disintegrate); без Minecraft. */
public final class DeathPhysics {

    public enum Style { SHATTER, ASCEND, EXPLOSION, DISINTEGRATE }

    /** Часть тела: центр, скорость, углы Эйлера (рад), угловая скорость, половинные размеры, задержка. */
    public static final class Part {
        public float x, y, z, vx, vy, vz;
        public float rx, ry, rz, wx, wy, wz;
        public final float hw, hh, hd;
        public final int kind;
        public float delay;
        public float phase;
        boolean resting;

        public Part(int kind, float x, float y, float z, float hw, float hh, float hd) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.z = z;
            this.hw = hw;
            this.hh = hh;
            this.hd = hd;
        }

        float radius() {
            return Math.max(hw, Math.max(hh, hd));
        }
    }

    private static final float GRAVITY = 16.0f;

    private DeathPhysics() {
    }

    /** Начальный импульс части относительно центра тела (cx, cy, cz). */
    public static void launch(Part p, Style style, float power, float spin, float duration,
                              float cx, float cy, float cz, Random r) {
        float dx = p.x - cx + (r.nextFloat() - 0.5f) * 0.2f;
        float dy = p.y - cy;
        float dz = p.z - cz + (r.nextFloat() - 0.5f) * 0.2f;
        float len = Math.max(0.05f, (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
        p.phase = r.nextFloat() * 6.2831855f;
        switch (style) {
            case SHATTER -> {
                float k = (1.5f + r.nextFloat() * 1.5f) * power;
                set(p, dx / len * k, 2.5f * power + dy / len * k, dz / len * k, spin * 7.0f, r);
            }
            case EXPLOSION -> {
                float k = (5.0f + r.nextFloat() * 3.0f) * power;
                set(p, dx / len * k, 3.0f * power + dy / len * k * 0.6f, dz / len * k, spin * 12.0f, r);
            }
            case ASCEND -> set(p, 0.0f, (0.5f + r.nextFloat() * 0.4f) * power, 0.0f, spin * 1.2f, r);
            default -> {
                set(p, 0.0f, 0.0f, 0.0f, spin * 0.5f, r);
                p.delay = Math.max(0.0f, (2.0f - p.y) / 2.0f) * duration * 0.45f;
            }
        }
    }

    private static void set(Part p, float vx, float vy, float vz, float spin, Random r) {
        p.vx = vx;
        p.vy = vy;
        p.vz = vz;
        p.wx = (r.nextFloat() - 0.5f) * 2.0f * spin;
        p.wy = (r.nextFloat() - 0.5f) * 2.0f * spin;
        p.wz = (r.nextFloat() - 0.5f) * 2.0f * spin;
    }

    /** Шаг физики; t: секунды от начала эффекта, groundY: уровень пола, bounce: отскок от земли. */
    public static void step(Part p, Style style, float dt, float t, float groundY, boolean bounce) {
        if (p.resting) return;
        switch (style) {
            case ASCEND -> {
                p.x += (float) Math.sin(t * 2.0f + p.phase) * 0.25f * dt;
                p.y += p.vy * dt;
                spinStep(p, dt);
            }
            case DISINTEGRATE -> {
                if (t > p.delay) p.y += 0.6f * dt;
                spinStep(p, dt);
            }
            default -> ballistic(p, dt, groundY, bounce);
        }
    }

    private static void ballistic(Part p, float dt, float groundY, boolean bounce) {
        p.vy -= GRAVITY * dt;
        float drag = (float) Math.pow(0.985, dt * 60.0);
        p.vx *= drag;
        p.vz *= drag;
        p.x += p.vx * dt;
        p.y += p.vy * dt;
        p.z += p.vz * dt;
        spinStep(p, dt);
        float floor = groundY + p.radius();
        if (p.y >= floor) return;
        p.y = floor;
        if (bounce && Math.abs(p.vy) > 1.0f) {
            p.vy = -p.vy * 0.45f;
            p.vx *= 0.8f;
            p.vz *= 0.8f;
            p.wx *= 0.7f;
            p.wy *= 0.7f;
            p.wz *= 0.7f;
        } else {
            p.vx = p.vy = p.vz = 0.0f;
            p.wx = p.wy = p.wz = 0.0f;
            p.resting = true;
        }
    }

    private static void spinStep(Part p, float dt) {
        p.rx += p.wx * dt;
        p.ry += p.wy * dt;
        p.rz += p.wz * dt;
    }

    /** Непрозрачность части в момент t (0..1). */
    public static float alpha(Part p, Style style, float t, float duration) {
        float a = switch (style) {
            case SHATTER -> 1.0f - ramp(t, duration * 0.6f, duration * 0.4f);
            case EXPLOSION -> 1.0f - ramp(t, duration * 0.4f, duration * 0.6f);
            case ASCEND -> 1.0f - ramp(t, duration * 0.3f, duration * 0.7f);
            default -> 1.0f - ramp(t, p.delay, duration * 0.35f);
        };
        return Math.max(0.0f, Math.min(1.0f, a));
    }

    private static float ramp(float t, float start, float length) {
        return Math.max(0.0f, Math.min(1.0f, (t - start) / Math.max(0.001f, length)));
    }
}
