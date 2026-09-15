/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.animation.hand;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

/**
 * Предметы с рукоятью, которые выглядят естественно при "хвате вдоль кулака"
 * (grip): мечи/инструменты/палки. Портировано из идеи Delta HMI
 * (heldAlongTheFist), список расширен по нашему набору ванильных предметов.
 */
public enum HeldAlongFist {
    ;

    public static boolean test(ItemStack item) {
        if (item == null || item.isEmpty()) return false;
        return item.is(ItemTags.SWORDS) || item.is(ItemTags.AXES) || item.is(ItemTags.PICKAXES)
                || item.is(ItemTags.SHOVELS) || item.is(ItemTags.HOES)
                || item.is(Items.MACE) || item.is(Items.TRIDENT)
                || item.is(Items.FISHING_ROD) || item.is(Items.CARROT_ON_A_STICK)
                || item.is(Items.WARPED_FUNGUS_ON_A_STICK) || item.is(Items.STICK)
                || item.is(Items.BLAZE_ROD) || item.is(Items.BREEZE_ROD)
                || item.is(Items.BONE) || item.is(Items.DEBUG_STICK);
    }
}
