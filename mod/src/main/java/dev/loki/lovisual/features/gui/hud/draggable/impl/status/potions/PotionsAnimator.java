package dev.loki.lovisual.features.gui.hud.draggable.impl.status;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PotionsAnimator {

    private PotionsAnimator() {}

    static List<Potions.AnimatedRow> animateRows(Potions host, List<Potions.Row> rows) {
        Map<String, Potions.Row> current = new LinkedHashMap<>();
        for (Potions.Row row : rows) {
            current.put(row.key(), row);
        }
        Map<String, Potions.Row> merged = new LinkedHashMap<>();
        merged.putAll(current);
        for (Map.Entry<String, Potions.Row> entry : host.lastEntries.entrySet()) {
            merged.putIfAbsent(entry.getKey(), entry.getValue());
        }
        host.lastEntries.clear();
        host.lastEntries.putAll(merged);
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Potions.Row> entry : host.lastEntries.entrySet()) {
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
        List<Potions.AnimatedRow> out = new ArrayList<>();
        for (Map.Entry<String, Potions.Row> entry : host.lastEntries.entrySet()) {
            float anim = host.entryAnim.getOrDefault(entry.getKey(), 0.0f);
            if (anim > 0.01f) {
                out.add(new Potions.AnimatedRow(entry.getValue(), anim));
            }
        }
        return out;
    }

    static float totalAnimatedHeight(List<Potions.AnimatedRow> rows, float rowStep) {
        float out = 0.0f;
        for (Potions.AnimatedRow row : rows) {
            out += rowStep * row.anim();
        }
        return out;
    }

    static int getBlinkAlpha(Potions.Row row, int baseAlpha, int minAlphaFloor) {
        MobEffectInstance inst = row.effect();
        if (inst == null) {
            return baseAlpha;
        }
        int duration = inst.getDuration();
        if (duration <= 0 || duration > Potions.BLINK_THRESHOLD_TICKS) {
            return baseAlpha;
        }
        double wave = Math.sin(System.currentTimeMillis() * Potions.BLINK_SPEED);
        float factor = (float) ((wave + 1.0) * 0.5);
        int minAlpha = Math.max(0, Math.min(baseAlpha, minAlphaFloor));
        return clamp255(Math.round(minAlpha + (baseAlpha - minAlpha) * (1.0f - factor)));
    }

    static boolean isBadEffect(MobEffectInstance effect) {
        return effect != null && effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL;
    }

    static int clamp255(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
