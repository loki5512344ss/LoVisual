package dev.loki.lovisual.features.module.modules.visuals.weather.blizzard;

import java.util.Random;

/**
 * Чистая физика снежинок: спавн в цилиндре вокруг камеры, ветер с порывами,
 * турбулентность и рецикл при выходе из зоны. Без зависимостей от Minecraft.
 */
final class SnowFlakes {

    static final int MAX_FLAKES = 4000;
    static final float FALL_SPEED = 1.6f;
    static final float AREA_FACTOR = 3.0f;
    static final double SPAWN_ABOVE = 8.0;
    static final double SPAWN_BELOW = 6.0;
    static final double RECYCLE_MARGIN = 1.25;

    /** Горизонтальный ветер с лёгким вертикальным сносом. */
    record Wind(double x, double y, double z) {
        static final Wind ZERO = new Wind(0.0, 0.0, 0.0);
    }

    final double[] x = new double[MAX_FLAKES];
    final double[] y = new double[MAX_FLAKES];
    final double[] z = new double[MAX_FLAKES];
    final float[] vx = new float[MAX_FLAKES];
    final float[] vy = new float[MAX_FLAKES];
    final float[] vz = new float[MAX_FLAKES];
    final float[] fall = new float[MAX_FLAKES];
    final float[] size = new float[MAX_FLAKES];
    final float[] alpha = new float[MAX_FLAKES];
    final float[] phase = new float[MAX_FLAKES];
    final float[] rate = new float[MAX_FLAKES];
    final float[] age = new float[MAX_FLAKES];
    int count;

    private final Random random = new Random(1L);

    void clear() {
        count = 0;
    }

    /** Фиксация seed — детерминизм для тестов. */
    void reseed(long seed) {
        random.setSeed(seed);
    }

    /** Плавный разгон (0.45/с) и затухание (0.6/с) силы шторма к цели, без перелёта. */
    static float ramp(float current, float target, float dt) {
        if (current < target) return Math.min(target, current + 0.45f * dt);
        return Math.max(target, current - 0.6f * dt);
    }

    /** Целевое число частиц: площадь цилиндра * плотность * доля силы шторма. */
    static int targetCount(float radius, float density, float strength) {
        if (radius <= 0f || density <= 0f || strength <= 0f) return 0;
        double raw = Math.PI * radius * radius * AREA_FACTOR * density * Math.min(1.0f, strength);
        if (raw >= MAX_FLAKES) return MAX_FLAKES;
        return (int) raw;
    }

    /** Ветер с порывами: направление медленно кружит, скорость дышит несколькими синусами. */
    static Wind wind(float seconds, float windPower, float strength) {
        if (strength <= 0f || windPower <= 0f) return Wind.ZERO;
        double angle = seconds * 0.17 + Math.sin(seconds * 0.45) * 0.35;
        double gust = 1.0 + Math.sin(seconds * 0.9) * 0.35 + Math.sin(seconds * 2.3) * 0.15;
        double speed = windPower * 14.0 * gust * strength;
        return new Wind(Math.cos(angle) * speed, Math.sin(seconds * 2.3) * speed * 0.12, Math.sin(angle) * speed);
    }

    /** Амплитуда боковой турбулентности, зависящей от мощности ветра. */
    static float turbulence(float windPower) {
        return 0.25f + 0.55f * Math.max(0f, windPower);
    }

    void update(float dt, double camX, double camY, double camZ, float radius,
                Wind wind, float turbulence, float speedMul, int target, float seconds) {
        if (dt <= 0f || target < 0) return;
        if (count > target) count = target;
        while (count < target) spawn(count++, camX, camY, camZ, radius);
        double recycle = radius * RECYCLE_MARGIN;
        double recycleSq = recycle * recycle;
        for (int i = 0; i < count; i++) {
            float wobbleX = (float) Math.sin(seconds * rate[i] + phase[i]) * turbulence * speedMul;
            float wobbleZ = (float) Math.cos(seconds * rate[i] * 0.83f + phase[i] * 1.7f) * turbulence * speedMul;
            vx[i] = (float) wind.x() + wobbleX;
            vy[i] = (float) wind.y() - FALL_SPEED * fall[i] * speedMul;
            vz[i] = (float) wind.z() + wobbleZ;
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
            z[i] += vz[i] * dt;
            age[i] += dt;
            double dx = x[i] - camX;
            double dz = z[i] - camZ;
            if (dx * dx + dz * dz > recycleSq
                    || y[i] < camY - SPAWN_BELOW
                    || y[i] > camY + SPAWN_ABOVE) {
                spawn(i, camX, camY, camZ, radius);
            }
        }
    }

    private void spawn(int i, double camX, double camY, double camZ, float radius) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double dist = radius * Math.sqrt(random.nextDouble());
        x[i] = camX + Math.cos(angle) * dist;
        z[i] = camZ + Math.sin(angle) * dist;
        y[i] = camY + SPAWN_ABOVE - random.nextDouble() * (SPAWN_ABOVE + SPAWN_BELOW);
        vx[i] = 0f;
        vy[i] = -FALL_SPEED;
        vz[i] = 0f;
        fall[i] = 0.6f + random.nextFloat() * 0.8f;
        size[i] = 0.75f + random.nextFloat() * 0.55f;
        alpha[i] = 0.35f + random.nextFloat() * 0.55f;
        phase[i] = random.nextFloat() * (float) (Math.PI * 2.0);
        rate[i] = 1.2f + random.nextFloat() * 2.2f;
        age[i] = 0f;
    }
}
