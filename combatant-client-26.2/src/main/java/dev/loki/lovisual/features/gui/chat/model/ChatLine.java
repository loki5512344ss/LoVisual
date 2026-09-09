/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.chat.model;

import net.minecraft.network.chat.Component;

public record ChatLine(Component text, long timestampMs) {
    public float ageSeconds() {
        return (System.currentTimeMillis() - timestampMs) / 1000f;
    }
}
