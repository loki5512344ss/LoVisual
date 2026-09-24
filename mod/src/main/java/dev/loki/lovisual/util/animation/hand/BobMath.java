package dev.loki.lovisual.util.animation.hand;

/**
 * Чистая математика синусоидального покачивания предмета при ходьбе/беге.
 * <p>
 * Фаза берётся из накопителя шагов игрока ({@code walkAnimation.position()}),
 * амплитуда — из нормализованной скорости ({@code walkAnimation.speed()},
 * 0..1). Один цикл латерали соответствует двум шагам, вертикаль проседает
 * дважды за цикл с лёгким сдвигом фазы (как у ванильного боба камеры).
 * Класс чистый: без Minecraft-типов, покрыт юнит-тестами.
 */
public enum BobMath {
    ;

    /** Радиан на единицу пройденной фазы (та же частота шагов, что у конечностей). */
    public static final float STEP_RATE = 0.6662f;

    /** Сдвиг фазы вертикали, чтобы проседание не совпадало с крайними точками латерали. */
    static final float VERTICAL_PHASE_OFFSET = 0.2f;

    /**
     * Амплитуда боба по нормализованной скорости: 0 — стоим, 1 — бег/полная ходьба.
     * Некорректные входы (NaN/Inf) дают 0.
     */
    public static float amplitude(float speed01) {
        if (Float.isNaN(speed01) || Float.isInfinite(speed01)) {
            return 0.0f;
        }
        if (speed01 <= 0.0f) {
            return 0.0f;
        }
        return Math.min(1.0f, speed01);
    }

    /** Латеральная компонента, -1..1, период — два шага. */
    public static float lateral(float phase) {
        return (float) Math.sin(phase * STEP_RATE);
    }

    /** Вертикальная компонента, -1..0: проседание дважды за цикл. */
    public static float vertical(float phase) {
        float c = (float) Math.cos(phase * STEP_RATE - VERTICAL_PHASE_OFFSET);
        return -Math.abs(c);
    }

    /**
     * Точка фазы, где латеральная компонента достигает максимума:
     * полезна тестам для проверки симметрии цикла.
     */
    public static float lateralPeakPhase() {
        return (float) (Math.PI / 2.0 / STEP_RATE);
    }
}
