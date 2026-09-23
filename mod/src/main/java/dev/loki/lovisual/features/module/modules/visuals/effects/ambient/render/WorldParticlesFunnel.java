package dev.loki.lovisual.features.module.modules.visuals.effects.ambient;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.features.module.modules.visuals.effects.ambient.geometry.WorldParticlesGeometry;
import dev.loki.lovisual.features.module.modules.visuals.optimization.FpsOptimizer;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.color.ColorBlendMode;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.color.ColorEffects;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.engine.uniform.impl.hand.effects.TextureTintUniforms;
import org.joml.Quaternionf;

import java.util.List;

/**
 * Режим FUNNEL: воронка — дисторшн-кольца, «глаз», bloom-лики и вспышка.
 */
final class WorldParticlesFunnel {
    private static final float EYE_SIZE_MULTIPLIER = 4.35f;
    private static final float DISTORTION_SIZE_MULTIPLIER = 5.25f;
    private static final float DISTORTION_OUTER_SIZE_MULTIPLIER = 6.85f;
    private static final float DISTORTION_INNER_SIZE_MULTIPLIER = 3.95f;
    private static final float EYE_ALPHA_MULTIPLIER = 0.72f;
    private static final float DISTORTION_ALPHA_MULTIPLIER = 0.16f;
    private static final float DISTORTION_OUTER_ALPHA_MULTIPLIER = 0.085f;
    private static final float DISTORTION_INNER_ALPHA_MULTIPLIER = 0.115f;
    private static final float RIM_ALPHA_MULTIPLIER = 0.22f;
    private static final float GHOST_ALPHA_MULTIPLIER = 0.18f;
    private static final float FLASH_ALPHA_MULTIPLIER = 0.34f;
    private static final float LEAK_ALPHA_MULTIPLIER = 0.32f;

    interface ColorProvider {
        int colorFor(WorldParticlesParticle particle, int index);
    }

    record Tint(int rgb, ColorBlendMode mode, float strength) {}

    private WorldParticlesFunnel() {}

    static void render(Renderer3D renderer, boolean useDepth, Renderer3D.DepthMode depthMode, float tickDelta,
                       List<WorldParticlesParticle> particles, Tint tint, ColorProvider colors) {
        TextureTintUniforms.update(
                tint.rgb(),
                tint.mode() == ColorBlendMode.ORIGINAL ? 0.0f : Mth.clamp(tint.strength(), 0.0f, 1.0f),
                tint.mode().ordinal()
        );

        MeshBuilder distortionMesh = WorldParticlesBatches.tintedTexture(renderer, useDepth, depthMode, TextureStorage.FUNNEL_DISTORTION);
        MeshBuilder eyeMesh = WorldParticlesBatches.tintedTexture(renderer, useDepth, depthMode, TextureStorage.FUNNEL_EYE);
        MeshBuilder bloomMesh = FpsOptimizer.bloomDisabled()
                ? null
                : WorldParticlesBatches.tintedTexture(renderer, useDepth, depthMode, TextureStorage.BLOOM);
        if (distortionMesh == null && eyeMesh == null && bloomMesh == null) {
            return;
        }

        int particleCount = particles.size();
        if (distortionMesh != null) {
            distortionMesh.ensureCapacity(particleCount * 12, particleCount * 18);
        }
        if (eyeMesh != null) {
            eyeMesh.ensureCapacity(particleCount * 16, particleCount * 24);
        }
        if (bloomMesh != null) {
            bloomMesh.ensureCapacity(particleCount * 20, particleCount * 30);
        }

        Quaternionf camRot = RenderState.cameraRotation;
        for (int i = 0; i < particles.size(); i++) {
            WorldParticlesParticle particle = particles.get(i);
            float alpha = particle.funnelAlpha();
            if (alpha <= 0.003f) {
                continue;
            }

            Vec3 pos = particle.interpolatePos(tickDelta);
            int baseColor = colors.colorFor(particle, i);
            float ageSeconds = particle.ageMs() / 1000.0f;
            float scale = particle.funnelScale();
            float pulse = 0.5f + 0.5f * (float) Math.sin(particle.ageMs() * 0.0048f + particle.visualPhase());
            float rimPulse = AnimationUtility.smoothstep(pulse);

            renderDistortion(distortionMesh, particle, pos, camRot, alpha, scale, baseColor, ageSeconds, rimPulse);
            renderEye(eyeMesh, particle, pos, camRot, alpha, scale, baseColor, ageSeconds, rimPulse);

            if (bloomMesh != null) {
                renderLeaks(bloomMesh, particle, pos, camRot, alpha, scale, baseColor, ageSeconds);
                renderFlash(bloomMesh, particle, pos, camRot, alpha, scale, baseColor);
            }
        }
    }

