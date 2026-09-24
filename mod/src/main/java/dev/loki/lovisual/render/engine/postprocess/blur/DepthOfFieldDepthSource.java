package dev.loki.lovisual.render.engine.postprocess.blur;

import dev.loki.lovisual.config.values.mode.EnumValue;

public enum DepthOfFieldDepthSource implements EnumValue.IdProvider {
    WORLD_SCENE("world_scene"),
    MAIN("main"),
    PRE_TRANSLUCENT("pre_translucent"),
    OFF("off");

    private final String id;

    DepthOfFieldDepthSource(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }
}
