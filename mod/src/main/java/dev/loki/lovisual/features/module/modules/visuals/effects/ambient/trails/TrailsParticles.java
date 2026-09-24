package dev.loki.lovisual.features.module.modules.visuals.effects.ambient;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.awt.Color;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import dev.loki.lovisual.features.module.modules.visuals.optimization.FpsOptimizer;
import dev.loki.lovisual.mixins.accessors.projectile.PersistentProjectileEntityAccessor;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.helpers.player.Particle3D;
import dev.loki.lovisual.render.helpers.util.ParticleTextureMode;

final class TrailsParticles {
    private static final float PARTICLE_GRAVITY = 0.01f;
    private static final float PARTICLE_DRAG = 0.96f;
    private static final int PARTICLE_MAX = 600;
    private static final int PROJECTILE_SCAN_RADIUS = 48;
    private static final int PROJECTILE_MAX_SPAWN = 140;

    @SuppressWarnings("unchecked")
    List<Particle3D>[] createBuckets() {
        List<Particle3D>[] b = new List[TextureStorage.RANDOM_PARTICLES.length];
        for (int i = 0; i < b.length; i++) b[i] = new java.util.ArrayList<>();
        return b;
    }

    void clearBuckets(List<Particle3D>[] buckets) {
        for (List<Particle3D> l : buckets) l.clear();
    }

    void updateExtra(List<Particle3D> extra) {
        if (!extra.isEmpty()) extra.removeIf(Particle3D::update);
    }

    void spawnWalk(net.minecraft.world.entity.player.Player player, int colorIndex, Random rnd, List<Particle3D> extra,
                   int walkCount, TrailsColor colors, String mode, int speed, int primary, int secondary,
                   float size, long life, ParticleTextureMode texMode, boolean randomColor) {
        if (walkCount <= 0) return;
        for (int i = 0; i < walkCount; i++) spawnAround(player, colorIndex, rnd, extra, 0.5, 0.4, 0.1,
                colors, mode, speed, primary, secondary, size, life, texMode, randomColor);
    }

    void spawnJump(net.minecraft.world.entity.player.Player player, int colorIndex, Random rnd, List<Particle3D> extra,
                   int jumpCount, TrailsColor colors, String mode, int speed, int primary, int secondary,
                   float size, long life, ParticleTextureMode texMode, boolean randomColor) {
        if (jumpCount <= 0) return;
        for (int i = 0; i < jumpCount; i++) spawnAround(player, colorIndex, rnd, extra, 0.4, 0.1, 0.2,
                colors, mode, speed, primary, secondary, size, life, texMode, randomColor);
    }

    void spawnProjectile(int colorIndex, List<Particle3D> extra, Random rnd, Minecraft mc,
                         int projectileCount, TrailsColor colors, String mode, int speed, int primary, int secondary,
                         float size, long life, ParticleTextureMode texMode, boolean randomColor) {
        if (mc.player == null || projectileCount <= 0) return;
        int spawned = 0;
        var box = mc.player.getBoundingBox().inflate(PROJECTILE_SCAN_RADIUS);
        List<Projectile> list = mc.level.getEntitiesOfClass(Projectile.class, box, e -> true);
        for (Projectile p : list) {
            if (p instanceof AbstractArrow a && isInGround(a)) continue;
            if (spawned >= PROJECTILE_MAX_SPAWN) break;
            for (int i = 0; i < projectileCount && spawned < PROJECTILE_MAX_SPAWN; i++) {
                spawnAt(p.getX() + rnd.nextDouble() * 0.6 - 0.3,
                        p.getY() + rnd.nextDouble() * Math.max(0.2, p.getBbHeight()),
                        p.getZ() + rnd.nextDouble() * 0.6 - 0.3,
                        colorIndex, rnd, extra, 0.12, colors, mode, speed, primary, secondary, size, life, texMode, randomColor);
                spawned++;
            }
        }
    }

    private boolean isInGround(AbstractArrow p) {
        if (p == null) return false;
        try { return ((PersistentProjectileEntityAccessor) p).lovisual$isInGround(); }
        catch (Throwable t) { return false; }
    }

