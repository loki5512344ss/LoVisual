/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.item.rarity;
import dev.loki.lovisual.features.module.modules.visuals.esp.overlay.DropESP;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import dev.loki.lovisual.util.item.rarity.ItemRarityColorProvider;
import dev.loki.lovisual.util.item.rarity.RarityColorConfig;

/**
 * Rarity -> RGBA for DropESP. Зачарованные вещи имеют минимум RARE.
 */
public final class RarityColorUtil implements ItemRarityColorProvider {

    public static final RarityColorUtil INSTANCE = new RarityColorUtil();

    private RarityColorUtil() {
    }

    @Override
    public float[] getRarityColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return white();
        }

        Rarity rarity = stack.get(DataComponents.RARITY);
        if (rarity == null) rarity = Rarity.COMMON;

        if (stack.isEnchanted()) {
            rarity = forceAtLeastRarity(rarity, Rarity.RARE);
        }

        int argb = RarityColorConfig.INSTANCE.getColor(rarity);
        int rgb = argb & 0x00FFFFFF;
        float r = (rgb >> 16 & 0xFF) / 255f;
        float g = (rgb >> 8 & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;

        return new float[]{r, g, b, 1f};
    }

    private Rarity forceAtLeastRarity(Rarity current, Rarity min) {
        return current.ordinal() < min.ordinal() ? min : current;
    }

    private float[] white() {
        return new float[]{1f, 1f, 1f, 1f};
    }
}
