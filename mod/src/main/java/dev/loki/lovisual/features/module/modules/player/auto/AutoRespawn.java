/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.player.auto;

import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.client.Minecraft;

/**
 * Automatically presses the vanilla respawn path after death, once per death
 * and only after a configurable delay.
 */
@ModuleInfo(
        id = "autorespawn",
        displayName = "Auto Respawn",
        aliases = {"autorespawn", "respawn"},
        category = ModuleCategory.PLAYER
)
public class AutoRespawn extends Module {
    private static final String SETTING_DELAY = "delay";

    private final NumberValue<Integer> delay = num("autoRespawnDelay", SETTING_DELAY, 500, 0, 5000);

    private final RespawnGate gate = new RespawnGate();

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            gate.reset();
            return;
        }

        boolean dead = mc.player.isDeadOrDying();
        long now = GameClock.millis();
        if (!gate.shouldRequestRespawn(dead, now, delay.get())) return;
        gate.onRequestSent();

        // Exactly what the vanilla death screen "Respawn" button does.
        mc.player.respawn();
    }

    /**
     * Pure death/delay state machine: one request per death, reset by coming
     * back to life. Unit-testable without Minecraft.
     */
    public static final class RespawnGate {
        private boolean dead;
        private long deathStartedMs;
        private boolean requestSent;

        /**
         * @return true when a respawn request should be sent right now
         */
        public boolean shouldRequestRespawn(boolean isDead, long nowMs, long delayMs) {
            if (!isDead) {
                reset();
                return false;
            }
            if (!this.dead) {
                this.dead = true;
                this.deathStartedMs = nowMs;
                this.requestSent = false;
                return false;
            }
            if (requestSent) return false;
            return nowMs - deathStartedMs >= Math.max(0L, delayMs);
        }

        public void onRequestSent() {
            requestSent = true;
        }

        public void reset() {
            dead = false;
            requestSent = false;
            deathStartedMs = 0L;
        }
    }
}
