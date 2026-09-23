/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.animation.hand;

/**
 * Сборка итоговой позы руки из отдельных эффектов: sway от обзора, боб от
 * ходьбы, дрейф к центру при использовании предмета и «въезд» при входе в мир.
 * <p>
 * Конвенции знаков соответствуют пространству ванильного рендера руки:
 * +X — влево на экране (правая рука стоит при X&lt;0), +Y — вверх, +Z — к
 * зрителю; углы в градусах. Смещения — в условных блоках.
 * Класс чистый: без Minecraft-типов, покрыт юнит-тестами.
 */
public enum HandPoseMath {
    ;

    /** Нулевая поза — применять трансформы не нужно. */
    public static final Pose ZERO = new Pose(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);

    private static final float SWAY_POS_PER_DEG = 0.010f;
    private static final float SWAY_ROT_Y_PER_DEG = 1.4f;
    private static final float SWAY_ROT_X_PER_DEG = 1.1f;
    private static final float SWAY_ROT_Z_PER_DEG = 0.5f;

    private static final float BOB_POS_X = 0.045f;
    private static final float BOB_POS_Y = 0.035f;
    private static final float BOB_ROT_Z_DEG = 2.5f;
    private static final float BOB_ROT_X_DEG = 3.0f;

    private static final float DRIFT_POS_X = 0.10f;
    private static final float DRIFT_POS_Z = 0.05f;
    private static final float DRIFT_ROT_Y_DEG = 10.0f;
    private static final float DRIFT_ROT_Z_DEG = 9.0f;

    private static final float JOIN_DROP = 0.85f;
    private static final float JOIN_TILT_DEG = 18.0f;

    /**
     * Отставание от поворота камеры: камера повернула вправо (yaw+) — предмет
     * сместился влево на экране (+X) и довернулся назад (ry-), взгляд вниз
     * (pitch+) — предмет приподнялся (+Y) и задрался (rx-).
     */
    public static Pose sway(float yawLagDeg, float pitchLagDeg, float strength) {
        float s = scale(strength);
        if (s <= 0.0f) {
            return ZERO;
        }
        return new Pose(
                yawLagDeg * SWAY_POS_PER_DEG * s,
                pitchLagDeg * SWAY_POS_PER_DEG * s,
                0.0f,
                -pitchLagDeg * SWAY_ROT_X_PER_DEG * s,
                -yawLagDeg * SWAY_ROT_Y_PER_DEG * s,
                yawLagDeg * SWAY_ROT_Z_PER_DEG * s
        );
    }

    /** Покачивание при ходьбе: {@code phase} — позиция шага, {@code amplitude01} — 0..1 от скорости. */
    public static Pose bob(float phase, float amplitude01, float strength) {
        float a = clamp01(amplitude01) * scale(strength);
        if (a <= 0.0f) {
            return ZERO;
        }
        float lateral = BobMath.lateral(phase);
        float vertical = BobMath.vertical(phase);
        return new Pose(
                -lateral * BOB_POS_X * a,
                vertical * BOB_POS_Y * a,
                0.0f,
                vertical * BOB_ROT_X_DEG * a,
                0.0f,
                -lateral * BOB_ROT_Z_DEG * a
        );
    }

    /**
     * Дрейф используемого предмета (еда/зелье/щит) к центру экрана.
     * Правая рука тянется влево (+X), левая — вправо (-X), плюс лёгкий
     * наклон внутрь.
     */
    public static Pose drift(float progress01, float strength, boolean rightArm) {
        float s = clamp01(progress01) * scale(strength);
        if (s <= 0.0f) {
            return ZERO;
        }
        float side = rightArm ? 1.0f : -1.0f;
        return new Pose(
                side * DRIFT_POS_X * s,
                0.0f,
                -DRIFT_POS_Z * s,
                0.0f,
                -side * DRIFT_ROT_Y_DEG * s,
                side * DRIFT_ROT_Z_DEG * s
        );
    }

    /**
     * "Хват вдоль кулака" (grip) для предметов с рукоятью (см. {@link HeldAlongFist}):
     * лёгкий сдвиг ближе к камере/в сторону и поворот — имитирует держание меча/палки
     * не прямо по-вертикали ванильного equip, а слегка наискось в кулаке.
     * {@code side}: +1 — правая рука, -1 — левая.
     */
    public static Pose grip(float extend, float slide, float out, float forward,
                            float tilt, float turn, float roll, float side) {
        return new Pose(
                0.32f * side * extend + out * side,
                slide,
                -0.95f * extend - forward,
                tilt,
                turn * side,
                roll * side
        );
    }

    /** Появление при входе в мир: предмет въезжает снизу и распрямляется. */
    public static Pose joinDrop(float progress01) {
        float p = clamp01(progress01);
        float remain = 1.0f - p;
        if (remain <= 0.0f) {
            return ZERO;
        }
        float square = remain * remain;
        return new Pose(0.0f, -JOIN_DROP * square, 0.0f, JOIN_TILT_DEG * square, 0.0f, 0.0f);
    }

    /** Поэлементная сумма поз. */
    public static Pose add(Pose a, Pose b) {
        if (a == ZERO) return b;
        if (b == ZERO) return a;
        return new Pose(
                a.dx() + b.dx(),
                a.dy() + b.dy(),
                a.dz() + b.dz(),
                a.rx() + b.rx(),
                a.ry() + b.ry(),
                a.rz() + b.rz()
        );
    }

    static float clamp01(float v) {
        if (Float.isNaN(v)) return 0.0f;
        if (v < 0.0f) return 0.0f;
        if (v > 1.0f) return 1.0f;
        return v;
    }

    /**
     * Множитель силы настройки: линейный, без потолка — диапазон настроек 0..2
     * должен именно усиливать эффект, а не обрезаться. Отрицательные и
     * сломанные значения дают ноль.
     */
    static float scale(float strength) {
        if (Float.isNaN(strength) || Float.isInfinite(strength)) return 0.0f;
        return Math.max(0.0f, strength);
    }

    /**
     * Итоговая поза руки: смещение (dx, dy, dz) и поворот (rx, ry, rz в градусах),
     * применяемые вокруг точки взгляда до ванильных трансформов руки.
     */
    public record Pose(float dx, float dy, float dz, float rx, float ry, float rz) {

        /** Все компоненты по модулю не больше epsilon — трансформ можно не применять. */
        public boolean isSignificant(float epsilon) {
            return Math.abs(dx) > epsilon
                    || Math.abs(dy) > epsilon
                    || Math.abs(dz) > epsilon
                    || Math.abs(rx) > epsilon
                    || Math.abs(ry) > epsilon
                    || Math.abs(rz) > epsilon;
        }
    }
}
