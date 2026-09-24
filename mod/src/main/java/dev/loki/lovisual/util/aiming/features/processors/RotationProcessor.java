package dev.loki.lovisual.util.aiming.features.processors;

import dev.loki.lovisual.util.aiming.rotation.RotationTarget;
import dev.loki.lovisual.util.aiming.data.Rotation;

public interface RotationProcessor {

    Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation);
}
