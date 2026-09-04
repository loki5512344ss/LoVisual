/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl;

import dev.loki.lovisual.util.player.effect.StatusEffectTracker;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Locale;

import static dev.loki.lovisual.features.gui.hud.draggable.impl.TargetHud.*;

final class TargetHudFormatter {

    private TargetHudFormatter() {
    }

    static float snapToStep(float value, float step) {
        if (step <= 0.0f) return value;
        return Math.round(value / step) * step;
    }

    static float formatDisplayHealth(float health) {
        float clamped = Math.max(0.0f, health);
        if (clamped <= HEALTH_ZERO_CLAMP) {
            return 0.0f;
        }
        float step = clamped < 1.0f ? HEALTH_TEXT_SMALL_STEP : HEALTH_TEXT_STEP;
        float snapped = snapToStep(clamped, step);
        return snapped <= HEALTH_ZERO_CLAMP ? 0.0f : snapped;
    }

    static String formatHealth(float health) {
        if (health >= 100.0f) {
            return Integer.toString(Math.round(health));
        }
        if (health >= 10.0f) {
            return String.format(Locale.ROOT, "%.1f", health);
        }
        return String.format(Locale.ROOT, "%.2f", health);
    }

    static String formatDistance(float distance) {
        if (distance >= 100.0f) {
            return Integer.toString(Math.round(distance));
        }
        return String.format(Locale.ROOT, "%.1f", distance);
    }

    static Identifier resolveEffectIcon(MobEffectInstance inst) {
        if (inst == null) {
            return null;
        }
        return inst.getEffect().unwrapKey()
                .map(key -> Identifier.fromNamespaceAndPath("minecraft", "textures/mob_effect/" + key.identifier().getPath() + ".png"))
                .orElse(null);
    }

    static String compactEffectText(int entityId, MobEffectInstance inst) {
        if (inst == null) {
            return "";
        }
        String duration = formatEffectDuration(entityId, inst);
        if (!duration.isEmpty()) {
            return duration;
        }
        int level = inst.getAmplifier() + 1;
        return level > 1 ? roman(level) : "";
    }

    static String formatEffectDuration(int entityId, MobEffectInstance inst) {
        if (inst == null) {
            return "";
        }
        if (StatusEffectTracker.shouldHideDuration(entityId, inst.getEffect())) {
            return "";
        }
        int ticks = inst.getDuration();
        if (ticks < 0 || ticks >= 32767) {
            return "";
        }
        int totalSeconds = Math.max(0, ticks / 20);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return seconds < 10 ? minutes + ":0" + seconds : minutes + ":" + seconds;
    }

    static int rgba(int red, int green, int blue, int alpha) {
        int r = Math.max(0, Math.min(255, red));
        int g = Math.max(0, Math.min(255, green));
        int b = Math.max(0, Math.min(255, blue));
        int a = Math.max(0, Math.min(255, alpha));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> Integer.toString(n);
        };
    }
}
