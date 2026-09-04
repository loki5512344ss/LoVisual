/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.aiming.features.processors;

import dev.loki.lovisual.util.aiming.RotationTarget;
import dev.loki.lovisual.util.aiming.data.Rotation;

public interface RotationProcessor {

    Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation);
}
