/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.aiming.features.processors.anglesmooth.impl;

import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.util.aiming.RotationTarget;
import dev.loki.lovisual.util.aiming.data.Rotation;
import dev.loki.lovisual.util.aiming.features.processors.anglesmooth.FactorAngleSmooth;

import java.util.concurrent.ThreadLocalRandom;

public final class SigmoidAngleSmooth extends FactorAngleSmooth {

    private final NumberValue<Float> horizontalMin;
    private final NumberValue<Float> horizontalMax;
    private final NumberValue<Float> verticalMin;
    private final NumberValue<Float> verticalMax;
    private final NumberValue<Float> steepness;
    private final NumberValue<Float> midpoint;

    public SigmoidAngleSmooth(NumberValue<Float> horizontalMin,
                              NumberValue<Float> horizontalMax,
                              NumberValue<Float> verticalMin,
                              NumberValue<Float> verticalMax,
                              NumberValue<Float> steepness,
                              NumberValue<Float> midpoint) {
        super("Sigmoid");
        this.horizontalMin = horizontalMin;
        this.horizontalMax = horizontalMax;
        this.verticalMin = verticalMin;
        this.verticalMax = verticalMax;
        this.steepness = steepness;
        this.midpoint = midpoint;
    }

    private static float random(float min, float max) {
        if (max <= min) return min;
        return (float) (ThreadLocalRandom.current().nextDouble(min, max));
    }

    @Override
    protected float[] calculateFactors(RotationTarget rotationTarget,
                                       Rotation currentRotation,
                                       Rotation targetRotation) {
        float rotationDifference = currentRotation.angleTo(targetRotation);

        float hMin = Math.min(horizontalMin.get(), horizontalMax.get());
        float hMax = Math.max(horizontalMin.get(), horizontalMax.get());
        float vMin = Math.min(verticalMin.get(), verticalMax.get());
        float vMax = Math.max(verticalMin.get(), verticalMax.get());

        float hSpeed = rotationTarget != null ? random(hMin, hMax) : hMin;
        float vSpeed = rotationTarget != null ? random(vMin, vMax) : vMin;

        float hFactor = computeFactor(rotationDifference, hSpeed);
        float vFactor = computeFactor(rotationDifference, vSpeed);
        return new float[]{hFactor, vFactor};
    }

    private float computeFactor(float rotationDifference, float turnSpeed) {
        float scaled = rotationDifference / 120f;
        double sigmoid = 1.0 / (1.0 + Math.exp(-steepness.get() * (scaled - midpoint.get())));
        float interpolated = (float) (sigmoid * turnSpeed);
        if (interpolated < 0f) interpolated = 0f;
        if (interpolated > 180f) interpolated = 180f;
        return interpolated;
    }
}
