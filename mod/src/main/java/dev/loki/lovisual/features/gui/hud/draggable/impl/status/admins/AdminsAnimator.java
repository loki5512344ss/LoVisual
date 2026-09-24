package dev.loki.lovisual.features.gui.hud.draggable.impl.status;

import dev.loki.lovisual.features.relations.StaffData;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AdminsAnimator {

    private AdminsAnimator() {}

    static List<Admins.AnimatedEntry> animateEntries(Admins host, List<StaffData.StaffEntry> entries) {
        Map<String, StaffData.StaffEntry> current = new LinkedHashMap<>();
        for (StaffData.StaffEntry entry : entries) {
            current.put(entry.name(), entry);
        }
        Map<String, StaffData.StaffEntry> merged = new LinkedHashMap<>();
        merged.putAll(current);
        for (Map.Entry<String, StaffData.StaffEntry> entry : host.lastEntries.entrySet()) {
            merged.putIfAbsent(entry.getKey(), entry.getValue());
        }
        host.lastEntries.clear();
        host.lastEntries.putAll(merged);

        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, StaffData.StaffEntry> entry : host.lastEntries.entrySet()) {
            String key = entry.getKey();
            float target = current.containsKey(key) ? 1.0f : 0.0f;
            float anim = host.entryAnim.getOrDefault(key, 0.0f);
            anim = AnimationUtility.approach(anim, target, AnimationUtility.deltaTime(), 12.0f);
            anim = AnimationUtility.snap(anim, target, 0.02f);
            if (anim <= 0.0f && target <= 0.0f) {
                toRemove.add(key);
            } else {
                host.entryAnim.put(key, anim);
            }
        }
        for (String key : toRemove) {
            host.entryAnim.remove(key);
            host.lastEntries.remove(key);
        }

        List<Admins.AnimatedEntry> out = new ArrayList<>();
        for (Map.Entry<String, StaffData.StaffEntry> entry : host.lastEntries.entrySet()) {
            float anim = host.entryAnim.getOrDefault(entry.getKey(), 0.0f);
            if (anim > 0.01f) {
                out.add(new Admins.AnimatedEntry(entry.getValue(), anim));
            }
        }
        return out;
    }

    static float totalAnimatedHeight(List<Admins.AnimatedEntry> rows, float rowStep) {
        float out = 0.0f;
        for (Admins.AnimatedEntry row : rows) {
            out += rowStep * row.anim();
        }
        return out;
    }

    static int clamp255(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
