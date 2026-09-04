/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.aiming.features.processors;

import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.util.aiming.RotationTarget;
import dev.loki.lovisual.util.aiming.data.Rotation;

public final class LazyAimProcessor implements RotationProcessor {

    private final NumberValue<Float> threshold;
    private final NumberValue<Float> followFactor;

    public LazyAimProcessor(NumberValue<Float> threshold, NumberValue<Float> followFactor) {
        this.threshold = threshold;
        this.followFactor = followFactor;
    }

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        float angle = currentRotation.angleTo(targetRotation);
        if (angle <= threshold.get()) {
            return currentRotation;
        }

        float factor = Math.max(0.1f, followFactor.get()) * Math.max(angle, 1.0f);
        return currentRotation.towardsLinear(targetRotation, factor, factor);
    }
}
