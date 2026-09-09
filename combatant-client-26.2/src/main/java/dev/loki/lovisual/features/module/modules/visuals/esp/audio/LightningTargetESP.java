/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.esp.audio;

import dev.loki.lovisual.config.values.BooleanValue;
import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;
import dev.loki.lovisual.events.meta.EventHandler;
import dev.loki.lovisual.events.impl.entity.AttackEntityEvent;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.util.target.TargetManager;
import dev.loki.lovisual.features.module.modules.visuals.esp.ESP;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(
        id = "lightning_target_esp",
        displayName = "Lightning Target ESP",
        category = ModuleCategory.VISUALS
)
public class LightningTargetESP extends Module {

    private final NumberValue<Float> lightningCount = num("lightningCount", "count", 4.0f, 1.0f, 10.0f);
    private final NumberValue<Float> lightningWidth = num("lightningWidth", "width", 1.0f, 0.1f, 3.0f);
    private final BooleanValue hurtColor = bool("hurtColor", "hurt_color", true);

    private float appearValue = 0f;
    private Vec3 lastTargetPos = null;
    private long lastAttackTime = 0L;
    private LivingEntity lastAttackedEntity = null;

    private final ArrayList<Bolt> bolts = new ArrayList<>();
    private long lastBoltSpawn = 0L;

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (event.getTarget() instanceof LivingEntity entity) {
            this.lastAttackTime = System.currentTimeMillis();
            this.lastAttackedEntity = entity;
        }
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        LivingEntity currentTarget = TargetManager.getTarget();
        boolean hasTarget = currentTarget != null && currentTarget.isAlive();

        LivingEntity activeTarget = hasTarget ? currentTarget : lastAttackedEntity;
        boolean hasActiveTarget = activeTarget != null && activeTarget.isAlive();

        appearValue = animateTo(appearValue, hasActiveTarget ? 1f : 0f, 0.05f);

        if (hasActiveTarget) {
            this.lastTargetPos = new Vec3(
                    activeTarget.getX(),
                    activeTarget.getY(),
                    activeTarget.getZ()
            );
        }

        if (appearValue <= 0.001f) {
            if (!hasActiveTarget) {
                lastTargetPos = null;
            }
            return;
        }

