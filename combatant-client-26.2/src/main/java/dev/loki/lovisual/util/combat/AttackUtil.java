/*
 * This file is part of the LoVisual Client distribution.
 * LoVisual modifications copyright (c) 2026 pivosos2007.
 *
 * Portions of this file are based on LiquidBounce
 * (https://github.com/CCBlueX/LiquidBounce).
 * Copyright (c) 2015-2026 CCBlueX.
 *
 * LiquidBounce portions are licensed under GPLv3-or-later.
 * LoVisual modifications are licensed under GPLv3.
 * See THIRD_PARTY_NOTICES.md for details.
 */

package dev.loki.lovisual.util.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.events.impl.AttackEntityEvent;
import dev.loki.lovisual.features.gui.hud.draggable.impl.TargetHud;
import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.combat.RubberHand;
import dev.loki.lovisual.features.module.modules.misc.HitSounds;
import dev.loki.lovisual.features.module.modules.visuals.HitEffect;
import dev.loki.lovisual.mixins.accessors.LocalPlayerAccessor;
import dev.loki.lovisual.mixins.accessors.MultiPlayerGameModeAccessor;
import dev.loki.lovisual.util.item.FoodUtil;
import dev.loki.lovisual.util.player.ResetAttackCooldown;
import dev.loki.lovisual.util.target.TargetManager;

/**
 * Centralized attack helper with RubberHand-aware handling.
 * <p>
 * Adapted from LiquidBounce (https://github.com/CCBlueX/LiquidBounce).
 */
public enum AttackUtil {
    ;

    public static boolean attack(Minecraft mc, LivingEntity target) {
        return attack(mc, target, null, null, false, true, false, false);
    }

    public static boolean attack(Minecraft mc, LivingEntity target, Float yaw, Float pitch, boolean silent) {
        return attack(mc, target, yaw, pitch, silent, true, false, false);
    }

    public static boolean attack(Minecraft mc,
                                 LivingEntity target,
                                 Float yaw,
                                 Float pitch,
                                 boolean silent,
                                 boolean sendRotationPacket) {
        return attack(mc, target, yaw, pitch, silent, sendRotationPacket, false);
    }

    public static boolean attack(Minecraft mc,
                                 LivingEntity target,
                                 Float yaw,
                                 Float pitch,
                                 boolean silent,
                                 boolean sendRotationPacket,
                                 boolean keepSprint) {
        if (mc == null || mc.player == null || mc.gameMode == null || target == null) {
            return false;
        }

        LocalPlayer player = mc.player;

        boolean usingItem = player.isUsingItem();

        if (usingItem) {
            RubberHand rh = Modules.get(RubberHand.class);
            boolean rhEnabled = rh != null && rh.isEnabled();

            boolean usingShield =
                    player.getUseItem().is(Items.SHIELD) ||
                            player.getOffhandItem().is(Items.SHIELD);
            boolean usingFood = FoodUtil.isFood(player.getUseItem());

            if (usingFood) {
                if (!rhEnabled || !rh.canAttackWhileEating()) {
                    return false;
                }
                return performAttack(mc, player, target, true);
            }

            if (usingShield) {
                if (!rhEnabled || !rh.canAttackWhileShield()) {
                    return false;
                }
                boolean usePressed = mc.options.keyUse.isDown();
                player.releaseUsingItem();
                mc.options.keyUse.setDown(false);
                boolean result = performAttack(mc, player, target, keepSprint);
                if (usePressed) {
                    mc.options.keyUse.setDown(true);
                }
                return result;
            }

            return false;
        }

        boolean attacked = performAttack(mc, player, target, keepSprint);
        return attacked;
    }

    public static boolean attack(Minecraft mc,
                                 LivingEntity target,
                                 Float yaw,
                                 Float pitch,
                                 boolean silent,
                                 boolean sendRotationPacket,
                                 boolean resetSprint,
                                 boolean resumeSprint) {
        return attack(mc, target, yaw, pitch, silent, sendRotationPacket, false);
    }

    private static boolean performAttack(Minecraft mc,
                                         LocalPlayer player,
                                         LivingEntity target,
                                         boolean keepSprint) {
        if (keepSprint) {
            if (!performKeepSprintAttack(mc, player, target)) {
                return false;
            }
        } else {
            mc.gameMode.attack(player, target);
        }

        player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    public static boolean attackCurrentItem(Minecraft mc, LivingEntity target, boolean keepSprint) {
        if (mc == null || mc.player == null || mc.gameMode == null || target == null) {
            return false;
        }

        return performAttack(mc, mc.player, target, keepSprint);
    }

    private static boolean performKeepSprintAttack(Minecraft mc,
                                                   LocalPlayer player,
                                                   LivingEntity target) {
        if (mc.getConnection() == null) return false;
        if (mc.gameMode instanceof MultiPlayerGameModeAccessor accessor) {
            accessor.lovisual$syncSelectedSlot();
        }

        notifyLocalAttack(target);
        mc.getConnection().send(new ServerboundInteractPacket(target.getId(), null, null, player.isShiftKeyDown()));
        ResetAttackCooldown.resetAttackCooldown(player);
        return true;
    }

    private static void notifyLocalAttack(LivingEntity target) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && Events.BUS.hasListeners(AttackEntityEvent.class)) {
            Events.BUS.post(new AttackEntityEvent(player, target));
        }
        HitSounds hitSounds = Modules.get(HitSounds.class);
        if (hitSounds != null) {
            hitSounds.handleHit(target);
        }
        HitEffect hitEffect = Modules.get(HitEffect.class);
        if (hitEffect != null) {
            hitEffect.handleHit(target);
        }
        TargetHud.notifyHit(target);
        TargetManager.onAttack(target);
    }

    public static void sendRotationPacket(Minecraft mc,
                                          LocalPlayer player,
                                          float yaw,
                                          float pitch) {
        if (mc.getConnection() == null) return;
        mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                player.getX(),
                player.getY(),
                player.getZ(),
                yaw,
                pitch,
                player.onGround(),
                player.horizontalCollision
        ));
    }

    public static void setSprinting(Minecraft mc, LocalPlayer player, boolean sprinting) {
        if (player == null) return;
        if (player.isSprinting() == sprinting) return;
        player.setSprinting(sprinting);
        if (mc.getConnection() == null) return;
        mc.getConnection().send(new ServerboundPlayerCommandPacket(
                player,
                sprinting ? ServerboundPlayerCommandPacket.Action.START_SPRINTING
                        : ServerboundPlayerCommandPacket.Action.STOP_SPRINTING
        ));
    }

    public static void sendStopSprinting(Minecraft mc, LocalPlayer player) {
        sendStopSprinting(mc, player, false);
    }

    public static void sendStopSprinting(Minecraft mc, LocalPlayer player, boolean force) {
        if (player == null || mc.getConnection() == null) return;
        if (!force && !player.isSprinting() && !SprintController.INSTANCE.isServerSprinting(player)) return;
        mc.getConnection().send(new ServerboundPlayerCommandPacket(
                player,
                ServerboundPlayerCommandPacket.Action.STOP_SPRINTING
        ));
        if (player instanceof LocalPlayerAccessor accessor) {
            accessor.lovisual$setLastSprinting(false);
        }
    }

}
