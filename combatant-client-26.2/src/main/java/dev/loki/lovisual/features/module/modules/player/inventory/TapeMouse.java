/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.player.inventory;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.mixins.accessors.misc.KeyMappingAccessor;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Repeats the selected mouse button on an interval, mirroring real clicks by
 * feeding vanilla {@code KeyMapping.click(...)} so {@code handleKeybinds}
 * processes them itself.
 */
@ModuleInfo(
        id = "tapemouse",
        displayName = "Tape Mouse",
        aliases = {"tapemouse", "tape"},
        category = ModuleCategory.PLAYER
)
public class TapeMouse extends Module {
    private static final String SETTING_BUTTON = "button";
    private static final String SETTING_INTERVAL = "interval";
    private static final String SETTING_ONLY_HOLD = "only_hold";

    private static final String BUTTON_ATTACK = "Attack";
    private static final String BUTTON_USE = "Use";

    private final ModeValue button = mode("tapeMouseButton", SETTING_BUTTON, BUTTON_ATTACK, BUTTON_USE);
    private final NumberValue<Integer> interval = num("tapeMouseInterval", SETTING_INTERVAL, 1000, 100, 5000);
    private final BooleanValue onlyHold = bool("tapeMouseOnlyHold", SETTING_ONLY_HOLD, true);

    private final ClickPacer pacer = new ClickPacer();

    @Override
    public void onEnable() {
        pacer.reset();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.options == null) return;
        if (mc.gui == null || mc.gui.screen() != null) return;

        boolean attack = BUTTON_ATTACK.equals(button.get());
        KeyMapping mapping = attack ? mc.options.keyAttack : mc.options.keyUse;
        if (mapping == null) return;
        if (onlyHold.get() && !mapping.isDown()) return;

        long now = GameClock.millis();
        if (!pacer.isDue(now, interval.get())) return;
        pacer.mark(now);

        // Identical to a physical press: vanilla MouseHandler calls the same
        // static KeyMapping.click for real input events.
        KeyMapping.click(((KeyMappingAccessor) (Object) mapping).lovisual$key());
    }

    /**
     * Pure interval pacing logic, unit-testable without Minecraft.
     */
    public static final class ClickPacer {
        private long lastClickMs;
        private boolean primed;

        public boolean isDue(long nowMs, long intervalMs) {
            long safeInterval = Math.max(1L, intervalMs);
            return !primed || nowMs - lastClickMs >= safeInterval;
        }

        public void mark(long nowMs) {
            lastClickMs = nowMs;
            primed = true;
        }

        public void reset() {
            primed = false;
        }
    }
}
