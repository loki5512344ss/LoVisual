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
