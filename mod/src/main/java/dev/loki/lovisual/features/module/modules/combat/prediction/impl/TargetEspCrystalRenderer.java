package dev.loki.lovisual.features.module.modules.combat.prediction;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspCrystals.CrystalInstance;
import dev.loki.lovisual.mixininterface.entity.IEntity;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.features.module.modules.misc.optimize.Optimize;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Crystal-orbit rendering subsystem for {@link TargetESP}: owns the per-instance crystal list,
 * rotation angle and last-target tracking, and bakes the additive/fill/line/bloom meshes for the
 * "crystals" mode. Colors are supplied by the caller so the palette stays in the module.
 */
final class TargetEspCrystalRenderer {
    private static final net.minecraft.resources.Identifier CRYSTAL_BLOOM = TextureStorage.BLOOM;

    private final List<CrystalInstance> crystalList = new ArrayList<>();
    private Entity lastCrystalTarget;
    private float crystalRotationAngle;

    void clear() {
        crystalList.clear();
        lastCrystalTarget = null;
    }

    void render(Renderer3D renderer, LivingEntity target, float tickDelta,
                boolean hitRed, float targetAlpha, IntFunction<Integer> argbColorAt) {
        if (renderer == null || target == null) {
            return;
        }
        if (crystalList.isEmpty() || target != lastCrystalTarget) {
            createCrystals(target);
            lastCrystalTarget = target;
        }
        if (crystalList.isEmpty()) {
            return;
        }

        crystalRotationAngle = (crystalRotationAngle + 0.5f) % 360.0f;
        float orbitYawRad = (float) Math.toRadians(crystalRotationAngle);
        float red = hitRed
                ? Mth.clamp((target.hurtTime - tickDelta) / 20.0f, 0.0f, 1.0f)
                : 0.0f;

        Renderer3D.DepthMode depthMode = Renderer3D.DepthMode.PRE_DEPTH;
        MeshBuilder additiveMesh = renderer.batch(
                LoVisualRenderPipelines.WORLD_COLORED_ADDITIVE_DEPTH,
                depthMode
        );
        MeshBuilder fillMesh = renderer.batch(
                LoVisualRenderPipelines.WORLD_COLORED_DEPTH,
                depthMode
        );
        MeshBuilder lineMesh = renderer.batch(
                LoVisualRenderPipelines.WORLD_COLORED_LINES_DEPTH,
                depthMode
        );
        MeshBuilder bloomMesh = Optimize.bloomDisabled()
                ? null
                : renderer.batchTextured(
                        LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE_DEPTH,
                        CRYSTAL_BLOOM,
                        depthMode
                );
        if (additiveMesh == null || fillMesh == null || lineMesh == null) {
            return;
        }

        float prevWidth = RenderState.lineWidth;
        RenderState.lineWidth = 1.0f;
        try {
            Vec3 targetPos = obtainEntityLerpedPos(target, tickDelta);
            for (int i = 0; i < crystalList.size(); i++) {
                CrystalInstance crystal = crystalList.get(i);
                int baseColor = crystalColor(argbColorAt, i, red, targetAlpha);
                crystal.append(additiveMesh, fillMesh, lineMesh, bloomMesh, targetPos, orbitYawRad, baseColor);
            }
        } finally {
            RenderState.lineWidth = prevWidth;
        }
    }

    private int crystalColor(IntFunction<Integer> argbColorAt, int index, float redFactor, float targetAlpha) {
        int baseArgb = argbColorAt.apply(index);
        int out;
        if (redFactor <= 0.0f) {
            out = baseArgb;
        } else {
            int redArgb = (baseArgb & 0xFF000000) | 0x00FF0000;
            out = AnimatedRenderColors.mixArgb(baseArgb, redArgb, redFactor);
        }
        return AnimatedRenderColors.scaleAlpha(out, targetAlpha);
    }

    private void createCrystals(Entity target) {
        crystalList.clear();
        if (target == null) {
            return;
        }
        crystalList.add(new CrystalInstance(new Vec3(0, 0.85, 0.8), new Vec3(-49, 0, 40)));
        crystalList.add(new CrystalInstance(new Vec3(0.2, 0.85, -0.675), new Vec3(35, 0, -30)));
        crystalList.add(new CrystalInstance(new Vec3(0.6, 1.35, 0.6), new Vec3(-30, 0, 35)));
        crystalList.add(new CrystalInstance(new Vec3(-0.74, 1.05, 0.4), new Vec3(-25, 0, -30)));
        crystalList.add(new CrystalInstance(new Vec3(0.74, 0.95, -0.4), new Vec3(0, 0, 0)));
        crystalList.add(new CrystalInstance(new Vec3(-0.475, 0.85, -0.375), new Vec3(30, 0, -25)));
        crystalList.add(new CrystalInstance(new Vec3(0, 1.35, -0.6), new Vec3(45, 0, 0)));
        crystalList.add(new CrystalInstance(new Vec3(0.85, 0.7, 0.1), new Vec3(-30, 0, 30)));
        crystalList.add(new CrystalInstance(new Vec3(-0.7, 1.35, -0.3), new Vec3(0, 0, 0)));
        crystalList.add(new CrystalInstance(new Vec3(-0.3, 1.35, 0.55), new Vec3(0, 0, 0)));
        crystalList.add(new CrystalInstance(new Vec3(-0.5, 0.7, 0.7), new Vec3(0, 0, 0)));
        crystalList.add(new CrystalInstance(new Vec3(0.5, 0.7, 0.7), new Vec3(0, 0, 0)));
        crystalList.add(new CrystalInstance(new Vec3(-0.7, 0.75, 0), new Vec3(0, 0, 0)));
        crystalList.add(new CrystalInstance(new Vec3(-0.2, 0.65, -0.7), new Vec3(0, 0, 0)));
    }

    private Vec3 obtainEntityLerpedPos(Entity e, float f) {
        try {
            return e.getPosition(f);
        } catch (NoSuchMethodError ex) {
            if (e instanceof IEntity access) {
                Vec3 last = access.get$InstantRenderPos();
                return last.lerp(e.position(), f);
            }
            return e.position();
        }
    }
}
