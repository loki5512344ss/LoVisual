package dev.loki.lovisual.features.module.modules.combat.combatlog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import dev.loki.lovisual.features.module.modules.combat.combatlog.CombatLogEntry;

/**
 * Pure aggregation over a window of {@link CombatLogEntry}: damage dealt/taken, DPS, and who
 * you're trading the most with. No Minecraft dependency — entries and "now" are passed in.
 */
public final class CombatLogStats {

    public record Summary(float totalDealt, float totalTaken, float dpsDealt, float dpsTaken,
                          String topTarget, String topAttacker) {
        public static final Summary EMPTY = new Summary(0f, 0f, 0f, 0f, null, null);
    }

    private CombatLogStats() {
    }

    public static Summary summarize(List<CombatLogEntry> entries, long now, long windowMs) {
        if (entries == null || entries.isEmpty() || windowMs <= 0) return Summary.EMPTY;

        float totalDealt = 0f;
        float totalTaken = 0f;
        long windowStart = now - windowMs;
        Map<String, Float> dealtTo = new HashMap<>();
        Map<String, Float> takenFrom = new HashMap<>();

        for (CombatLogEntry entry : entries) {
            if (entry.timestampMs() < windowStart || entry.timestampMs() > now) continue;

            if (entry.dealtByMe()) {
                totalDealt += entry.damage();
                dealtTo.merge(entry.otherName(), entry.damage(), Float::sum);
            } else {
                totalTaken += entry.damage();
                takenFrom.merge(entry.otherName(), entry.damage(), Float::sum);
            }
        }

        float seconds = windowMs / 1000f;
        float dpsDealt = seconds > 0 ? totalDealt / seconds : 0f;
        float dpsTaken = seconds > 0 ? totalTaken / seconds : 0f;

        return new Summary(totalDealt, totalTaken, dpsDealt, dpsTaken, topName(dealtTo), topName(takenFrom));
    }

    private static String topName(Map<String, Float> byName) {
        String best = null;
        float bestValue = -1f;
        for (Map.Entry<String, Float> entry : byName.entrySet()) {
            if (entry.getValue() > bestValue) {
                bestValue = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }
}