        renderLightning(renderer, tickDelta, hasActiveTarget, activeTarget);
    }

    private void renderLightning(Renderer3D renderer, float partialTicks, boolean hasActiveTarget, LivingEntity activeTarget) {
        Minecraft mc = Minecraft.getInstance();
        if (lastTargetPos == null) return;

        long now = System.currentTimeMillis();
        int maxBolts = Math.max(4, Math.round(lightningCount.get()) * 2);
        long spawnInterval = (long) Math.max(15, 65.0f / (lightningCount.get() / 3.0f));

        if (hasActiveTarget && activeTarget != null && activeTarget.isAlive()) {
            if (bolts.size() < maxBolts) {
                if (now - lastBoltSpawn > spawnInterval) {
                    bolts.add(spawnBolt(activeTarget));
                    lastBoltSpawn = now;
                }
            }
        }
        bolts.removeIf(b -> now - b.spawn > b.life);

        if (bolts.isEmpty()) return;

        Vec3 basePos = lastTargetPos;
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        float hurtPC = getHurtPC();
        int baseColor = 0xFF55FFFF;
        int redColor = 0xFFFF3C3C;
        int glowColor = blendColor(baseColor, redColor, hurtPC);
        int coreColor = blendColor(0xFFFFFFFF, redColor, hurtPC);

        float widthScale = lightningWidth.get();

        renderRibbonLayer(renderer, basePos, cam, bolts,
                0.045f * widthScale, setAlpha(glowColor, (int) (appearValue * 45)), now, appearValue);
        renderRibbonLayer(renderer, basePos, cam, bolts,
                0.018f * widthScale, setAlpha(glowColor, (int) (appearValue * 95)), now, appearValue);
        renderRibbonLayer(renderer, basePos, cam, bolts,
                0.006f * widthScale, setAlpha(coreColor, (int) (appearValue * 235)), now, appearValue);
    }

    private void renderRibbonLayer(Renderer3D renderer, Vec3 base, Vec3 cam,
                                   ArrayList<Bolt> bolts, float width, int color, long now, float appearValue) {
        MeshBuilder mesh = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED, Renderer3D.DepthMode.NONE);
        if (mesh == null) return;

        for (Bolt bolt : bolts) {
            float alpha = boltAlpha(bolt, now) * appearValue;
            if (alpha <= 0.02f) continue;
            int a = (color >> 24) & 0xFF;
            int layerA = (int) (a * alpha);
            if (layerA <= 0 || bolt.points.size() < 2) continue;
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            appendRibbonPoints(mesh, base, cam, bolt.points, width, r, g, b, layerA);
            for (List<Vec3> branch : bolt.branches) {
                appendRibbonPoints(mesh, base, cam, branch, width * 0.6f, r, g, b, (int) (layerA * 0.85f));
            }
        }
    }

    private Bolt spawnBolt(LivingEntity target) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float radius = target.getBbWidth() * 0.5f;
        float height = target.getBbHeight();

        double startAngle = random.nextDouble() * Math.PI * 2.0;
        double startY = 0.15 + random.nextDouble() * Math.max(0.2, height - 0.4);
        Vec3 start = new Vec3(
                Math.cos(startAngle) * radius * 0.7,
                startY,
                Math.sin(startAngle) * radius * 0.7
        );

        double theta = random.nextDouble() * Math.PI * 2.0;
        double phi = Math.toRadians((random.nextDouble() - 0.5) * 150.0);
        Vec3 dir = new Vec3(
                Math.cos(phi) * Math.cos(theta),
                Math.sin(phi) * 0.8 + 0.25,
                Math.cos(phi) * Math.sin(theta)
        ).normalize();

        double length = 0.35 + random.nextDouble() * 0.45;
        Vec3 end = start.add(dir.scale(length));

        ArrayList<Vec3> points = new ArrayList<>(List.of(start, end));
        points = displacePoints(points, 0.09, 3);

        ArrayList<List<Vec3>> branches = new ArrayList<>();
        int branchCount = 1 + random.nextInt(3);
        for (int i = 0; i < branchCount; i++) {
            int anchorIndex = 1 + random.nextInt(Math.max(1, points.size() - 2));
            Vec3 anchor = points.get(anchorIndex);
            Vec3 branchDir = randomPerpendicular(dir);
            if (random.nextBoolean()) {
                branchDir = branchDir.scale(-1.0);
            }
            double branchLength = 0.12 + random.nextDouble() * 0.18;
            Vec3 branchEnd = anchor.add(branchDir.scale(branchLength));
            ArrayList<Vec3> branchPoints = new ArrayList<>(List.of(anchor, branchEnd));
            branches.add(displacePoints(branchPoints, 0.05, 2));
        }

        return new Bolt(points, branches, 140L + random.nextLong(160L), random.nextFloat() * 6.28f);
    }

    private ArrayList<Vec3> displacePoints(ArrayList<Vec3> input, double amount, int passes) {
        ArrayList<Vec3> points = new ArrayList<>(input);
        double amt = amount;
        for (int pass = 0; pass < passes; pass++) {
            ArrayList<Vec3> next = new ArrayList<>(points.size() * 2);
            for (int i = 0; i < points.size() - 1; i++) {
                Vec3 a = points.get(i);
                Vec3 b = points.get(i + 1);
                next.add(a);
                Vec3 dir = b.subtract(a);
                if (dir.lengthSqr() < 1.0E-7) {
                    next.add(a.add(b.scale(0.5)));
                    continue;
                }
                Vec3 perp = randomPerpendicular(dir.normalize());
                double offset = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.0 * amt;
                next.add(a.add(b.scale(0.5)).add(perp.scale(offset)));
            }
            next.add(points.get(points.size() - 1));
            points = next;
            amt *= 0.5;
        }
        return points;
    }

    private Vec3 randomPerpendicular(Vec3 dir) {
        Vec3 arbitrary = Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 perp1 = dir.cross(arbitrary).normalize();
        Vec3 perp2 = dir.cross(perp1).normalize();
        double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0;
        return perp1.scale(Math.cos(angle)).add(perp2.scale(Math.sin(angle)));
    }

    private void appendRibbonPoints(MeshBuilder mesh, Vec3 base, Vec3 cam,
                                    List<Vec3> points, float width, int r, int g, int b, int a) {
        if (a <= 0 || points.size() < 2) return;
        float half = width * 0.5f;

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 p1 = base.add(points.get(i));
            Vec3 p2 = base.add(points.get(i + 1));
            Vec3 segDir = p2.subtract(p1);
            if (segDir.lengthSqr() < 1.0E-8) continue;
            Vec3 viewDir = p1.add(p2).scale(0.5).subtract(cam);
            if (viewDir.lengthSqr() < 1.0E-8) continue;
            Vec3 side = segDir.cross(viewDir);
            double sideLen = side.length();
            if (sideLen < 1.0E-8 || !Double.isFinite(sideLen)) continue;
            side = side.scale(1.0 / sideLen);

            float x1 = (float) (p1.x - cam.x);
            float y1 = (float) (p1.y - cam.y);
            float z1 = (float) (p1.z - cam.z);
            float x2 = (float) (p2.x - cam.x);
            float y2 = (float) (p2.y - cam.y);
            float z2 = (float) (p2.z - cam.z);
            float sx = (float) side.x * half;
            float sy = (float) side.y * half;
            float sz = (float) side.z * half;

            int i1 = mesh.vec3(x1 - sx, y1 - sy, z1 - sz).color(r, g, b, a).next();
            int i2 = mesh.vec3(x1 + sx, y1 + sy, z1 + sz).color(r, g, b, a).next();
            int i3 = mesh.vec3(x2 + sx, y2 + sy, z2 + sz).color(r, g, b, a).next();
            int i4 = mesh.vec3(x2 - sx, y2 - sy, z2 - sz).color(r, g, b, a).next();
            mesh.quad(i1, i2, i3, i4);
        }
    }

    private float boltAlpha(Bolt bolt, long now) {
        float life = Math.max(0.0f, Math.min(1.0f, (now - bolt.spawn) / (float) bolt.life));
        float fade = life < 0.18f ? life / 0.18f : 1.0f - (life - 0.18f) / 0.82f;
        float flicker = 0.78f + 0.22f * (float) Math.sin(now * 0.045f + bolt.phase);
        return Math.max(0.0f, Math.min(1.0f, fade)) * flicker;
    }

    private float getHurtPC() {
        if (!hurtColor.get()) return 0f;
        float fromAttack = 0f;
        long elapsed = System.currentTimeMillis() - lastAttackTime;
        if (elapsed < 420L && lastAttackedEntity != null && lastAttackedEntity.isAlive()) {
            fromAttack = 1.0f - (float) elapsed / 420.0f;
        }
        float progress = Math.max(0f, fromAttack);
        return progress * progress * (3.0f - 2.0f * progress);
    }

    private int blendColor(int c1, int c2, float factor) {
        factor = Math.max(0f, Math.min(1f, factor));
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF, a1 = (c1 >> 24) & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF, a2 = (c2 >> 24) & 0xFF;
        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);
        int a = (int) (a1 + (a2 - a1) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int setAlpha(int color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private float animateTo(float current, float target, float delta) {
        if (current < target) return Math.min(current + delta, target);
        if (current > target) return Math.max(current - delta, target);
        return current;
    }

    private static class Bolt {
        final List<Vec3> points;
        final List<List<Vec3>> branches;
        final long spawn;
        final long life;
        final float phase;

        Bolt(List<Vec3> points, List<List<Vec3>> branches, long life, float phase) {
            this.points = points;
            this.branches = branches;
            this.spawn = System.currentTimeMillis();
            this.life = life;
            this.phase = phase;
        }
    }
}