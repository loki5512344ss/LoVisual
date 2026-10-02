package dev.loki.lovisual.features.module.modules.visuals.weather.blizzard;

import java.util.Random;

/**
 * Чистая физика снежинок по образцу evo: спавн в цилиндре вокруг камеры между землёй (карта высот) и потолком
 * над головой, ветер с порывами, турбулентность, рецикл при выходе из зоны или касании земли.
 * Под крышей земля выше потолка спавна, поэтому снега внутри помещений нет. Без зависимостей от Minecraft.
 */
final class SnowFlakes {

    static final int MAX_FLAKES = 12000;
    static final float FALL_SPEED = 1.6f;
    static final float AREA_FACTOR = 9.0f;
    static final double SPAWN_CEILING = 8.0;
    static final double SPAWN_FLOOR_MARGIN = 6.0;
    static final double RECYCLE_MARGIN = 1.25;
    static final double RECYCLE_HEIGHT = 16.0;
    static final int SPAWN_ATTEMPTS = 3;
    static final float WIND_SPEED_MAX = 16.0f;
    static final float TURBULENCE_AMPLITUDE = 1.1f;
    static final float RISE_PER_SECOND = 0.45f;
    static final float FALL_PER_SECOND = 0.6f;
    static final float EXPOSURE_PER_SECOND = 1.4f;

    /** Высота верхнего блока, который останавливает снег, в колонке (x, z) (карта MOTION_BLOCKING). */
    @FunctionalInterface
    interface Ground {
        float height(int x, int z);
    }

    /** Ветер: горизонтальная скорость и лёгкий вертикальный снос. */
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
    private final float[] ground = new float[MAX_FLAKES];
    private final int[] columnX = new int[MAX_FLAKES];
    private final int[] columnZ = new int[MAX_FLAKES];
    int count;

    private final Random random = new Random(1L);

    void clear() {
        count = 0;
    }

    /** Фиксация seed: детерминизм для тестов. */
    void reseed(long seed) {
        random.setSeed(seed);
    }

    /** Шаг к цели с постоянной скоростью, без перелёта. */
    static float approach(float current, float target, float perSecond, float dt) {
        float step = perSecond * dt;
        if (Math.abs(target - current) <= step) return target;
        return current + (target > current ? step : -step);
    }

    /** Сила шторма: разгон 0.45/с, затухание 0.6/с. */
    static float ramp(float current, float target, float dt) {
        return approach(current, target, target > current ? RISE_PER_SECOND : FALL_PER_SECOND, dt);
    }

    /** Цель экспозиции: 1 на открытом небе (камера не ниже верхнего блока колонки), 0 под крышей. */
    static float exposureTarget(double camY, float groundHeight) {
        return camY < groundHeight ? 0.0f : 1.0f;
    }

