package dev.loki.lovisual.util.aiming.features.processors.anglesmooth;

import dev.loki.lovisual.util.aiming.rotation.RotationTarget;
import dev.loki.lovisual.util.aiming.data.Rotation;

public final class NoneAngleSmooth extends AngleSmooth {

    public NoneAngleSmooth() {
        super("None");
    }

    @Override
    public int calculateTicks(Rotation currentRotation, Rotation targetRotation) {
        return 0;
    }

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        return currentRotation;
    }
}
