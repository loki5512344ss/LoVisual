/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.events.impl;

import dev.loki.lovisual.events.Event;
import lombok.Getter;
import lombok.Setter;

@Getter
public class LightmapEvent extends Event {
    private final float originalAmbient;
    @Setter
    private float ambientLight;

    public LightmapEvent(float ambientLight) {
        this.originalAmbient = ambientLight;
        this.ambientLight = ambientLight;
    }

    public void raiseAmbientLight(float ambientLight) {
        if (ambientLight > this.ambientLight) {
            this.ambientLight = ambientLight;
        }
    }

    public boolean isModified() {
        return Math.abs(ambientLight - originalAmbient) > 1.0e-4f;
    }
}
