package dev.loki.lovisual.render.engine.postprocess.blur;

import dev.loki.lovisual.config.values.mode.EnumValue;

public enum MotionBlurMode implements EnumValue.IdProvider {
    WORLD_AND_HAND("world_and_hand"),
    WORLD_ONLY("world_only");

    private final String id;

    MotionBlurMode(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }
}
