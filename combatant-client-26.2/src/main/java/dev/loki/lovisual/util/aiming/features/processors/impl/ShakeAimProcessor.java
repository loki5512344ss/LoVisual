/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.aiming.features.processors.impl;
import dev.loki.lovisual.util.aiming.features.processors.RotationProcessor;


import net.minecraft.client.Minecraft;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.util.aiming.rotation.RotationTarget;
import dev.loki.lovisual.util.aiming.data.Rotation;

public final class ShakeAimProcessor implements RotationProcessor {

    private final NumberValue<Float> yawAmplitude;
    private final NumberValue<Float> pitchAmplitude;
    private final NumberValue<Float> speed;

    public ShakeAimProcessor(NumberValue<Float> yawAmplitude,
                             NumberValue<Float> pitchAmplitude,
                             NumberValue<Float> speed) {
        this.yawAmplitude = yawAmplitude;
        this.pitchAmplitude = pitchAmplitude;
        this.speed = speed;
    }

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return targetRotation;
        }

        float phase = mc.player.tickCount * Math.max(0.01f, speed.get());
        int entityId = rotationTarget != null && rotationTarget.entity != null ? rotationTarget.entity.getId() : 0;
        float seed = entityId * 0.173f;

        float yawOffset = (float) Math.sin(phase + seed) * yawAmplitude.get();
        float pitchOffset = (float) Math.cos(phase * 1.17f + seed) * pitchAmplitude.get();
        return new Rotation(targetRotation.yaw() + yawOffset, targetRotation.pitch() + pitchOffset);
    }
}
