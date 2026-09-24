package dev.loki.lovisual.features.gui.chat.model;

import net.minecraft.network.chat.Component;

public record ChatLine(Component text, long timestampMs) {
    public float ageSeconds() {
        return (System.currentTimeMillis() - timestampMs) / 1000f;
    }
}
