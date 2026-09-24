package dev.loki.lovisual.util.block.bed;

import dev.loki.lovisual.config.values.mode.EnumValue;

public enum SelfBedMode implements EnumValue.IdProvider {
    NONE("none"),
    COLOR("color"),
    SPAWN_LOCATION("spawn_location"),
    MANUAL("manual");

    private final String id;

    SelfBedMode(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }
}
