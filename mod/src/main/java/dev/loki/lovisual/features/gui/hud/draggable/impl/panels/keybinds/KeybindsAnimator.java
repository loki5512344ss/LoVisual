/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.panels;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class KeybindsAnimator {

    private KeybindsAnimator() {}

    static List<Keybinds.AnimatedRow> animateRows(Keybinds host, List<Keybinds.Row> rows) {
        Map<String, Keybinds.Row> current = new LinkedHashMap<>();
        for (Keybinds.Row row : rows) current.put(row.key(), row);
        Map<String, Keybinds.Row> merged = new LinkedHashMap<>();
        merged.putAll(current);
        for (Map.Entry<String, Keybinds.Row> e : host.lastEntries.entrySet()) merged.putIfAbsent(e.getKey(), e.getValue());
        host.lastEntries.clear();
        host.lastEntries.putAll(merged);
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Keybinds.Row> e : host.lastEntries.entrySet()) {
            String key = e.getKey();
            float target = current.containsKey(key) ? 1.0f : 0.0f;
            float anim = host.entryAnim.getOrDefault(key, 0.0f);
            anim = AnimationUtility.approach(anim, target, AnimationUtility.deltaTime(), 12.0f);
            anim = AnimationUtility.snap(anim, target, 0.02f);
            if (anim <= 0.0f && target <= 0.0f) toRemove.add(key);
            else host.entryAnim.put(key, anim);
        }
        for (String key : toRemove) {
            host.entryAnim.remove(key);
            host.lastEntries.remove(key);
        }
        List<Keybinds.AnimatedRow> out = new ArrayList<>();
        for (Map.Entry<String, Keybinds.Row> e : host.lastEntries.entrySet()) {
            float anim = host.entryAnim.getOrDefault(e.getKey(), 0.0f);
            if (anim > 0.01f) out.add(new Keybinds.AnimatedRow(e.getValue(), anim));
        }
        return out;
    }

    static float totalAnimatedHeight(List<Keybinds.AnimatedRow> rows, float rowStep) {
        float out = 0.0f;
        for (Keybinds.AnimatedRow r : rows) out += rowStep * r.anim();
        return out;
    }

    static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
