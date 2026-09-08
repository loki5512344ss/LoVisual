/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.player;

import dev.loki.lovisual.config.values.BooleanValue;
import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;
import dev.loki.lovisual.features.module.Notifier;
import dev.loki.lovisual.features.relations.PlayerRelations;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Leaves the server by simple safety triggers: low health or unfriendly
 * players nearby. Fires once, after a configurable delay, with a warning
 * notification so the leave never comes as a surprise.
 */
@ModuleInfo(
        id = "autoleave",
        displayName = "Auto Leave",
        aliases = {"autoleave", "autodc"},
        category = ModuleCategory.PLAYER
)
public class AutoLeave extends Module {
    private static final String SETTING_HP_ENABLED = "hp_enabled";
    private static final String SETTING_HP_THRESHOLD = "hp_threshold";
    private static final String SETTING_PLAYERS_ENABLED = "players_enabled";
    private static final String SETTING_PLAYER_COUNT = "player_count";
    private static final String SETTING_RADIUS = "radius";
    private static final String SETTING_IGNORE_FRIENDS = "ignore_friends";
    private static final String SETTING_DELAY = "delay";

    private final BooleanValue hpEnabled = bool("autoLeaveHpEnabled", SETTING_HP_ENABLED, true);
    private final NumberValue<Double> hpThreshold =
            visibleWhen(num("autoLeaveHpThreshold", SETTING_HP_THRESHOLD, 6.0, 1.0, 19.0), hpEnabled::get);
    private final BooleanValue playersEnabled = bool("autoLeavePlayersEnabled", SETTING_PLAYERS_ENABLED, false);
    private final NumberValue<Integer> playerCount =
            visibleWhen(num("autoLeavePlayerCount", SETTING_PLAYER_COUNT, 1, 1, 20), playersEnabled::get);
    private final NumberValue<Integer> radius =
            visibleWhen(num("autoLeaveRadius", SETTING_RADIUS, 32, 8, 128), playersEnabled::get);
    private final BooleanValue ignoreFriends =
            visibleWhen(bool("autoLeaveIgnoreFriends", SETTING_IGNORE_FRIENDS, true), playersEnabled::get);
    private final NumberValue<Integer> delay = num("autoLeaveDelay", SETTING_DELAY, 1000, 0, 5000);

    private final LeavePolicy policy = new LeavePolicy();
    private LocalPlayer lastPlayer;

    @Override
    public void onDisable() {
        lastPlayer = null;
        policy.reset();
    }

    @Override
    public void onFrame(float tickDelta) {
        // onTick only fires while a player exists; re-arming on world change
        // has to happen where the player absence is still observable.
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            lastPlayer = null;
            policy.reset();
            return;
        }
        if (mc.player != lastPlayer) {
            lastPlayer = mc.player;
            policy.reset();
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return;

        boolean triggered = isLowHealth(mc.player) || hasUnfriendlyPlayersNearby(mc);
        if (!policy.onTick(triggered, GameClock.millis(), delay.get())) return;
        if (policy.consumeWarning()) {
            Notifier.warning("Auto Leave: disconnecting now");
        }
        leave(mc);
    }

    private boolean isLowHealth(LocalPlayer player) {
        return hpEnabled.get() && LeavePolicy.isHealthLow(player.getHealth(), hpThreshold.get().floatValue());
    }

    private boolean hasUnfriendlyPlayersNearby(Minecraft mc) {
        if (!playersEnabled.get()) return false;

        double radiusSq = (double) radius.get() * radius.get();
        int unfriendly = 0;
        for (Player other : mc.level.players()) {
            if (other == mc.player || other.isSpectator()) continue;
            if (!other.isAlive()) continue;
            if (ignoreFriends.get() && PlayerRelations.get().isFriend(other.getGameProfile().name())) continue;
            if (other.distanceToSqr(mc.player) > radiusSq) continue;
            unfriendly++;
        }
        return LeavePolicy.isPlayerCountReached(unfriendly, playerCount.get());
    }

    private void leave(Minecraft mc) {
        policy.reset();
        mc.disconnect(null, false);
    }

    /**
     * Pure trigger state machine: warn once on a rising trigger edge, then
     * leave after the configured delay. Any quiet tick re-arms the gate.
     */
    public static final class LeavePolicy {
        private boolean triggered;
        private long triggerMs;
        private boolean warningShown;

        public boolean onTick(boolean anyTrigger, long nowMs, long delayMs) {
            if (!anyTrigger) {
                reset();
                return false;
            }
            if (!triggered) {
                triggered = true;
                triggerMs = nowMs;
                warningShown = false;
                return Math.max(0L, delayMs) <= 0L;
            }
            return nowMs - triggerMs >= Math.max(0L, delayMs);
        }

        public boolean consumeWarning() {
            if (warningShown) return false;
            warningShown = true;
            return true;
        }

        public void reset() {
            triggered = false;
            triggerMs = 0L;
            warningShown = false;
        }

        public static boolean isHealthLow(float health, float threshold) {
            return health <= threshold;
        }

        public static boolean isPlayerCountReached(int count, int required) {
            return count >= Math.max(1, required);
        }
    }
}
