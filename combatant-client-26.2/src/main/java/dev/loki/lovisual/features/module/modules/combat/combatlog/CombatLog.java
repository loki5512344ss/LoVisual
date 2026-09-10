package dev.loki.lovisual.features.module.modules.combat.combatlog;

import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.events.meta.EventHandler;
import dev.loki.lovisual.events.impl.entity.AttackEntityEvent;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import dev.loki.lovisual.features.module.modules.combat.combatlog.CombatLogEntry;
import dev.loki.lovisual.features.module.modules.combat.combatlog.CombatLogStats;

/**
 * Tracks and attributes combat hits (damage dealt/taken) by watching health deltas of nearby
 * living entities each tick, correlated with local {@link AttackEntityEvent}s for outgoing hits
 * and nearby players' swing animation for incoming ones. No exact damage packet exists
 * client-side, so this is a health-delta reconstruction, not a server-authoritative log.
 */
@ModuleInfo(
        id = "combatlog",
        displayName = "CombatLog",
        category = ModuleCategory.COMBAT,
        description = "Логирует нанесённый/полученный урон и считает DPS по бою"
)
public final class CombatLog extends Module {

    private static final int MAX_ENTRIES = 200;
    private static final long OUTGOING_ATTRIBUTION_WINDOW_MS = 500L;
    private static final long INCOMING_ATTACKER_WINDOW_MS = 300L;
    private static final double INCOMING_ATTACKER_RANGE = 6.0;
    private static final double SCAN_RADIUS = 24.0;

    private final NumberValue<Integer> combatWindowSeconds = num("combatWindowSeconds", 10, 3, 60);
    private final NumberValue<Integer> historySeconds = num("combatHistorySeconds", 30, 5, 300);

    private final List<CombatLogEntry> entries = new ArrayList<>();
    private final Map<UUID, Float> prevHealth = new HashMap<>();
    private final Map<UUID, Long> pendingSelfHits = new HashMap<>();
    private float prevSelfHealth = -1f;

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (event == null || event.getTarget() == null) return;
        pendingSelfHits.put(event.getTarget().getUUID(), GameClock.millis());
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        long now = GameClock.millis();
        pendingSelfHits.entrySet().removeIf(e -> now - e.getValue() > OUTGOING_ATTRIBUTION_WINDOW_MS);

        trackOutgoingHits(mc, now);
        trackIncomingHits(mc, now);
        pruneHistory(now);
    }

    private void trackOutgoingHits(Minecraft mc, long now) {
        for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class,
                mc.player.getBoundingBox().inflate(SCAN_RADIUS), e -> e != mc.player)) {
            UUID id = entity.getUUID();
            float health = entity.getHealth();
            Float previous = prevHealth.put(id, health);
            if (previous == null || previous <= health) continue;

            if (pendingSelfHits.containsKey(id)) {
                float damage = previous - health;
                entries.add(new CombatLogEntry(entity.getName().getString(), damage, true, now));
                pendingSelfHits.remove(id);
            }
        }
    }

    private void trackIncomingHits(Minecraft mc, long now) {
        float selfHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        if (prevSelfHealth < 0f) {
            prevSelfHealth = selfHealth;
            return;
        }
        if (selfHealth < prevSelfHealth) {
            float damage = prevSelfHealth - selfHealth;
            String attacker = findLikelyAttacker(mc, now);
            entries.add(new CombatLogEntry(attacker, damage, false, now));
        }
        prevSelfHealth = selfHealth;
    }

    private String findLikelyAttacker(Minecraft mc, long now) {
        Player nearest = null;
        double nearestDistSq = INCOMING_ATTACKER_RANGE * INCOMING_ATTACKER_RANGE;

        for (Player player : mc.level.players()) {
            if (player == mc.player || !player.swinging) continue;
            double distSq = player.distanceToSqr(mc.player);
            if (distSq <= nearestDistSq) {
                nearestDistSq = distSq;
                nearest = player;
            }
        }
        return nearest != null ? nearest.getName().getString() : "Unknown";
    }

    private void pruneHistory(long now) {
        long maxAgeMs = historySeconds.get() * 1000L;
        entries.removeIf(e -> now - e.timestampMs() > maxAgeMs);
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(0);
        }
    }

    @Override
    public void onDisable() {
        entries.clear();
        prevHealth.clear();
        pendingSelfHits.clear();
        prevSelfHealth = -1f;
    }

    public List<CombatLogEntry> getEntries() {
        return entries;
    }

    public CombatLogStats.Summary getSummary() {
        return CombatLogStats.summarize(entries, GameClock.millis(), combatWindowSeconds.get() * 1000L);
    }
}
