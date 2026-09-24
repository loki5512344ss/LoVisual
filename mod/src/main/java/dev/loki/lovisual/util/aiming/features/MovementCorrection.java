package dev.loki.lovisual.util.aiming.features;

import dev.loki.lovisual.config.values.mode.EnumValue;

public enum MovementCorrection implements EnumValue.IdProvider {
    OFF("Off"),
    STRICT("Strict"),
    SILENT("Silent");

    private final String id;

    MovementCorrection(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }
}
