package dev.loki.lovisual.render.helpers.util;

import dev.loki.lovisual.config.values.mode.EnumValue;

public enum ParticleTextureMode implements EnumValue.IdProvider {
    BLOOM("bloom"),
    RANDOM("random");

    private final String id;

    ParticleTextureMode(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }
}
