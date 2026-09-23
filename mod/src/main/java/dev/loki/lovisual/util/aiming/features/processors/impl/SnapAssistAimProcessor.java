/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.aiming.features.processors.impl;
import dev.loki.lovisual.util.aiming.features.processors.RotationProcessor;


import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.util.aiming.rotation.RotationTarget;
import dev.loki.lovisual.util.aiming.data.Rotation;

public final class SnapAssistAimProcessor implements RotationProcessor {

    private final NumberValue<Float> snapThreshold;
    private final NumberValue<Float> snapFactor;

    public SnapAssistAimProcessor(NumberValue<Float> snapThreshold, NumberValue<Float> snapFactor) {
        this.snapThreshold = snapThreshold;
        this.snapFactor = snapFactor;
    }

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        float angle = currentRotation.angleTo(targetRotation);
        if (angle <= snapThreshold.get()) {
            return targetRotation;
        }

        float rampWindow = Math.max(snapThreshold.get() * 2.0f, snapThreshold.get() + 1.0f);
        if (angle > rampWindow) {
            return targetRotation;
        }

        float factor = Math.max(1.0f, snapFactor.get()) * angle;
        return currentRotation.towardsLinear(targetRotation, factor, factor);
    }
}
