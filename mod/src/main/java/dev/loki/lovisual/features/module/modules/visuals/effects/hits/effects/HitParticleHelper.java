package dev.loki.lovisual.features.module.modules.visuals.effects.hits;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.loki.lovisual.features.module.modules.visuals.optimization.FpsOptimizer;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.helpers.player.Particle3D;
import dev.loki.lovisual.render.helpers.util.ParticleTextureMode;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class HitParticleHelper {
    private static final float HIT_PARTICLE_GRAVITY = 0.02f;
    private static final float HIT_PARTICLE_DRAG = 0.94f;
    private static final int HIT_PARTICLE_MAX = 320;
    private static final int HIT_PARTICLE_SPAWN_CAP_PER_TICK = 32;

    private final HitEffect parent;
    private final Random random = new Random();
    private final List<Particle3D> hitParticles = new ArrayList<>();
    private final List<Particle3D>[] hitParticleBuckets = createBuckets();
    private int lastHitParticleTick = Integer.MIN_VALUE;
    private int spawnedHitParticlesThisTick = 0;

    HitParticleHelper(HitEffect parent) {
        this.parent = parent;
    }

    void clear() {
        hitParticles.clear();
        lastHitParticleTick = Integer.MIN_VALUE;
        spawnedHitParticlesThisTick = 0;
    }

    void tick() {
        if (!hitParticles.isEmpty()) hitParticles.removeIf(Particle3D::update);
    }

    void spawn(LivingEntity target) {
        int count = reserveHitParticleBudget(Math.max(0, parent.hitParticleCount.get()));
        if (count <= 0) return;
        int baseColor = parent.getColorArgb(target.getId() % 360);
        float size = parent.hitParticleSize.get();
        long life = parent.hitParticleLifeMs.get();
        long fadeIn = Math.min(250L, life / 4L);
        long fadeOut = Math.min(900L, life / 2L);
        for (int i = 0; i < count; i++) {
            if (hitParticles.size() >= HIT_PARTICLE_MAX) hitParticles.remove(0);
            double px = target.getX() + random.nextDouble() * 0.6 - 0.3;
            double py = target.getY() + random.nextDouble() * Math.max(0.2, target.getBbHeight());
            double pz = target.getZ() + random.nextDouble() * 0.6 - 0.3;
            double motion = 0.06;
            Vec3 vel = new Vec3(random.nextDouble() * motion * 2 - motion,
                    random.nextDouble() * 0.04 - 0.02,
                    random.nextDouble() * motion * 2 - motion);
            int color = parent.hitParticleRandomColor.get() ? randomColor() : baseColor;
            ParticleTexture tex = resolveParticleTexture();
            hitParticles.add(new Particle3D(new Vec3(px, py, pz), vel, life, fadeIn, fadeOut, size, color,
                    tex.texture(), tex.index(), HIT_PARTICLE_DRAG, HIT_PARTICLE_GRAVITY,
                    (random.nextFloat() - 0.5f) * 8f));
        }
    }

    void render(Renderer3D renderer, float tickDelta) {
        if (hitParticles.isEmpty()) return;
        boolean useDepth = parent.depthTest.get();
        RenderPipeline pipeline = useDepth ? LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE_LIQUID_IGNORE
                : LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE;
        Renderer3D.DepthMode depthMode = useDepth ? Renderer3D.DepthMode.PRE_DEPTH : Renderer3D.DepthMode.MAIN;
        Quaternionf camRot = RenderState.cameraRotation;
        if (parent.hitParticleTexture.get() == ParticleTextureMode.BLOOM) {
            if (FpsOptimizer.bloomDisabled()) return;
            MeshBuilder mesh = renderer.batchTextured(pipeline, TextureStorage.FIRE_FLY, depthMode);
            if (mesh == null) return;
            for (Particle3D p : hitParticles) renderParticle(mesh, p, tickDelta, camRot);
            return;
        }
        clearBuckets(hitParticleBuckets);
        for (Particle3D p : hitParticles) {
            int idx = p.textureIndex();
            if (idx >= 0 && idx < hitParticleBuckets.length) hitParticleBuckets[idx].add(p);
        }
        for (int i = 0; i < hitParticleBuckets.length; i++) {
            List<Particle3D> bucket = hitParticleBuckets[i];
            if (bucket.isEmpty()) continue;
            MeshBuilder mesh = renderer.batchTextured(pipeline, TextureStorage.RANDOM_PARTICLES[i], depthMode);
            if (mesh == null) continue;
            for (Particle3D p : bucket) renderParticle(mesh, p, tickDelta, camRot);
        }
    }

    private void renderParticle(MeshBuilder mesh, Particle3D particle, float tickDelta, Quaternionf camRot) {
        Vec3 pos = particle.interpolate(tickDelta);
        float alpha = particle.alpha();
        if (alpha <= 0.01f) return;
        int argb = HitRenderUtil.applyOpacity(particle.color(), alpha);
        HitRenderUtil.addBillboardQuad(mesh, pos.x, pos.y, pos.z, particle.size(), camRot, argb);
    }

    private int reserveHitParticleBudget(int requested) {
        Minecraft mc = Minecraft.getInstance();
        if (requested <= 0 || mc.player == null) return 0;
        int tick = mc.player.tickCount;
        if (tick != lastHitParticleTick) {
            lastHitParticleTick = tick;
            spawnedHitParticlesThisTick = 0;
        }
        int remaining = HIT_PARTICLE_SPAWN_CAP_PER_TICK - spawnedHitParticlesThisTick;
        if (remaining <= 0) return 0;
        int granted = Math.min(requested, remaining);
        spawnedHitParticlesThisTick += granted;
        return granted;
    }

    private int randomColor() {
        float h = random.nextFloat();
        float s = 0.7f + random.nextFloat() * 0.3f;
        float v = 0.8f + random.nextFloat() * 0.2f;
        return Color.HSBtoRGB(h, s, v) | 0xFF000000;
    }

    private ParticleTexture resolveParticleTexture() {
        if (parent.hitParticleTexture.get() == ParticleTextureMode.BLOOM) {
            return new ParticleTexture(TextureStorage.FIRE_FLY, -1);
        }
        int idx = random.nextInt(TextureStorage.RANDOM_PARTICLES.length);
        return new ParticleTexture(TextureStorage.RANDOM_PARTICLES[idx], idx);
    }

    @SuppressWarnings("unchecked")
    private static List<Particle3D>[] createBuckets() {
        List<Particle3D>[] buckets = new List[TextureStorage.RANDOM_PARTICLES.length];
        for (int i = 0; i < buckets.length; i++) buckets[i] = new ArrayList<>();
        return buckets;
    }

    private static void clearBuckets(List<Particle3D>[] buckets) {
        for (List<Particle3D> bucket : buckets) bucket.clear();
    }

    private record ParticleTexture(Identifier texture, int index) {}
}