    private static void renderDistortion(MeshBuilder mesh, WorldParticlesParticle particle, Vec3 pos,
                                         Quaternionf camRot, float alpha, float scale, int baseColor,
                                         float ageSeconds, float rimPulse) {
        if (mesh == null) return;
        float outerRoll = particle.visualPhase() * 0.63f - ageSeconds * particle.distortionSpin() * 0.54f;
        int outerArgb = funnelColor(baseColor, 0xFFFFFF, alpha * DISTORTION_OUTER_ALPHA_MULTIPLIER * (0.82f + rimPulse * 0.18f));
        addQuad(mesh, pos, camRot,
                particle.size() * DISTORTION_OUTER_SIZE_MULTIPLIER * scale * (1.0f + rimPulse * 0.055f),
                outerRoll, outerArgb);

        float distortionRoll = particle.visualPhase() + ageSeconds * particle.distortionSpin();
        int distortionArgb = funnelColor(baseColor, 0xFFFFFF, alpha * DISTORTION_ALPHA_MULTIPLIER);
        addQuad(mesh, pos, camRot, particle.size() * DISTORTION_SIZE_MULTIPLIER * scale, distortionRoll, distortionArgb);

        float innerRoll = -particle.visualPhase() * 1.18f + ageSeconds * (0.18f - particle.distortionSpin() * 0.35f);
        int innerArgb = funnelColor(baseColor, 0xFFFFFF, alpha * DISTORTION_INNER_ALPHA_MULTIPLIER * (1.0f - rimPulse * 0.25f));
        addQuad(mesh, pos, camRot, particle.size() * DISTORTION_INNER_SIZE_MULTIPLIER * scale, innerRoll, innerArgb);
    }

    private static void renderEye(MeshBuilder mesh, WorldParticlesParticle particle, Vec3 pos,
                                  Quaternionf camRot, float alpha, float scale, int baseColor,
                                  float ageSeconds, float rimPulse) {
        if (mesh == null) return;
        float eyeRoll = -particle.visualPhase() * 0.42f + ageSeconds * particle.eyeSpin();
        int rimArgb = funnelColor(baseColor, 0xFFC76A, alpha * RIM_ALPHA_MULTIPLIER * (0.62f + rimPulse * 0.38f));
        addQuad(mesh, pos, camRot,
                particle.size() * (EYE_SIZE_MULTIPLIER + 0.48f) * scale * (1.0f + rimPulse * 0.045f),
                eyeRoll - 0.18f, rimArgb);

        int ghostA = funnelColor(baseColor, 0x92E8FF, alpha * GHOST_ALPHA_MULTIPLIER * (0.70f + rimPulse * 0.16f));
        addQuad(mesh, pos, camRot, particle.size() * 3.74f * scale, eyeRoll + 0.74f, ghostA);

        int ghostB = funnelColor(baseColor, 0xFF9D47, alpha * GHOST_ALPHA_MULTIPLIER * 0.68f * (1.0f - rimPulse * 0.18f));
        addQuad(mesh, pos, camRot, particle.size() * 4.88f * scale, eyeRoll - 1.06f, ghostB);

        int eyeArgb = funnelColor(baseColor, 0xFFFFFF, alpha * EYE_ALPHA_MULTIPLIER);
        addQuad(mesh, pos, camRot, particle.size() * EYE_SIZE_MULTIPLIER * scale, eyeRoll, eyeArgb);
    }

    private static void renderFlash(MeshBuilder mesh, WorldParticlesParticle particle, Vec3 pos,
                                    Quaternionf camRot, float alpha, float scale, int baseColor) {
        float lifeT = particle.lifeProgress();
        float flashCenter = 0.34f + ColorEffects.hash01(particle.visualPhase() + 5.73f) * 0.38f;
        float flash = 1.0f - AnimationUtility.smoothstep(Mth.clamp(Math.abs(lifeT - flashCenter) / 0.13f, 0.0f, 1.0f));
        if (flash <= 0.002f) return;
        int flashArgb = funnelColor(baseColor, 0x7FE8FF, alpha * FLASH_ALPHA_MULTIPLIER * flash);
        addQuad(mesh, pos, camRot, particle.size() * (1.45f + flash * 0.82f) * scale, particle.visualPhase() * 0.21f, flashArgb);
    }

    private static void renderLeaks(MeshBuilder mesh, WorldParticlesParticle particle, Vec3 pos,
                                    Quaternionf camRot, float alpha, float scale, int baseColor, float ageSeconds) {
        for (int i = 0; i < 4; i++) {
            float seed = ColorEffects.hash01(particle.visualPhase() + i * 13.17f);
            float angle = particle.visualPhase() + i * 1.5708f + ageSeconds * (0.28f + seed * 0.16f);
            float distance = particle.size() * scale * (1.45f + seed * 1.10f);
            Vec3 leakPos = WorldParticlesGeometry.offsetInBillboardPlane(pos, camRot, angle, distance);

            float wave = 0.5f + 0.5f * (float) Math.sin(ageSeconds * (2.0f + seed * 0.9f) + particle.visualPhase() + i);
            float leakAlpha = alpha * LEAK_ALPHA_MULTIPLIER * (0.45f + AnimationUtility.smoothstep(wave) * 0.55f);
            int leakRgb = switch (i & 3) {
                case 0 -> 0xFF3A24;
                case 1 -> 0xA7FF2E;
                case 2 -> 0xFFD84E;
                default -> 0x55F7FF;
            };

            WorldParticlesGeometry.addBillboardRect(
                    mesh,
                    leakPos.x, leakPos.y, leakPos.z,
                    particle.size() * scale * (0.055f + seed * 0.035f),
                    particle.size() * scale * (0.42f + seed * 0.34f),
                    camRot,
                    angle - 1.5708f,
                    funnelColor(baseColor, leakRgb, leakAlpha)
            );
        }
    }

    private static void addQuad(MeshBuilder mesh, Vec3 pos, Quaternionf camRot, float size, float roll, int argb) {
        WorldParticlesGeometry.addBillboardQuad(mesh, pos.x, pos.y, pos.z, size, camRot, roll, argb);
    }

    private static int funnelColor(int baseArgb, int sourceRgb, float alphaMultiplier) {
        return ColorEffects.argbWithBaseAlpha(baseArgb, sourceRgb, alphaMultiplier);
    }
}
