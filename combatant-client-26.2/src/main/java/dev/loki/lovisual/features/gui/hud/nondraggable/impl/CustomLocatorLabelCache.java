/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.nondraggable.impl;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.loki.lovisual.features.gui.hud.nondraggable.impl.CustomBar.*;

public final class CustomLocatorLabelCache {

    private static final Map<String, Float> ANIM = new HashMap<>();
    private static final Map<String, CustomBarModel.LocatorLabel> CACHE = new HashMap<>();

    private CustomLocatorLabelCache() {
    }

    static float updateAnim(String key, float target, float dt) {
        float current = ANIM.getOrDefault(key, 0f);
        float next = AnimationUtility.approach(current, target, dt, LOCATOR_LABEL_ANIM_SPEED);
        next = AnimationUtility.snap(next, target, 0.01f);
        ANIM.put(key, next);
        return next;
    }

    static void cache(String key, CustomBarModel.LocatorLabel label) {
        CACHE.put(key, label);
    }

    static void decay(Set<String> seen, float dt) {
        if (ANIM.isEmpty()) return;
        var it = ANIM.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (seen.contains(entry.getKey())) continue;
            float next = AnimationUtility.approach(entry.getValue(), 0f, dt, LOCATOR_LABEL_ANIM_SPEED);
            next = AnimationUtility.snap(next, 0f, 0.01f);
            if (next <= 0.0f) {
                CACHE.remove(entry.getKey());
                it.remove();
            } else {
                entry.setValue(next);
            }
        }
    }

    static void appendCached(List<CustomBarModel.LocatorLabel> labels) {
        if (labels == null || CACHE.isEmpty()) return;
        for (Map.Entry<String, CustomBarModel.LocatorLabel> entry : CACHE.entrySet()) {
            float alpha = ANIM.getOrDefault(entry.getKey(), 0f);
            if (alpha <= 0.01f) continue;
            CustomBarModel.LocatorLabel cached = entry.getValue();
            labels.add(new CustomBarModel.LocatorLabel(cached.name(), cached.distance(), alpha, cached.target(), cached.active(), cached.sort()));
        }
    }
}
