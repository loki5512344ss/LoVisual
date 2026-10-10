package dev.loki.lovisual.mixins.optimize.particle;

import dev.loki.lovisual.features.module.modules.misc.optimize.particle.camera.CameraPosition;
import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BaseAshSmokeParticle;
import net.minecraft.client.particle.BubbleColumnUpParticle;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stage 3: per-particle light cache + far-particle physics skip.
 *
 * <p>Two optimizations in one mixin (matches ZakoOpt):
 * <ul>
 *   <li><b>Light cache</b> — vanilla {@code getLightCoords(partialTick)} walks
 *       the chunk's light section data per call, called per particle per frame.
 *       The value only changes on tick boundaries (light updates), so caching
 *       by game tick removes the walk from the hot path.</li>
 *   <li><b>Physics skip</b> — vanilla {@code move} does full collision against
 *       the world for every particle. Far particles (>16 blocks) and three
 *       specific always-skip subclasses ({@link BaseAshSmokeParticle},
 *       {@link CampfireSmokeParticle}, {@link BubbleColumnUpParticle}) skip
 *       the collision check by setting {@code hasPhysics=false} for the
 *       duration of {@code move}.</li>
 * </ul>
 *
 * <p>Ported from {@code zako.opt.mixin.particle.ParticleMixin} (ZakoOpt,
 * LGPL-3.0). Original author: Zako — https://t.me/StarikZako.
 * Light-cache gated on the {@code microOpts} toggle (Stage 1 wiring);
 * physics-skip also gated on {@code microOpts} since both are micro-opts.
 * When the toggle is off, the original code runs unchanged.
 */
@Mixin(Particle.class)
public abstract class ParticleMixin {

    @Shadow
    @Final
    protected ClientLevel level;

    @Shadow
    protected boolean hasPhysics;

    @Shadow
    protected double x;

    @Shadow
    protected double y;

    @Shadow
    protected double z;

    @Unique
    private boolean lovisual$physicsBefore;

    @Unique
    private long lovisual$lightTick = Long.MIN_VALUE;

    @Unique
    private int lovisual$light;

    @Inject(method = "getLightCoords", at = @At("HEAD"), cancellable = true)
    private void lovisual$cachedLight(float partialTick, CallbackInfoReturnable<Integer> cir) {
        if (lovisual$lightTick == level.getGameTime() && OptimizeToggles.particleLight()) {
            cir.setReturnValue(lovisual$light);
        }
    }

    @Inject(method = "getLightCoords", at = @At("RETURN"))
    private void lovisual$storeLight(float partialTick, CallbackInfoReturnable<Integer> cir) {
        lovisual$light = cir.getReturnValueI();
        lovisual$lightTick = level.getGameTime();
    }

    @Inject(method = "move", at = @At("HEAD"))
    private void lovisual$cheapPhysics(double dx, double dy, double dz, CallbackInfo ci) {
        lovisual$physicsBefore = hasPhysics;
        if (hasPhysics && OptimizeToggles.particlePhysics() && lovisual$skipCollision()) {
            hasPhysics = false;
        }
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void lovisual$restorePhysics(double dx, double dy, double dz, CallbackInfo ci) {
        hasPhysics = lovisual$physicsBefore;
    }

    @Unique
    private boolean lovisual$skipCollision() {
        Object self = this;
        if (self instanceof BaseAshSmokeParticle
                || self instanceof CampfireSmokeParticle
                || self instanceof BubbleColumnUpParticle) {
            return true;
        }
        Vec3 cam = CameraPosition.get();
        double ddx = x - cam.x;
        double ddy = y - cam.y;
        double ddz = z - cam.z;
        return ddx * ddx + ddy * ddy + ddz * ddz > 16 * 16;
    }
}
