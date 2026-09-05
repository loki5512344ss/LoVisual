/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.animation;

/**
 * Кривые анимации для equip-движений руки. Чистая математика без Minecraft.
 */
public enum EaseCurves {
    ;

    /**
     * Ease-out-back: перелетает чуть дальше 1.0 перед установкой, давая движению вес.
     *
     * @param c1 сила «отдачи» (оригинал — 1.70158)
     */
    public static float easeOutBack(float x, float c1) {
        float c3 = c1 + 1.0f;
        float t = x - 1.0f;
        return 1.0f + c3 * t * t * t + c1 * t * t;
    }

    /**
     * Перекладывает линейный [0,1] equip-прогресс через ease-out-back,
     * затем подмешивает по intensity: 0 — ванильная линейность, 1 — полная кривая.
     */
    public static float shapeEquipProgress(float linear, float c1, float intensity) {
        float eased = easeOutBack(linear, c1);
        float amount = Math.max(0.0f, Math.min(1.0f, intensity));
        return linear + (eased - linear) * amount;
    }
}
