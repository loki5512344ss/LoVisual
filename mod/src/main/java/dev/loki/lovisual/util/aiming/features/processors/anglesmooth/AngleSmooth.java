/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.aiming.features.processors.anglesmooth;

import dev.loki.lovisual.util.aiming.rotation.RotationTarget;
import dev.loki.lovisual.util.aiming.data.Rotation;
import dev.loki.lovisual.util.aiming.features.processors.RotationProcessor;

public abstract class AngleSmooth implements RotationProcessor {

    private final String name;

    protected AngleSmooth(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public abstract int calculateTicks(Rotation currentRotation, Rotation targetRotation);

    @Override
    public abstract Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation);
}
