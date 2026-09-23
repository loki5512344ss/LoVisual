/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.metrics;

import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

import java.time.LocalTime;

final class SystemTimeIcons {

    private static final String[] CLOCK_ICONS = {
            iconString(0xEA10),
            iconString(0xEA14),
            iconString(0xEA15),
            iconString(0xEA16),
            iconString(0xEA17),
            iconString(0xEA18),
            iconString(0xEA19),
            iconString(0xEA1A),
            iconString(0xEA10),
            iconString(0xEA11),
            iconString(0xEA12),
            iconString(0xEA13)
    };

    private SystemTimeIcons() {
    }

    static String iconString(int codepoint) {
        return new String(Character.toChars(codepoint));
    }

    static TextRenderer getWeatherIcons() {
        return Fonts.renderer("WeatherIcons", FontInfo.Type.Regular, TextRenderer.get());
    }

    static String formatTime(LocalTime time, boolean showSeconds) {
        if (showSeconds) {
            return String.format("%02d:%02d:%02d", time.getHour(), time.getMinute(), time.getSecond());
        }
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

    static String clockIcon(int hour) {
        return CLOCK_ICONS[hour % 12];
    }
}
