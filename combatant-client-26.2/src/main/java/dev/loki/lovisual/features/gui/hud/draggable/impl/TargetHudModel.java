/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

import java.util.List;

record EquipmentEntry(ItemStack stack, float width) {
}

record EquipmentLayout(List<EquipmentEntry> entries, float width, float height) {
}

record EquipmentRenderTask(ItemStack stack, float x, float y, float scale) {
}

record EffectChip(MobEffectInstance effect, Identifier iconId, String text, float width) {
}

record EffectLayout(List<EffectChip> chips, float width, float height) {
}
