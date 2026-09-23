/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.aiming.data;

import net.minecraft.world.phys.Vec3;

public record RotationWithVector(Rotation rotation, Vec3 vec) {
}
