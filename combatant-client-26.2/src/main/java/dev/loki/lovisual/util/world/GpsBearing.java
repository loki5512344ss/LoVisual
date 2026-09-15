/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.world;

/**
 * Чистая математика GPS-компаса: направление на точку и угол доворота стрелки
 * от текущего взгляда игрока. Вынесено из модуля для юнит-тестов без Minecraft.
 */
public enum GpsBearing {
    ;

    /**
     * Азимут от игрока к точке в системе координат Minecraft (0° = юг, +Z),
     * растёт по часовой при виде сверху — совпадает с {@code Entity.getYRot()}.
     */
    public static float bearingDegrees(double dx, double dz) {
        return (float) (-(Math.atan2(dx, dz) * (180.0 / Math.PI)));
    }

    /**
     * Угол, на который нужно повернуть стрелку-компас (0° — «прямо по курсу»):
     * разница между азимутом на цель и текущим yaw игрока, приведённая к (-180, 180].
     */
    public static float arrowRotationDegrees(float bearing, float playerYaw) {
        return wrapDegrees(bearing - playerYaw);
    }

    public static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0f;
        if (wrapped >= 180.0f) wrapped -= 360.0f;
        if (wrapped < -180.0f) wrapped += 360.0f;
        return wrapped;
    }

    public static double horizontalDistance(double dx, double dz) {
        return Math.sqrt(dx * dx + dz * dz);
    }
}
