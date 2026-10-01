package dev.loki.lovisual.features.module.modules.visuals.effects.hits.blood;

import dev.loki.lovisual.features.module.modules.visuals.effects.hits.blood.BloodParticles.Drop;
import dev.loki.lovisual.features.module.modules.visuals.effects.hits.blood.BloodParticles.Splat;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** KillEffect "Blood" mode: a burst of physically simulated drops that stain the floor. */
public final class BloodKillHelper {
    private static final int MAX_DROPS = 900;
    private static final int MAX_SPLATS = 700;
    private static final double RANGE_SQ = 96.0 * 96.0;

    private final Random random = new Random();
    private final List<Drop> drops = new ArrayList<>();
    private final List<Splat> splats = new ArrayList<>();

    /** @param style BURST, FOUNTAIN, RING or SPRAY (case-insensitive); {@code away} points attacker to victim */
    public void spawn(Vec3 center, float width, float height, int count, float power, float scale, int color,
                      String style, Vec3 away) {
        Vec3 flat = new Vec3(away.x, 0, away.z);
        Vec3 dir = flat.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : flat.normalize();
        BloodPhysics.burst(drops, center, width, height, count, power, scale, color,
                BloodPhysics.Style.of(style), dir, random);
        trim(drops, MAX_DROPS);
    }

    public void tick(ClientLevel level, Entity viewer, float splatSeconds) {
        long now = GameClock.millis();
        long life = (long) (splatSeconds * 1000f);
        splats.removeIf(s -> now - s.startMs() > life);
        if (drops.isEmpty()) return;

        BloodPhysics.Surface surface = surface(level, viewer);
        List<Drop> children = new ArrayList<>();
        drops.removeIf(d -> !BloodPhysics.step(d, surface, splats, children, now, random));
        drops.addAll(children);
        trim(drops, MAX_DROPS);
        trim(splats, MAX_SPLATS);
    }

    public void render(Renderer3D renderer, Vec3 camera, float tickDelta, float splatSeconds) {
        if (renderer == null || (drops.isEmpty() && splats.isEmpty())) return;
        MeshBuilder mesh = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED_DEPTH, Renderer3D.DepthMode.MAIN);
        if (mesh == null) return;

        long now = GameClock.millis();
        float lifeMs = splatSeconds * 1000f;
        for (Splat s : splats) {
            if (s.pos().distanceToSqr(camera) <= RANGE_SQ) {
                BloodGeometry.splat(mesh, s, now, lifeMs);
            }
        }
        float t = Math.max(0f, Math.min(1f, tickDelta));
        for (Drop d : drops) {
            Vec3 p = d.prev.add(d.pos.subtract(d.prev).scale(t));
            if (p.distanceToSqr(camera) <= RANGE_SQ) {
                BloodGeometry.drop(mesh, p, d);
            }
        }
    }

    public void clear() {
        drops.clear();
        splats.clear();
    }

    private static BloodPhysics.Surface surface(ClientLevel level, Entity viewer) {
        return new BloodPhysics.Surface() {
            @Override
            public BloodPhysics.Hit cast(Vec3 from, Vec3 to) {
                BlockHitResult hit = level.clip(new ClipContext(from, to,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, viewer));
                if (hit == null || hit.getType() != HitResult.Type.BLOCK) return null;
                return new BloodPhysics.Hit(hit.getLocation(), hit.getDirection().getStepX(),
                        hit.getDirection().getStepY(), hit.getDirection().getStepZ());
            }

            @Override
            public float light(Vec3 pos) {
                return 0.3f + 0.7f * (level.getMaxLocalRawBrightness(BlockPos.containing(pos)) / 15.0f);
            }
        };
    }

    private static void trim(List<?> list, int max) {
        if (list.size() > max) {
            list.subList(0, list.size() - max).clear();
        }
    }
}
