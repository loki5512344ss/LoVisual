/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.targethud;

import dev.loki.lovisual.util.player.PlayerHealthResolver;
import dev.loki.lovisual.util.pvp.opponents.TotemPopCounter;
import dev.loki.lovisual.util.pvp.opponents.TotemPopSnapshot;
import dev.loki.lovisual.util.screen.ClientScreen;
import dev.loki.lovisual.util.target.TargetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.TargetHud;

public final class TargetHudTargetResolver {

    private TargetHudTargetResolver() {
    }

    public static LivingEntity resolveTarget(TargetHud element, boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        LivingEntity target = TargetManager.getTarget();
        if (target != null && element.playersOnly.get() && !(target instanceof Player)) {
            target = null;
        }
        if (target == null && (preview || ClientScreen.current() instanceof ChatScreen)) {
            target = mc.player;
        }
        return target;
    }

    public static void updateDisplayTarget(TargetHud element, LivingEntity target, long now) {
        Minecraft mc = Minecraft.getInstance();
        if (target != null) {
            if (element.displayTarget != target) {
                element.displayTarget = target;
                PlayerHealthResolver.HealthSnapshot health = PlayerHealthResolver.resolve(target);
                float baseMaxHealth = Math.max(1.0f, target.getMaxHealth());
                float realHealth = Math.max(0.0f, health.totalHealth() - health.absorption());
                element.displayedHealth = health.totalHealth();
                element.healthAnimation = realHealth / baseMaxHealth;
                element.trailAnimation = element.healthAnimation;
                element.absorptionAnimation = health.absorption() / baseMaxHealth;
                element.displayedDistance = mc.player != null ? mc.player.distanceTo(target) : 0.0f;
                element.waveStartMs = now;
            }
            element.lastSeenMs = now;
            return;
        }

        if (element.displayTarget == mc.player && !(ClientScreen.current() instanceof ChatScreen)) {
            element.lastSeenMs = 0L;
        }
    }

    public static boolean shouldShow(TargetHud element, LivingEntity target, long now) {
        if (target != null) return true;
        return element.displayTarget != null && element.lastSeenMs > 0L && now - element.lastSeenMs <= Math.max(0L, element.holdMs.get());
    }

    public static TotemPopSnapshot resolveTotemPopSnapshot(TargetHud element, LivingEntity target) {
        if (!(target instanceof Player player)) {
            return TotemPopSnapshot.empty(null);
        }
        return TotemPopCounter.snapshot(player.getUUID());
    }
}
