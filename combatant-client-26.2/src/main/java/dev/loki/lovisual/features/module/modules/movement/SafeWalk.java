/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.movement;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import dev.loki.lovisual.config.values.BooleanValue;
import dev.loki.lovisual.config.values.EnumValue;
import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.events.EventHandler;
import dev.loki.lovisual.events.impl.MovementInputEvent;
import dev.loki.lovisual.events.impl.PlayerSafeWalkEvent;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;
import dev.loki.lovisual.util.entity.EagleUtil;

//todo Description
@ModuleInfo(
        id = "safewalk",
        displayName = "SafeWalk",
        category = ModuleCategory.MOVEMENT
)
public final class SafeWalk extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private final EnumValue<Mode> mode = enumMode("mode", Mode.ON_EDGE);
    private final EnumValue<OnEdgeMode> onEdgeMode =
            visibleWhen(enumMode("on_edge_mode", OnEdgeMode.STOP), this::isOnEdgeMode);
    private final NumberValue<Double> edgeDistance =
            visibleWhen(num("edge_distance", 0.1, 0.01, 0.5), this::isOnEdgeMode);
    private final NumberValue<Integer> keepTicks =
            visibleWhen(num("keep_ticks", 2, 1, 20), this::isOnEdgeMode);
    private final NumberValue<Integer> sneakTicks =
            visibleWhen(num("sneak_ticks", 0, 0, 20), this::isOnEdgeMode);
    private final BooleanValue jump =
            visibleWhen(bool("jump", false), this::isOnEdgeMode);
    private final EagleUtil.EdgeRecovery edgeRecovery = new EagleUtil.EdgeRecovery();

    private boolean isOnEdgeMode() {
        return mode.get() == Mode.ON_EDGE;
    }

    @Override
    public void onDisable() {
        edgeRecovery.reset();
    }

    public boolean handleExternalOnEdge(LocalPlayer player, MovementInputEvent event) {
        if (player == null || event == null || player.getAbilities().flying) {
            edgeRecovery.reset();
            return false;
        }

        if (!player.onGround() && !event.isSneak()) {
            edgeRecovery.reset();
            return false;
        }

        return edgeRecovery.handleOnEdge(
                player,
                event,
                edgeDistance.get(),
                onEdgeMode.get().recoveryMode(),
                keepTicks.get(),
                sneakTicks.get(),
                jump.get()
        );
    }

    @EventHandler
    private void onSafeWalk(PlayerSafeWalkEvent event) {
        if (!isEnabled() || mode.get() != Mode.SAFE) {
            return;
        }

        LocalPlayer player = mc.player;
        if (player == null || !player.onGround() || player.getAbilities().flying) {
            return;
        }

        event.setSafeWalk(true);
    }

    @EventHandler
    private void onMovementInput(MovementInputEvent event) {
        if (!isEnabled() || mode.get() != Mode.ON_EDGE) {
            edgeRecovery.reset();
            return;
        }

        LocalPlayer player = mc.player;
        if (player == null || player.getAbilities().flying) {
            edgeRecovery.reset();
            return;
        }

        if (!player.onGround() && !event.isSneak()) {
            edgeRecovery.reset();
            return;
        }

        handleExternalOnEdge(player, event);
    }

    public enum Mode {
        SAFE,
        ON_EDGE
    }

    @RequiredArgsConstructor
    public enum OnEdgeMode {
        STOP(EagleUtil.RecoveryMode.STOP),
        INVERT(EagleUtil.RecoveryMode.INVERT),
        CENTER(EagleUtil.RecoveryMode.CENTER);

        private final EagleUtil.RecoveryMode recoveryMode;

        public EagleUtil.RecoveryMode recoveryMode() {
            return recoveryMode;
        }
    }
}
