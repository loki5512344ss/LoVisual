package dev.loki.lovisual.render.engine.postprocess.blur;

import dev.loki.lovisual.config.values.mode.EnumValue;

public enum DepthOfFieldQuality implements EnumValue.IdProvider {
    LOW("low", 8),
    MEDIUM("medium", 14),
    HIGH("high", 22);

    private final String id;
    private final int taps;

    DepthOfFieldQuality(String id, int taps) {
        this.id = id;
        this.taps = taps;
    }

    @Override
    public String id() {
        return id;
    }

    public int taps() {
        return taps;
    }
}
