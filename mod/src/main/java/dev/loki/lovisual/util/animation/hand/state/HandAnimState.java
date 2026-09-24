package dev.loki.lovisual.util.animation.hand.state;

import dev.loki.lovisual.util.animation.SmoothValue;

/**
 * Покадровое состояние динамики рук: сводит sway, дрейф использования и
 * появление при входе в мир. Обновляется ровно один раз за кадр
 * ({@link #beginFrame}), рендер только читает значения — это исключает
 * двойные шаги, когда кадр рендерит обе руки.
 * <p>
 * Player/level передаются как Object-идентичности: смена игрока или мира
 * перезапускает анимацию появления и гасит ложные дельты взгляда.
 * Класс чистый: без Minecraft-типов, покрыт юнит-тестами.
 */
public final class HandAnimState {

    /** Резкая смена взгляда больше этих пределов считается телепортом, а не движением мыши. */
    static final float TELEPORT_YAW_DEG = 45.0f;
    static final float TELEPORT_PITCH_DEG = 25.0f;
    static final float MAX_FRAME_DELTA_TICKS = 5.0f;

    /** Скорость дрейфа использования (99% за ~ln(100)/speed тиков). */
    static final float DRIFT_SPEED = 1.8f;
    /** Скорость появления при входе: ~10 тиков до почти полной посадки. */
    static final float JOIN_SPEED = 0.45f;

    private final SwayState sway = new SwayState();
    private final SmoothValue driftMain = new SmoothValue(DRIFT_SPEED);
    private final SmoothValue driftOff = new SmoothValue(DRIFT_SPEED);
    private final SmoothValue join = new SmoothValue(JOIN_SPEED);

    private float lastYaw = Float.NaN;
    private float lastPitch = Float.NaN;
    private Object lastPlayer;
    private Object lastLevel;

    /**
     * Один шаг на кадр.
     *
     * @param player     идентичность игрока (для смены мира/респавна)
     * @param level      идентичность мира
     * @param yaw        текущий yaw взгляда, градусы (любой диапазон)
     * @param pitch      текущий pitch взгляда, градусы
     * @param usingMain  предмет используется в главной руке
     * @param usingOff   предмет используется во второй руке
     * @param deltaTicks реальное время кадра в тиках
     */
    public void beginFrame(Object player, Object level, float yaw, float pitch,
                           boolean usingMain, boolean usingOff, float deltaTicks) {
        if (player == null) {
            reset();
            return;
        }

        if (player != lastPlayer || level != lastLevel) {
            lastPlayer = player;
            lastLevel = level;
            join.setInstant(false);
            lastYaw = Float.NaN;
            lastPitch = Float.NaN;
        }

        float dt = SwayState.sanitize(deltaTicks);
        if (Float.isNaN(lastYaw)) {
            lastYaw = yaw;
            lastPitch = pitch;
            return;
        }

        float deltaYaw = SwayState.wrapDegrees(yaw - lastYaw);
        float deltaPitch = pitch - lastPitch;
        lastYaw = yaw;
        lastPitch = pitch;

        boolean lookJump = Math.abs(deltaYaw) > TELEPORT_YAW_DEG
                || Math.abs(deltaPitch) > TELEPORT_PITCH_DEG;

        if (dt > 0.0f) {
            if (lookJump) {
                sway.reset();
            } else {
                sway.update(deltaYaw, deltaPitch, Math.min(dt, MAX_FRAME_DELTA_TICKS));
            }
            driftMain.setTarget(usingMain);
            driftMain.update(dt);
            driftOff.setTarget(usingOff);
            driftOff.update(dt);
            join.setTarget(true);
            join.update(dt);
        }
    }

    /** Полный сброс: следующий кадр считается первым после входа в мир. */
    public void reset() {
        sway.reset();
        driftMain.setInstant(false);
        driftOff.setInstant(false);
        join.setInstant(false);
        lastYaw = Float.NaN;
        lastPitch = Float.NaN;
        lastPlayer = null;
        lastLevel = null;
    }

    public SwayState sway() {
        return sway;
    }

    /** Сглаженный прогресс дрейфа для указанной руки, 0..1. */
    public float drift(boolean mainHand) {
        return (mainHand ? driftMain : driftOff).value();
    }

    /** Прогресс появления при входе в мир, 0..1 (1 — предмет встал на место). */
    public float joinProgress() {
        return join.value();
    }
}
