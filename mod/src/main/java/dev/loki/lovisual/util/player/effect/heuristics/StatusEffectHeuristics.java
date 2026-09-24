package dev.loki.lovisual.util.player.effect.heuristics;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.util.player.effect.heuristics.StatusEffectInference;
import dev.loki.lovisual.util.player.effect.tracker.StatusEffectTracker;

import java.util.List;

/**
 * Heuristics to guess effects from colored entity-effect particles.
 */
public enum StatusEffectHeuristics {
    ;

    private static final java.util.Map<Integer, java.util.Map<Holder<MobEffect>, Long>> LAST_SEEN = new java.util.HashMap<>();
    private static final long HEURISTIC_TIMEOUT_MS = 10000L;

    public static void handle(ParticleOptions particle, double px, double py, double pz) {
        Holder<MobEffect> direct = ParticleColors.directEffectFromParticle(particle);
        if (direct != null) {
            registerHeuristic(direct, px, py, pz);
            return;
        }

        Holder<MobEffect> guessed = ParticlePalette.guess(ParticleColors.extractColor(particle));
        if (guessed == null) return;
        registerHeuristic(guessed, px, py, pz);
    }

    private static void registerHeuristic(Holder<MobEffect> guessed, double px, double py, double pz) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        List<? extends Player> players = mc.level.players();
        double bestDist = Double.MAX_VALUE;
        Player target = null;
        Vec3 pos = new Vec3(px, py, pz);

        for (Player p : players) {
            double d = p.position().distanceTo(pos);
            if (d < bestDist) {
                bestDist = d;
                target = p;
            }
        }

        if (target == null) return;
        if (mc.player != null && target.getId() == mc.player.getId()) return;
        if (!StatusEffectInference.isParticleNearPlayer(target, pos)) return;

        long now = System.currentTimeMillis();
        // Always refresh last seen so we don't drop while particles keep coming.
        markSeen(target.getId(), guessed, now);
        boolean has = StatusEffectTracker.has(target.getId(), guessed);
        boolean hideCurrent = StatusEffectTracker.shouldHideDuration(target.getId(), guessed);
        if (has && !hideCurrent) {
            return; // already tracked with a visible duration, just refreshed last-seen
        }
        StatusEffectInference.ResolvedEffect resolved = null;
        if (!has || hideCurrent) {
            resolved = StatusEffectInference.resolve(target, guessed, pos, false);
            if (has && (resolved == null || !resolved.showDuration())) {
                return; // already tracked, and no duration upgrade available
            }
        }
        // Heuristic lifetime is handled by stale timeout; set infinite here.
        int heuristicDurationTicks = -1;
        int amplifier = 0;
        boolean hideDuration = true;
        if (resolved != null) {
            resolved = StatusEffectInference.resolve(target, guessed, pos, true);
            if (resolved != null) {
                amplifier = resolved.amplifier();
                heuristicDurationTicks = resolved.duration();
                hideDuration = !resolved.showDuration();
            }
        }
        StatusEffectTracker.apply(target.getId(), guessed, amplifier, heuristicDurationTicks, false, true, true, true, hideDuration);
    }

    private static void markSeen(int entityId, Holder<MobEffect> effect, long time) {
        var map = LAST_SEEN.computeIfAbsent(entityId, k -> new java.util.HashMap<>());
        map.put(effect, time);
    }

    /**
     * Called from tracker tick to drop heuristic effects if we stopped seeing particles.
     */
    public static void pruneStale(long nowMs) {
        if (LAST_SEEN.isEmpty()) return;
        var removeEntities = new java.util.ArrayList<Integer>();
        for (var entry : LAST_SEEN.entrySet()) {
            int eid = entry.getKey();
            var effects = entry.getValue();
            var toRemove = new java.util.ArrayList<Holder<MobEffect>>();
            for (var e : effects.entrySet()) {
                if (nowMs - e.getValue() > HEURISTIC_TIMEOUT_MS) {
                    StatusEffectTracker.remove(eid, e.getKey());
                    toRemove.add(e.getKey());
                }
            }
            toRemove.forEach(effects::remove);
            if (effects.isEmpty()) removeEntities.add(eid);
        }
        removeEntities.forEach(LAST_SEEN::remove);
    }
}
