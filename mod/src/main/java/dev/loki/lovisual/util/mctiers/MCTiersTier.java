/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.mctiers;

/**
 * Чистое форматирование одной строки рейтинга mctiers.com (тир 1..5,
 * retired-флаг) в текст/цвет — вынесено из парсинга JSON, чтобы тестить
 * без сети. Соответствие тиров цветам/подписям взято из публичного формата
 * mctiers.com (HT1/LT2/LT3/LT4, T5+ как запасной вариант).
 */
public record MCTiersTier(int tier, boolean retired) {

    public String label() {
        return switch (tier) {
            case 1 -> "HT1";
            case 2 -> "LT2";
            case 3 -> "LT3";
            case 4 -> "LT4";
            default -> "T" + tier;
        };
    }

    /** ARGB, непрозрачный. */
    public int colorArgb() {
        return switch (tier) {
            case 1 -> 0xFFFF5555;
            case 2 -> 0xFFFF55FF;
            case 3 -> 0xFF5555FF;
            case 4 -> 0xFF55FF55;
            default -> 0xFFAAAAAA;
        };
    }

    public static String modeApiKey(String fixType) {
        if (fixType == null) return "vanilla";
        return switch (fixType) {
            case "UHC" -> "uhc";
            case "Pot" -> "pot";
            case "NethOP" -> "neth_pot";
            case "SMP" -> "smp";
            case "Sword" -> "sword";
            case "Axe" -> "axe";
            case "Mace" -> "mace";
            default -> "vanilla";
        };
    }

    public static String modeDisplayName(String fixType) {
        return fixType == null ? "Vanilla" : fixType;
    }
}
