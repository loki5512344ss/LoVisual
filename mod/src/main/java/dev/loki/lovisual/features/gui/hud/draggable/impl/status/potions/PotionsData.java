/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.status;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

final class PotionsData {

    private PotionsData() {}

    static List<Potions.Row> collectRows() {
        List<Potions.Row> rows = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            mc.player.getActiveEffects().stream()
                    .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(I18n.get(a.getDescriptionId()), I18n.get(b.getDescriptionId())))
                    .forEach(inst -> {
                        String name = I18n.get(inst.getDescriptionId());
                        String amp = inst.getAmplifier() > 0 ? Integer.toString(inst.getAmplifier() + 1) : "";
                        String duration = formatDuration(inst);
                        Identifier effectId = inst.getEffect().unwrapKey().map(ResourceKey::identifier).orElse(null);
                        String key = effectId != null ? effectId.toString() : name;
                        Identifier iconId = effectId != null ? Identifier.fromNamespaceAndPath("minecraft", "textures/mob_effect/" + effectId.getPath() + ".png") : null;
                        rows.add(new Potions.Row(key, inst, name, amp, duration, iconId));
                    });
        }
        return rows;
    }

    static String formatDuration(MobEffectInstance inst) {
        int rawTicks = inst.getDuration();
        if (rawTicks < 0) {
            return "**:**";
        }
        int ticks = Math.max(0, rawTicks);
        int minutes = ticks / 1200;
        if (minutes > 60) {
            return "**:**";
        }
        return minutes + ":" + String.format("%02d", (ticks % 1200) / 20);
    }
}
