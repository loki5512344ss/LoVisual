package dev.loki.lovisual.util.animation.hand.state;

/**
 * Накопитель «отставания» предмета от поворота камеры (view sway).
 * <p>
 * Модель: дельты взгляда за кадр (в градусах) накапливаются в отставание
 * {@code lag}, ограниченное сверху, и экспоненциально затухают к нулю
 * (RELAX). Отображаемое значение догоняет накопленное отставание
 * экспонентой (RESPONSE). Всё в градусах, время — в тиках, поэтому
 * поведение не зависит от частоты кадров.
 * <p>
 * Класс чистый: без Minecraft-типов, покрыт юнит-тестами.
 */
public final class SwayState {

    public static final float MAX_YAW_LAG_DEG = 6.0f;
    public static final float MAX_PITCH_LAG_DEG = 4.0f;

    /** Скорость догоняния отображаемого отставания, 1/тик. */
    static final float RESPONSE_PER_TICK = 6.0f;
    /** Скорость возврата накопленного отставания к нулю, 1/тик. */
    static final float RELAX_PER_TICK = 0.25f;
    /** Верхняя граница шага, чтобы пауза/лаг не давали рывок. */
    static final float MAX_DELTA_TICKS = 5.0f;

    /** Ниже этого всё состояние защёлкивается в точный ноль — конечное время затухания. */
    private static final float SETTLE_EPSILON = 1.0e-4f;

    private float yawLag;    // накопленное отставание по yaw, градусы
    private float pitchLag;  // накопленное отставание по pitch, градусы
    private float yawSmooth; // отображаемое отставание по yaw, градусы
    private float pitchSmooth;

    /**
     * Продвигает состояние на один кадр.
     *
     * @param deltaYawDeg   изменение yaw камеры за кадр (может быть любым по знаку)
     * @param deltaPitchDeg изменение pitch камеры за кадр
     * @param deltaTicks    реальное время кадра в тиках; неположительное пропускается
     */
    public void update(float deltaYawDeg, float deltaPitchDeg, float deltaTicks) {
        float dt = sanitize(deltaTicks);
        if (dt <= 0.0f) {
            return;
        }
        yawLag = clamp(yawLag + sanitize(deltaYawDeg), -MAX_YAW_LAG_DEG, MAX_YAW_LAG_DEG);
        pitchLag = clamp(pitchLag + sanitize(deltaPitchDeg), -MAX_PITCH_LAG_DEG, MAX_PITCH_LAG_DEG);

        float follow = 1.0f - (float) Math.exp(-RESPONSE_PER_TICK * dt);
        yawSmooth += (yawLag - yawSmooth) * follow;
        pitchSmooth += (pitchLag - pitchSmooth) * follow;

        float relax = (float) Math.exp(-RELAX_PER_TICK * dt);
        yawLag *= relax;
        pitchLag *= relax;

        if (Math.abs(yawLag) <= SETTLE_EPSILON && Math.abs(pitchLag) <= SETTLE_EPSILON
                && Math.abs(yawSmooth) <= SETTLE_EPSILON && Math.abs(pitchSmooth) <= SETTLE_EPSILON) {
            reset();
        }
    }

    /** Мгновенный сброс: предмет стоит ровно, без хвостов. */
    public void reset() {
        yawLag = 0.0f;
        pitchLag = 0.0f;
        yawSmooth = 0.0f;
        pitchSmooth = 0.0f;
    }

    /** Сглаженное отставание по yaw в градусах. */
    public float yawLag() {
        return yawSmooth;
    }

    /** Сглаженное отставание по pitch в градусах. */
    public float pitchLag() {
        return pitchSmooth;
    }

    /** Обе оси в нуле — считать можно не применять трансформ вовсе. */
    public boolean isSettled() {
        return yawLag == 0.0f && pitchLag == 0.0f && yawSmooth == 0.0f && pitchSmooth == 0.0f;
    }

    /**
     * Приводит разницу углов к диапазону [-180, 180): переход через
     * границу мира не даёт ложного рывка на 360 градусов.
     */
    public static float wrapDegrees(float degrees) {
        float d = sanitize(degrees) % 360.0f;
        if (d >= 180.0f) {
            d -= 360.0f;
        }
        if (d < -180.0f) {
            d += 360.0f;
        }
        return d;
    }

    static float sanitize(float v) {
        return (Float.isNaN(v) || Float.isInfinite(v)) ? 0.0f : v;
    }

    static float clamp(float v, float min, float max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}
