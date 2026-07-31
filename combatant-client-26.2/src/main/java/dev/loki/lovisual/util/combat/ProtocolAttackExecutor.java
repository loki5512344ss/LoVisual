/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.combat.RubberHand;
import dev.loki.lovisual.util.item.FoodUtil;
import dev.loki.lovisual.util.player.ResetAttackCooldown;

/**
 * Shared protocol-aware attack execution.
 * 1.9+ relies on vanilla cooldown reset from attackEntity.
 * 1.8 path keeps explicit reset for legacy behavior.
 */
public enum ProtocolAttackExecutor {
    ;

    public static boolean attackOnce(Minecraft mc, LocalPlayer player, LivingEntity target, boolean legacyProtocol) {
        return attackOnce(mc, player, target, legacyProtocol, false);
    }

    public static boolean attackOnce(Minecraft mc,
                                     LocalPlayer player,
                                     LivingEntity target,
                                     boolean legacyProtocol,
                                     boolean keepSprint) {
        if (mc == null || player == null || target == null) {
            return false;
        }

        RubberHand rubberHand = Modules.get(RubberHand.class);
        boolean rubberFoodAttack = player.isUsingItem()
                && FoodUtil.isFood(player.getUseItem())
                && rubberHand != null
                && rubberHand.isEnabled()
                && rubberHand.canAttackWhileEating();

        boolean attacked = rubberFoodAttack
                ? AttackUtil.attackCurrentItem(mc, target, keepSprint)
                : AttackUtil.attack(
                mc,
                target,
                null,
                null,
                false,
                false,
                keepSprint
        );

        if (attacked && legacyProtocol) {
            ResetAttackCooldown.resetAttackCooldown(player);
        }

        return attacked;
    }
}
