package dev.loki.lovisual.features.module.modules.visuals.effects.hits;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.renderer.Renderer3DBatchBindings;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.engine.uniform.impl.world.KillEffectBeamUniforms;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

final class KillBeamHelper {
    private static final long BEAM_LIFE_MS = 2500L;

    private final List<BeamEffect> beamEffects = new ArrayList<>();

    void add(Vec3 pos, int colorArgb, BeamKind kind) {
        beamEffects.add(new BeamEffect(pos, colorArgb, GameClock.millis(), kind));
    }

    void tick() {
        if (beamEffects.isEmpty()) {
            return;
        }
        long now = GameClock.millis();
        beamEffects.removeIf(b -> now - b.createdAtMs > BEAM_LIFE_MS);
    }

    void render(Renderer3D renderer, Minecraft mc, float beamIntensity, float beamSize) {
        renderBeams(renderer, mc, BeamKind.PORTAL,
                LoVisualRenderPipelines.WORLD_KILL_EFFECT_PORTAL, beamIntensity, beamSize);
        renderBeams(renderer, mc, BeamKind.TORNADO,
                LoVisualRenderPipelines.WORLD_KILL_EFFECT_TORNADO, beamIntensity, beamSize);
    }

    void clear() {
        beamEffects.clear();
    }

    boolean isEmpty() {
        return beamEffects.isEmpty();
    }

    private void renderBeams(Renderer3D renderer, Minecraft mc,
                             BeamKind kind, RenderPipeline pipeline,
                             float beamIntensity, float beamSize) {
        if (beamEffects.isEmpty()) {
            return;
        }
        Vec3 camPos = mc.gameRenderer.mainCamera().position();
        Quaternionf camRot = RenderState.cameraRotation;
        long now = GameClock.millis();
        for (BeamEffect beam : beamEffects) {
            if (beam.kind != kind) {
                continue;
            }
            float progress = Math.min(1f, (now - beam.createdAtMs) / (float) BEAM_LIFE_MS);
            float time = (now - beam.createdAtMs) / 1000f;
            int argb = beam.colorArgb;
            float cr = ((argb >> 16) & 255) / 255f;
            float cg = ((argb >> 8) & 255) / 255f;
            float cb = (argb & 255) / 255f;
            float ca = ((argb >> 24) & 255) / 255f;
            KillEffectBeamUniforms.update(time, progress, beamIntensity, beamSize, cr, cg, cb, ca);
            Renderer3DBatchBindings bindings = Renderer3DBatchBindings.none()
                    .withUniform("KillEffectBeam", KillEffectBeamUniforms.get());
            MeshBuilder mesh = renderer.batch(pipeline, Renderer3D.DepthMode.NONE, bindings);
            if (mesh == null) {
                continue;
            }
            if (kind == BeamKind.PORTAL) {
                HitRenderUtil.addBillboardQuad(mesh, beam.pos.x, beam.pos.y + 1.0,
                        beam.pos.z, beamSize, camRot, 0xFFFFFFFF);
            } else {
                addVerticalQuad(mesh, beam.pos, beamSize, beamSize * 2f, camPos, 0xFFFFFFFF);
            }
        }
    }

    private static void addVerticalQuad(MeshBuilder mesh, Vec3 basePos,
                                        float width, float height, Vec3 cameraPos, int argb) {
        Vec3 toCam = new Vec3(cameraPos.x - basePos.x, 0.0, cameraPos.z - basePos.z);
        if (toCam.lengthSqr() < 1.0E-6) {
            toCam = new Vec3(1.0, 0.0, 0.0);
        }
        Vec3 forward = toCam.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        double halfWidth = width / 2.0;
        Vec3 p1 = basePos.add(right.scale(-halfWidth));
        Vec3 p2 = basePos.add(right.scale(halfWidth));
        Vec3 p3 = p2.add(0.0, height, 0.0);
        Vec3 p4 = p1.add(0.0, height, 0.0);
        mesh.ensureQuadCapacity();
        int i1 = mesh.vec3(p1.x, p1.y, p1.z).vec2(0, 0).color(new RenderColor(argb)).next();
        int i2 = mesh.vec3(p2.x, p2.y, p2.z).vec2(1, 0).color(new RenderColor(argb)).next();
        int i3 = mesh.vec3(p3.x, p3.y, p3.z).vec2(1, 1).color(new RenderColor(argb)).next();
        int i4 = mesh.vec3(p4.x, p4.y, p4.z).vec2(0, 1).color(new RenderColor(argb)).next();
        mesh.quad(i1, i2, i3, i4);
    }

    enum BeamKind {
        PORTAL, TORNADO
    }

    private record BeamEffect(Vec3 pos, int colorArgb, long createdAtMs, BeamKind kind) {}
}
