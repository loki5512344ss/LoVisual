/*
 * This file is part of the LoVisual Client distribution.
 * LoVisual modifications copyright (c) 2026 pivosos2007.
 *
 * Portions of this file are based on LiquidBounce
 * (https://github.com/CCBlueX/LiquidBounce).
 * Copyright (c) 2015-2026 CCBlueX.
 *
 * LiquidBounce portions are licensed under GPLv3-or-later.
 * LoVisual modifications are licensed under GPLv3.
 * See THIRD_PARTY_NOTICES.md for details.
 */

package dev.loki.lovisual.util.aiming.features.processors.anglesmooth;

import dev.loki.lovisual.util.aiming.RotationTarget;
import dev.loki.lovisual.util.aiming.RotationUtil;
import dev.loki.lovisual.util.aiming.data.Rotation;
import dev.loki.lovisual.util.aiming.features.processors.RotationProcessor;

/**
 * Angle smooth using per-axis factors.
 * <p>
 * Ported from LiquidBounce (CCBlueX).
 */
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
