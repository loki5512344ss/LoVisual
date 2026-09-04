/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.postprocess;

import dev.loki.lovisual.config.values.EnumValue;

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
