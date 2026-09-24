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
