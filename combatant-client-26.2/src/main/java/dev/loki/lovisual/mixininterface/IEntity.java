/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixininterface;

import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.render.helpers.TrailPoint;

import java.util.List;

public interface IEntity {
    Vec3 get$InstantRenderPos();

    List<TrailPoint> combatant$getTrails();
}
