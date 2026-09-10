/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.aiming.features.processors.anglesmooth;

import dev.loki.lovisual.util.aiming.rotation.RotationTarget;
import dev.loki.lovisual.util.aiming.rotation.RotationUtil;
import dev.loki.lovisual.util.aiming.data.Rotation;
import dev.loki.lovisual.util.aiming.features.processors.RotationProcessor;

public abstract class FactorAngleSmooth extends AngleSmooth implements RotationProcessor {

    protected FactorAngleSmooth(String name) {
        super(name);
    }

    protected abstract float[] calculateFactors(RotationTarget rotationTarget,
                                                Rotation currentRotation,
                                                Rotation targetRotation);

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        float[] factors = calculateFactors(rotationTarget, currentRotation, targetRotation);
        Rotation stepped = currentRotation.towardsLinear(targetRotation, factors[0], factors[1]);
        float gcd = (float) RotationUtil.gcd();
        float snapTolerance = Math.max(gcd * 2.0f, 0.5f);
        if (stepped.angleTo(targetRotation) <= snapTolerance) {
            return targetRotation;
        }
        return stepped;
    }

    @Override
    public int calculateTicks(Rotation currentRotation, Rotation targetRotation) {
        Rotation rot = currentRotation;
        int ticks = -1;
        do {
            float[] factors = calculateFactors(null, rot, targetRotation);
            rot = rot.towardsLinear(targetRotation, factors[0], factors[1]);
            ticks++;
        } while (!rot.approximatelyEquals(targetRotation) && ticks < 80);
        return ticks;
    }
}
