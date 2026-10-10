package dev.loki.lovisual.features.module.modules.misc.optimize.particle;

import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;

/**
 * Per-frame thinning of far particles. Particles beyond 16 blocks are kept
 * every other frame (32+ blocks: every fourth) using
 * {@code System.identityHashCode(particle) >>> 4} as a stable per-particle
 * salt so the choice doesn't flicker.
 *
 * <p>The hot-path API takes the camera position as primitives (no
 * {@code Vec3} allocation); the {@code keep(Object, double)} overload is the
 * pure-logic piece, testable without Minecraft types.
 *
 * <p>The {@code tick} field (sampled once per {@code ParticleEngine.render}) lives on the
 * (Stage 3) particle mixin that owns the per-frame lifecycle; the pure-logic
 * distance decision is here.
 */
public enum ParticleLod {
    ;

    private static final double NEAR_SQ = 16.0 * 16.0;
    private static final double MID_SQ  = 32.0 * 32.0;

    /**
     * True when the particle should be kept this frame. The caller passes the
     * particle's identity (for the per-particle salt) and the squared distance
     * from the camera — both already computed by the renderer.
     */
    public static boolean keep(Object particle, double distanceSq) {
        if (!OptimizeToggles.particleLod()) {
            return true;
        }
        if (distanceSq < NEAR_SQ) {
            return true;
        }
        int h = System.identityHashCode(particle) >>> 4;
        return distanceSq < MID_SQ ? (h & 1) == 0 : (h & 3) == 0;
    }

    /**
     * Convenience overload: compute squared distance from primitives and
     * dispatch to {@link #keep(Object, double)}.
     */
    public static boolean keep(Object particle, double x, double y, double z,
                               double camX, double camY, double camZ) {
        double dx = x - camX, dy = y - camY, dz = z - camZ;
        return keep(particle, dx * dx + dy * dy + dz * dz);
    }
}
