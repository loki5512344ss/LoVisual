package dev.loki.lovisual.features.module.modules.visuals.effects.hits;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.loki.lovisual.mixininterface.entity.ILocalPlayer;
import dev.loki.lovisual.mixininterface.entity.IPlayerAttackCooldown;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.helpers.particle.OrientedQuadRenderer;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.util.combat.hit.AttackHitPoint;
import dev.loki.lovisual.util.time.Timer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class HitBubbleHelper {
    private static final int NORMAL_STARS = 12;
    private static final int CRIT_STARS = 18;
    private static final float CHARGED_THRESHOLD = 0.9f;

    private final HitEffect parent;
    private final Random random = new Random();
    private final List<HitBubble> bubbles = new ArrayList<>();
    private final List<Star> stars = new ArrayList<>();

    HitBubbleHelper(HitEffect parent) {
        this.parent = parent;
    }

    void clear() {
        bubbles.clear();
        stars.clear();
    }

    void handleHit(LivingEntity target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ILocalPlayer access = (ILocalPlayer) mc.player;
        float yaw = access.lovisual$getLastYaw();
        float pitch = access.lovisual$getLastPitch();
        Vec3 point = AttackHitPoint.resolve(target, yaw, pitch);
        if (point == null) point = target.position();
        bubbles.add(new HitBubble(point, -yaw, pitch, new Timer()));
        if (!parent.starsEnabled.get()) return;
        if (!(mc.player instanceof IPlayerAttackCooldown cd)) return;
        float cooldown = cd.lovisual$getAttackCooldownProgress(0.0f);
        if (cooldown < CHARGED_THRESHOLD) return;
        boolean crit = isCritical(mc.player, cooldown);
        spawnStars(point, crit);
    }

    void tick() {
        if (!parent.isBubblesMode() || !parent.starsEnabled.get()) {
            stars.clear();
            return;
        }
        if (!stars.isEmpty()) stars.removeIf(Star::update);
    }

    void render(Renderer3D renderer, float tickDelta) {
        boolean useDepth = parent.depthTest.get();
        RenderPipeline pipeline = useDepth
                ? LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE_LIQUID_IGNORE
                : LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE;
        Renderer3D.DepthMode depthMode = useDepth ? Renderer3D.DepthMode.PRE_DEPTH : Renderer3D.DepthMode.MAIN;
        if (!bubbles.isEmpty()) {
            MeshBuilder mesh = renderer.batchTextured(pipeline, TextureStorage.BUBBLE, depthMode);
            if (mesh != null) {
                for (HitBubble b : bubbles) {
                    float lifeMs = b.life.getPassedTimeMs();
                    float lifeTotal = Math.max(1f, parent.lifeTime.get() * 50f);
                    float angle = -lifeMs / 4f;
                    float factor = Math.min(1f, lifeMs / lifeTotal);
                    float scale = factor * 2f * parent.bubbleScale.get();
                    if (scale <= 0.01f) continue;
                    int c1 = HitRenderUtil.applyOpacity(parent.getColorArgb(270), 1f - factor);
                    int c2 = HitRenderUtil.applyOpacity(parent.getColorArgb(0), 1f - factor);
                    int c3 = HitRenderUtil.applyOpacity(parent.getColorArgb(180), 1f - factor);
                    int c4 = HitRenderUtil.applyOpacity(parent.getColorArgb(90), 1f - factor);
                    OrientedQuadRenderer.addOrientedQuad(mesh, b.pos, b.yaw, b.pitch, angle, scale, c1, c2, c3, c4);
                }
                bubbles.removeIf(b -> b.life.passedMs(parent.lifeTime.get() * 50L));
            }
        }
        if (parent.starsEnabled.get() && !stars.isEmpty()) {
            MeshBuilder mesh = renderer.batchTextured(pipeline, TextureStorage.PARTICLE_STARS, depthMode);
            if (mesh == null) return;
            Quaternionf camRot = RenderState.cameraRotation;
            for (Star s : stars) {
                Vec3 pos = s.interpolate(tickDelta);
                float t = s.getLifeProgress();
                float scale = s.size * (1f - t);
                if (scale <= 0.01f) continue;
                int argb = HitRenderUtil.applyOpacity(s.color, 1f - t);
                HitRenderUtil.addBillboardQuad(mesh, pos.x, pos.y, pos.z, scale, camRot, argb);
            }
        }
    }

    private boolean isCritical(Player attacker, float cooldown) {
        if (cooldown < CHARGED_THRESHOLD) return false;
        if (attacker.onGround()) return false;
        if (attacker.onClimbable()) return false;
        if (attacker.isInWater()) return false;
        if (attacker.isPassenger()) return false;
        return attacker.fallDistance > 0.0f;
    }

    private void spawnStars(Vec3 pos, boolean crit) {
        int count = crit ? CRIT_STARS : NORMAL_STARS;
        int color = crit ? parent.critStarsColor.getArgb() : parent.starsColor.getArgb();
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 0.14 + random.nextDouble() * 0.14;
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            double vy = 0.12 + random.nextDouble() * 0.12;
            long life = 500 + random.nextInt(250);
            float size = 0.18f + random.nextFloat() * 0.12f;
            stars.add(new Star(pos, new Vec3(vx, vy, vz), life, size, color));
        }
    }

    private record HitBubble(Vec3 pos, float yaw, float pitch, Timer life) {}

    private static class Star {
        private final Timer timer;
        private final long lifeMs;
        private final float size;
        private final int color;
        private Vec3 pos;
        private Vec3 prevPos;
        private Vec3 velocity;

        private Star(Vec3 pos, Vec3 velocity, long lifeMs, float size, int color) {
            this.pos = pos;
            this.prevPos = pos;
            this.velocity = velocity;
            this.timer = new Timer();
            this.lifeMs = lifeMs;
            this.size = size;
            this.color = color;
        }

        private boolean update() {
            prevPos = pos;
            pos = pos.add(velocity);
            velocity = velocity.multiply(0.94, 0.94, 0.94).add(0.0, -0.03, 0.0);
            return timer.passedMs(lifeMs);
        }

        private Vec3 interpolate(float tickDelta) {
            return prevPos.lerp(pos, tickDelta);
        }

        private float getLifeProgress() {
            return Math.min(1f, timer.getPassedTimeMs() / (float) lifeMs);
        }
    }
}