    private void spawnAround(net.minecraft.world.entity.player.Player player, int colorIndex, Random rnd, List<Particle3D> extra,
                             double radius, double height, double motionScale,
                             TrailsColor colors, String mode, int speed, int primary, int secondary,
                             float size, long life, ParticleTextureMode texMode, boolean randomColor) {
        double px = player.getX() + rnd.nextDouble() * radius - radius * 0.5;
        double py = player.getY() + rnd.nextDouble() * Math.max(0.1, height);
        double pz = player.getZ() + rnd.nextDouble() * radius - radius * 0.5;
        spawnAt(px, py, pz, colorIndex, rnd, extra, motionScale, colors, mode, speed, primary, secondary, size, life, texMode, randomColor);
    }

    private void spawnAt(double x, double y, double z, int colorIndex, Random rnd, List<Particle3D> extra,
                         double motionScale, TrailsColor colors, String mode, int speed, int primary, int secondary,
                         float size, long life, ParticleTextureMode texMode, boolean randomColor) {
        if (extra.size() >= PARTICLE_MAX) extra.remove(0);
        double motion = 0.06 * motionScale;
        Vec3 vel = new Vec3(rnd.nextDouble() * motion * 2 - motion, rnd.nextDouble() * 0.03 - 0.015, rnd.nextDouble() * motion * 2 - motion);
        int col = randomColor ? randomColor(rnd) : colors.getColorArgb(mode, speed, colorIndex, primary, secondary);
        ParticleTexture tex = resolve(texMode, rnd);
        long fadeIn = Math.min(200L, life / 4L);
        long fadeOut = Math.min(700L, life / 2L);
        extra.add(new Particle3D(new Vec3(x, y, z), vel, life, fadeIn, fadeOut, size, col, tex.texture(), tex.index(), PARTICLE_DRAG, PARTICLE_GRAVITY, (rnd.nextFloat() - 0.5f) * 8f));
    }

    private int randomColor(Random rnd) {
        float h = rnd.nextFloat(); float s = 0.7f + rnd.nextFloat() * 0.3f; float v = 0.8f + rnd.nextFloat() * 0.2f;
        return Color.HSBtoRGB(h, s, v) | 0xFF000000;
    }

    private ParticleTexture resolve(ParticleTextureMode mode, Random rnd) {
        if (mode == ParticleTextureMode.BLOOM) return new ParticleTexture(TextureStorage.FIRE_FLY, -1);
        int idx = rnd.nextInt(TextureStorage.RANDOM_PARTICLES.length);
        return new ParticleTexture(TextureStorage.RANDOM_PARTICLES[idx], idx);
    }

    void renderExtra(Renderer3D renderer, float tickDelta, boolean useDepth, ParticleTextureMode mode,
                     List<Particle3D> extra, List<Particle3D>[] buckets, TrailsGeometry geo) {
        RenderPipeline pipeline = useDepth ? LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE_LIQUID_IGNORE : LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE;
        Renderer3D.DepthMode dm = useDepth ? Renderer3D.DepthMode.PRE_DEPTH : Renderer3D.DepthMode.MAIN;
        Quaternionf camRot = RenderState.cameraRotation;
        if (mode == ParticleTextureMode.BLOOM) {
            if (FpsOptimizer.bloomDisabled()) return;
            MeshBuilder mesh = renderer.batchTextured(pipeline, TextureStorage.FIRE_FLY, dm);
            if (mesh == null) return;
            for (Particle3D p : extra) renderOne(mesh, p, tickDelta, camRot, geo);
            return;
        }
        clearBuckets(buckets);
        for (Particle3D p : extra) { int idx = p.textureIndex(); if (idx >= 0 && idx < buckets.length) buckets[idx].add(p); }
        for (int i = 0; i < buckets.length; i++) {
            List<Particle3D> b = buckets[i]; if (b.isEmpty()) continue;
            MeshBuilder mesh = renderer.batchTextured(pipeline, TextureStorage.RANDOM_PARTICLES[i], dm);
            if (mesh == null) continue;
            for (Particle3D p : b) renderOne(mesh, p, tickDelta, camRot, geo);
        }
    }

    private void renderOne(MeshBuilder mesh, Particle3D p, float tickDelta, Quaternionf camRot, TrailsGeometry geo) {
        Vec3 pos = p.interpolate(tickDelta); float a = p.alpha(); if (a <= 0.01f) return;
        int argb = geo.withAlpha(p.color(), (int) (((p.color() >>> 24) & 0xFF) * a));
        geo.addBillboardQuad(mesh, pos.x, pos.y, pos.z, p.size(), camRot, argb);
    }

    record ParticleTexture(net.minecraft.resources.Identifier texture, int index) {}
}
