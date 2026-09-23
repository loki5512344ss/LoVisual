/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.status;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class CooldownsAnimator {

    private CooldownsAnimator() {}

    static List<Cooldowns.AnimatedRow> animateRows(Cooldowns host, List<Cooldowns.Row> rows) {
        Map<String, Cooldowns.Row> current = new LinkedHashMap<>();
        for (Cooldowns.Row row : rows) current.put(row.key(), row);
        Map<String, Cooldowns.Row> merged = new LinkedHashMap<>();
        merged.putAll(current);
        for (Map.Entry<String, Cooldowns.Row> entry : host.lastEntries.entrySet()) merged.putIfAbsent(entry.getKey(), entry.getValue());
        host.lastEntries.clear();
        host.lastEntries.putAll(merged);
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Cooldowns.Row> entry : host.lastEntries.entrySet()) {
            String key = entry.getKey();
            float target = current.containsKey(key) ? 1.0f : 0.0f;
            float anim = host.entryAnim.getOrDefault(key, 0.0f);
            anim = AnimationUtility.approach(anim, target, AnimationUtility.deltaTime(), 12.0f);
            anim = AnimationUtility.snap(anim, target, 0.02f);
            if (anim <= 0.0f && target <= 0.0f) toRemove.add(key);
            else host.entryAnim.put(key, anim);
        }
        for (String key : toRemove) { host.entryAnim.remove(key); host.lastEntries.remove(key); }
        List<Cooldowns.AnimatedRow> out = new ArrayList<>();
        for (Map.Entry<String, Cooldowns.Row> entry : host.lastEntries.entrySet()) {
            float anim = host.entryAnim.getOrDefault(entry.getKey(), 0.0f);
            if (anim > 0.01f) out.add(new Cooldowns.AnimatedRow(entry.getValue(), anim));
        }
        return out;
    }

    static float totalAnimatedHeight(List<Cooldowns.AnimatedRow> rows, float rowStep) {
        float out = 0.0f;
        for (Cooldowns.AnimatedRow row : rows) out += rowStep * row.anim();
        return out;
    }

    static int getBlinkAlpha(Cooldowns host, Cooldowns.Row row, int baseAlpha, int minAlphaFloor) {
        if (!host.blink.get()) return baseAlpha;
        if (row.remainingTicks() <= 0 || row.remainingTicks() > Cooldowns.BLINK_THRESHOLD_TICKS) return baseAlpha;
        double wave = Math.sin(System.currentTimeMillis() * Cooldowns.BLINK_SPEED);
        float factor = (float) ((wave + 1.0) * 0.5);
        int minAlpha = Math.max(0, Math.min(baseAlpha, minAlphaFloor));
        return clamp255(Math.round(minAlpha + (baseAlpha - minAlpha) * (1.0f - factor)));
    }

    static int clamp255(int value) { return Math.max(0, Math.min(255, value)); }
}
