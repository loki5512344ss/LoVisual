/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.animation;

/**
 * Общий набор строковых опций {@link ModeValue}-настройки цвета и парсер в
 * {@link AnimatedRenderColors.Mode} — вынесено, чтобы каждый модуль с
 * анимированным цветом не дублировал один и тот же switch (см. HitEffect,
 * HitBubbles).
 */
public enum AnimatedColorModeOptions {
    ;

    public static final String[] OPTIONS = {
            "Static", "Rainbow", "LightRainbow", "Sky", "Fade", "DoubleColor", "Analogous", "Theme"
    };

    public static AnimatedRenderColors.Mode parse(String value) {
        return switch (value == null ? "" : value) {
            case "Rainbow" -> AnimatedRenderColors.Mode.RAINBOW;
            case "LightRainbow" -> AnimatedRenderColors.Mode.LIGHT_RAINBOW;
            case "Sky" -> AnimatedRenderColors.Mode.SKY;
            case "Fade" -> AnimatedRenderColors.Mode.FADE;
            case "DoubleColor" -> AnimatedRenderColors.Mode.DOUBLE_COLOR;
            case "Analogous" -> AnimatedRenderColors.Mode.ANALOGOUS;
            case "Theme" -> AnimatedRenderColors.Mode.THEME;
            default -> AnimatedRenderColors.Mode.STATIC;
        };
    }
}