    /**
     * Целевая сила шторма. Без «только в снегопад» это просто интенсивность; иначе шторм идёт, пока в биоме
     * игрока снег и идёт осадок, и растёт с его силой.
     */
    static float targetStrength(float intensity, boolean onlyInSnowfall, float rainLevel, boolean snowBiome) {
        if (!onlyInSnowfall) return intensity;
        if (rainLevel <= 0.02f || !snowBiome) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, rainLevel)) * intensity;
    }

    /** Целевое число частиц: площадь цилиндра * плотность * сила шторма, не больше {@link #MAX_FLAKES}. */
    static int targetCount(float radius, float density, float strength) {
        if (radius <= 0f || density <= 0f || strength <= 0f) return 0;
        double raw = Math.PI * radius * radius * AREA_FACTOR * density * strength;
        return (int) Math.min(MAX_FLAKES, raw);
    }

    /** Ветер с порывами: направление медленно поворачивает, скорость дышит двумя синусами. */
    static Wind wind(double seconds, float windPower, float strength) {
        if (strength <= 0f || windPower <= 0f) return Wind.ZERO;
        double angle = seconds * 0.02 + Math.sin(seconds * 0.9 * 0.5) * 0.35;
        double gust = 1.0 + Math.sin(seconds * 0.9) * 0.35 + Math.sin(seconds * 2.3) * 0.15;
        double speed = windPower * WIND_SPEED_MAX * gust * strength;
        return new Wind(Math.cos(angle) * speed, Math.sin(seconds * 2.3) * speed * 0.12, Math.sin(angle) * speed);
    }

    /** Амплитуда боковой турбулентности (блоков/с) по настройке 0..1. */
    static float turbulence(float setting) {
        return Math.max(0f, setting) * TURBULENCE_AMPLITUDE;
    }

    void update(float dt, double camX, double camY, double camZ, float radius, Wind wind, float turbulence,
                int target, double seconds, Ground ground) {
        if (dt <= 0f || target < 0) return;
        target = Math.min(target, MAX_FLAKES);
        if (count > target) count = target;
        while (count < target && respawn(count, camX, camY, camZ, radius, wind, ground)) count++;
        double recycle = radius * RECYCLE_MARGIN;
        double recycleSq = recycle * recycle;
        for (int i = count - 1; i >= 0; i--) {
            float wobbleX = (float) Math.sin(seconds * rate[i] + phase[i]) * turbulence;
            float wobbleZ = (float) Math.cos(seconds * rate[i] * 0.83f + phase[i] * 1.7f) * turbulence;
            float nvx = (float) wind.x() + wobbleX;
            float nvy = (float) wind.y() - FALL_SPEED * fall[i];
            float nvz = (float) wind.z() + wobbleZ;
            double nx = x[i] + nvx * dt;
            double ny = y[i] + nvy * dt;
            double nz = z[i] + nvz * dt;
            int cx = (int) Math.floor(nx);
            int cz = (int) Math.floor(nz);
            if (cx != columnX[i] || cz != columnZ[i]) {
                columnX[i] = cx;
                columnZ[i] = cz;
                this.ground[i] = ground.height(cx, cz);
            }
            double dx = nx - camX;
            double dz = nz - camZ;
            boolean lost = ny <= this.ground[i] || dx * dx + dz * dz > recycleSq || ny > camY + RECYCLE_HEIGHT;
            if (lost) {
                if (!respawn(i, camX, camY, camZ, radius, wind, ground)) remove(i);
                continue;
            }
            x[i] = nx;
            y[i] = ny;
            z[i] = nz;
            vx[i] = nvx;
            vy[i] = nvy;
            vz[i] = nvz;
            age[i] += dt;
        }
    }

    private void remove(int i) {
        int last = --count;
        if (i == last) return;
        x[i] = x[last];
        y[i] = y[last];
        z[i] = z[last];
        vx[i] = vx[last];
        vy[i] = vy[last];
        vz[i] = vz[last];
        fall[i] = fall[last];
        size[i] = size[last];
        alpha[i] = alpha[last];
        phase[i] = phase[last];
        rate[i] = rate[last];
        age[i] = age[last];
        ground[i] = ground[last];
        columnX[i] = columnX[last];
        columnZ[i] = columnZ[last];
    }

    /** Ставит снежинку в случайное место цилиндра между землёй и потолком; false, если над колонкой крыша. */
    private boolean respawn(int i, double camX, double camY, double camZ, float radius, Wind wind, Ground world) {
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = radius * Math.sqrt(random.nextDouble());
            double px = camX + Math.cos(angle) * dist;
            double pz = camZ + Math.sin(angle) * dist;
            int cx = (int) Math.floor(px);
            int cz = (int) Math.floor(pz);
            float height = world.height(cx, cz);
            double ceiling = camY + SPAWN_CEILING;
            double floor = Math.max(height, camY - SPAWN_FLOOR_MARGIN);
            if (ceiling <= floor) continue;
            x[i] = px;
            y[i] = floor + random.nextDouble() * (ceiling - floor);
            z[i] = pz;
            fall[i] = 0.6f + random.nextFloat() * 0.9f;
            size[i] = 0.55f + random.nextFloat() * 0.9f;
            alpha[i] = 0.45f + random.nextFloat() * 0.55f;
            phase[i] = random.nextFloat() * (float) (Math.PI * 2.0);
            rate[i] = 1.2f + random.nextFloat() * 2.2f;
            age[i] = 0f;
            vx[i] = (float) wind.x();
            vy[i] = -FALL_SPEED;
            vz[i] = (float) wind.z();
            ground[i] = height;
            columnX[i] = cx;
            columnZ[i] = cz;
            return true;
        }
        return false;
    }
}
