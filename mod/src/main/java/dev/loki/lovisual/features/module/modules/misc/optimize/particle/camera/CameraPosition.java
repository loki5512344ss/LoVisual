package dev.loki.lovisual.features.module.modules.misc.optimize.particle.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Cached camera position accessor for the particle optimization layer.
 * Vanilla {@code Particle.move} recomputes the camera position via
 * {@code Minecraft.getInstance().gameRenderer.mainCamera().position()} per
 * particle per tick — but the camera only moves once per frame. This helper
 * is the single point where the camera position is sampled, so
 * {@link ParticleMixin} reads it without re-walking the gameRenderer chain
 * per particle.
 *
 * <p>Ported from {@code zako.opt.particle.CameraPosition} (ZakoOpt, LGPL-3.0).
 * Original author: Zako — https://t.me/StarikZako. Render-thread only —
 * particle ticking runs on the render thread.
 */
public final class CameraPosition {

    private CameraPosition() {
    }

    /**
     * Sample the current camera position. Caller must not cache the returned
     * {@link Vec3} — it may be a reused instance whose values change between
     * frames.
     */
    public static Vec3 get() {
        return Minecraft.getInstance().gameRenderer.mainCamera().position();
    }
}
