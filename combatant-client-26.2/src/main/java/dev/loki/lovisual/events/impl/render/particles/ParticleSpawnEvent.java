/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.events.impl.render.particles;

import net.minecraft.core.particles.ParticleOptions;
import dev.loki.lovisual.events.Event;

public class ParticleSpawnEvent extends Event {
    private final ParticleOptions options;
    private final double x;
    private final double y;
    private final double z;

    public ParticleSpawnEvent(ParticleOptions options, double x, double y, double z) {
        this.options = options;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public ParticleOptions options() {
        return options;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }
}
