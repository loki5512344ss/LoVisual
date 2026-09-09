/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.item;

import net.minecraft.world.item.Rarity;
import dev.loki.lovisual.config.ConfigNameProvider;
import dev.loki.lovisual.config.ConfigObject;
import dev.loki.lovisual.config.ConfigSerializer;
import dev.loki.lovisual.config.values.ConfigValue;
import dev.loki.lovisual.config.values.RGBColorValue;
import dev.loki.lovisual.features.module.modules.visuals.esp.overlay.DropESP;

import java.util.ArrayList;
import java.util.List;

/**
 * Editable rarity colors (RGB) for tooltips/DropESP/etc.
 */
public final class RarityColorConfig implements ConfigObject, ConfigNameProvider {

    public static final RarityColorConfig INSTANCE = new RarityColorConfig();
    private final RGBColorValue common = new RGBColorValue("rarity_common", "#FFFFFF");
    private final RGBColorValue uncommon = new RGBColorValue("rarity_uncommon", "#55FF55");
    private final RGBColorValue rare = new RGBColorValue("rarity_rare", "#5555FF");
    private final RGBColorValue epic = new RGBColorValue("rarity_epic", "#FF55FF");

    private RarityColorConfig() {
        ConfigSerializer.load(this);
    }

    @Override
    public String getConfigName() {
        return "raritycolorconfig";
    }

    public int getColor(Rarity rarity) {
        if (rarity == null) return common.getArgb();
        return switch (rarity) {
            case UNCOMMON -> uncommon.getArgb();
            case RARE -> rare.getArgb();
            case EPIC -> epic.getArgb();
            default -> common.getArgb();
        };
    }

    public RGBColorValue commonValue() {
        return common;
    }

    public RGBColorValue uncommonValue() {
        return uncommon;
    }

    public RGBColorValue rareValue() {
        return rare;
    }

    public RGBColorValue epicValue() {
        return epic;
    }

    @Override
    public List<ConfigValue<?>> getConfigValues() {
        List<ConfigValue<?>> list = new ArrayList<>();
        list.add(common);
        list.add(uncommon);
        list.add(rare);
        list.add(epic);
        return list;
    }
}
