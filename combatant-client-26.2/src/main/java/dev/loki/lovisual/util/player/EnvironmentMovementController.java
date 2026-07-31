/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
public enum EnvironmentMovementController {
    ;

    private static final float DEFAULT_SLIPPERINESS = 0.6F;
    private static final float DEFAULT_VELOCITY_MULTIPLIER = 1.0F;
    private static final float STICKY_SLIPPERINESS = 0.8F;
    private static final float STICKY_VELOCITY_MULTIPLIER = 0.4F;

    public static Float getSlipperinessOverride(Block block) {
        // NoStun module removed; no slipperiness override remains.
        return null;
    }

    public static Float getVelocityMultiplierOverride(Block block) {
        // NoStun module removed; no velocity multiplier override remains.
        return null;
    }

    public static boolean shouldPreserveHoneySlide(Entity entity) {
        if (isNoStunHoneyEnabled()) {
            return true;
        }

        return false;
    }

    private static boolean isNoStunHoneyEnabled() {
        // NoStun module removed; never preserve honey slide.
        return false;
    }

    private static float vanillaSlipperiness(Block block) {
        if (block == Blocks.HONEY_BLOCK || block == Blocks.SLIME_BLOCK) {
            return STICKY_SLIPPERINESS;
        }

        return DEFAULT_SLIPPERINESS;
    }

    private static float vanillaVelocityMultiplier(Block block) {
        if (block == Blocks.HONEY_BLOCK || block == Blocks.SOUL_SAND) {
            return STICKY_VELOCITY_MULTIPLIER;
        }

        return DEFAULT_VELOCITY_MULTIPLIER;
    }
}
