package dev.loki.lovisual.features.module.modules.visuals.effects.hits.blood;

import net.minecraft.world.phys.Vec3;

/** Mutable blood drop in flight and immutable floor splat. */
final class BloodParticles {
    private BloodParticles() {
    }

    static final class Drop {
        Vec3 pos;
        Vec3 prev;
        Vec3 vel;
        final float size;
        final int color;
        final boolean child;
        final float scale;
        float light = 1.0f;
        int age;

        Drop(Vec3 pos, Vec3 vel, float size, int color, float scale, boolean child) {
            this.pos = pos;
            this.prev = pos;
            this.vel = vel;
            this.size = size;
            this.color = color | 0xFF000000;
            this.scale = scale;
            this.child = child;
        }
    }

    record Splat(Vec3 pos, float radius, int color, long startMs, long seed, float light, int layer) {
    }
}
