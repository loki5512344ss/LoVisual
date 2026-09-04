/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.aiming.preference;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.util.aiming.data.Rotation;

import java.util.Comparator;

public interface RotationPreference extends Comparator<Rotation> {

    Vec3 getPreferredSpotOnBox(AABB box, Vec3 eyes, double range);
}
