package dev.loki.lovisual.features.module.modules.combat.targetesp.sword;

import dev.loki.lovisual.features.module.modules.combat.targetesp.sword.SwordPhysics.Frame;
import dev.loki.lovisual.features.module.modules.combat.targetesp.sword.SwordPhysics.Sword;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.math.RenderMath;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Random;
import java.util.function.IntFunction;

import static dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspRender.addBillboardVertices;
import static dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspRender.applyOpacity;

/** "Swords" TargetESP mode: eight glowing swords orbit the target and recoil when it takes a hit. */
public final class TargetEspSwords {
    private static final int HURT_COLOR = 0xFFFF4178;
    private static final float GLOW_SCALE = 1.35f;
    private static final float GLOW_ALPHA = 0.45f;

    private final Sword[] swords = new Sword[SwordPhysics.COUNT];
    private final Random random = new Random();
    private LivingEntity lastTarget;
    private int lastHurt;
    private long lastMs;

    public TargetEspSwords() {
        for (int i = 0; i < swords.length; i++) {
            swords[i] = new Sword();
        }
    }

    public void reset() {
        lastTarget = null;
        lastHurt = 0;
        lastMs = 0L;
    }

    /**
     * @param speedMs   milliseconds per orbit
     * @param radius    orbit radius multiplier
     * @param size      sword size multiplier
     * @param bob       vertical bobbing amplitude in blocks
     */
    public void render(Renderer3D renderer, LivingEntity target, float tickDelta, IntFunction<Integer> palette,
                       long speedMs, float radius, float size, float bob, boolean glow, boolean physics) {
        if (renderer == null || target == null) return;
        long now = GameClock.millis();
        float dt = lastMs == 0L ? 0f : Math.min(0.1f, (now - lastMs) / 1000f);
        lastMs = now;
        float step = Math.max(0.2f, Math.min(2.0f, dt * 60f));

        Vec3 pos = RenderMath.getLerpedPos(target, tickDelta);
        double centerY = pos.y + target.getBbHeight() * 0.45;
        double orbitRadius = (target.getBbWidth() + 0.35) * radius;
        long period = Math.max(200L, speedMs);
        double orbit = (double) (now % period) / period * Math.PI * 2.0;

        if (lastTarget != target) {
            lastTarget = target;
            lastHurt = target.hurtTime;
            SwordPhysics.place(swords, pos.x, pos.z, centerY, orbitRadius, orbit);
        }
        int hurt = target.hurtTime;
        boolean kick = hurt > lastHurt && hurt >= 9;
        lastHurt = hurt;
        float hurtPc = hurt > 0 ? (float) Math.sin(hurt * (Math.PI / 10.0)) : 0f;

        float yawRad = (float) Math.toRadians(net.minecraft.client.Minecraft.getInstance().gameRenderer.mainCamera().yRot());
        SwordPhysics.step(swords, new Frame(pos.x, pos.z, centerY, orbitRadius, orbit, now, bob, step, kick,
                physics, yawRad), random);

        float halfW = 0.20f * size;
        float halfH = 0.40f * size;
        if (glow) {
            MeshBuilder halo = renderer.batchTextured(LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE,
                    TextureStorage.BLOOM, Renderer3D.DepthMode.NONE);
            if (halo != null) draw(halo, palette, hurtPc, halfW * GLOW_SCALE, halfH * GLOW_SCALE, GLOW_ALPHA * (1f + hurtPc * 0.65f));
        }
        MeshBuilder core = renderer.batchTextured(LoVisualRenderPipelines.WORLD_TEXTURED, TextureStorage.SWORD,
                Renderer3D.DepthMode.NONE);
        if (core != null) draw(core, palette, hurtPc, halfW, halfH, 1f);
    }

    private void draw(MeshBuilder mesh, IntFunction<Integer> palette, float hurtPc, float halfW, float halfH, float alpha) {
        for (int i = 0; i < swords.length; i++) {
            Sword s = swords[i];
            int base = AnimatedRenderColors.mixArgb(palette.apply(i * 45), HURT_COLOR, hurtPc);
            int color = applyOpacity(base, alpha);
            Quaternionf rot = new Quaternionf(RenderState.cameraRotation)
                    .rotateZ((float) Math.toRadians(s.roll))
                    .rotateX((float) Math.toRadians(s.pitch));
            Vector3f right = new Vector3f(1, 0, 0).rotate(rot).mul(halfW);
            Vector3f up = new Vector3f(0, 1, 0).rotate(rot).mul(halfH);
            addBillboardVertices(mesh, s.x, s.y, s.z, right, up, color, color, color, color);
        }
    }
}
