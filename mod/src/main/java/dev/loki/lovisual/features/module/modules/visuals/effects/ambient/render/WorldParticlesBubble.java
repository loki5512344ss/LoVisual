package dev.loki.lovisual.features.module.modules.visuals.effects.ambient;

import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.features.module.modules.visuals.effects.ambient.geometry.WorldParticlesGeometry;
import dev.loki.lovisual.features.module.modules.visuals.effects.ambient.geometry.WorldParticlesSphere;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import org.joml.Quaternionf;

/**
 * Режим AIR_BUBBLES: воздушный пузырь — оболочка-сфера + глоу, заливка,
 * блик и вторичный блик в плоскости билборда.
 */
final class WorldParticlesBubble {
    private static final float FILL_SIZE_MULTIPLIER = 3.15f;
    private static final float GLOW_SIZE_MULTIPLIER = 5.10f;
    private static final float HIGHLIGHT_SIZE_MULTIPLIER = 0.34f;
    private static final float SECONDARY_HIGHLIGHT_SIZE_MULTIPLIER = 0.20f;
    private static final float FILL_ALPHA_MULTIPLIER = 0.34f;
    private static final float GLOW_ALPHA_MULTIPLIER = 0.13f;
    private static final float DEPTH_ALPHA_MULTIPLIER = 0.22f;
    private static final float HIGHLIGHT_ALPHA_MULTIPLIER = 0.74f;

    private WorldParticlesBubble() {}

    static void renderParticle(MeshBuilder shellMesh,
                               MeshBuilder spriteMesh,
                               WorldParticlesParticle particle,
                               Vec3 pos,
                               Quaternionf camRot,
                               float alpha,
                               int baseColor) {
        float ageSeconds = particle.ageMs() / 1000.0f;
        float lifeT = particle.lifeProgress();
        float pulse = 0.5f + 0.5f * (float) Math.sin(ageSeconds * 3.1f + particle.visualPhase());
        float pop = 1.0f + AnimationUtility.smoothstep(pulse) * 0.09f;
        float radius = particle.size() * FILL_SIZE_MULTIPLIER * pop;
        float lightAngle = particle.visualPhase() + 2.35f + ageSeconds * 0.16f;
        float depthAngle = lightAngle + (float) Math.PI;
        int brightColor = AnimatedRenderColors.mixArgb(baseColor, 0xFFFFFFFF, 0.72f);
        int softColor = AnimatedRenderColors.mixArgb(baseColor, 0xFFFFFFFF, 0.32f);
        int shadowColor = AnimatedRenderColors.mixArgb(baseColor, 0xFF000000, 0.42f);
        Quaternionf sphereRotation = new Quaternionf()
                .rotateY(particle.visualPhase() + ageSeconds * 0.34f)
                .rotateX(particle.visualPhase() * 0.47f + ageSeconds * 0.22f)
                .rotateZ(particle.visualPhase() * -0.31f + ageSeconds * 0.16f);
        float sphereRadius = radius * 0.82f;

        if (shellMesh != null) {
            WorldParticlesSphere.addSphereShell(shellMesh, pos, sphereRotation, sphereRadius, baseColor, brightColor, alpha);
        }

        if (spriteMesh == null) {
            return;
        }

        WorldParticlesGeometry.addBillboardQuadGradient(
                spriteMesh,
                pos.x, pos.y, pos.z,
                particle.size() * GLOW_SIZE_MULTIPLIER * pop,
                camRot,
                particle.visualPhase() * 0.22f + ageSeconds * 0.12f,
                WorldParticlesGeometry.multiplyAlpha(shadowColor, alpha * GLOW_ALPHA_MULTIPLIER * 0.58f),
                WorldParticlesGeometry.multiplyAlpha(baseColor, alpha * GLOW_ALPHA_MULTIPLIER * 0.88f),
                WorldParticlesGeometry.multiplyAlpha(brightColor, alpha * GLOW_ALPHA_MULTIPLIER * 1.32f),
                WorldParticlesGeometry.multiplyAlpha(softColor, alpha * GLOW_ALPHA_MULTIPLIER)
        );

        renderDepthShade(spriteMesh, pos, camRot, radius, depthAngle, particle.visualPhase(), pulse, alpha, baseColor, brightColor, softColor, shadowColor);

        float fillAlpha = alpha * FILL_ALPHA_MULTIPLIER;
        WorldParticlesGeometry.addBillboardQuadGradient(
                spriteMesh,
                pos.x, pos.y, pos.z,
                radius,
                camRot,
                particle.visualPhase() * -0.16f,
                WorldParticlesGeometry.multiplyAlpha(shadowColor, fillAlpha * 0.92f),
                WorldParticlesGeometry.multiplyAlpha(baseColor, fillAlpha * 1.06f),
                WorldParticlesGeometry.multiplyAlpha(brightColor, fillAlpha * 0.98f),
                WorldParticlesGeometry.multiplyAlpha(softColor, fillAlpha * 1.18f)
        );

        Vec3 highlight = WorldParticlesGeometry.offsetInBillboardPlane(pos, camRot, lightAngle, radius * 0.42f);
        float highlightAlpha = alpha * HIGHLIGHT_ALPHA_MULTIPLIER * (1.0f - lifeT * 0.22f);
        WorldParticlesGeometry.addBillboardQuadGradient(
                spriteMesh,
                highlight.x, highlight.y, highlight.z,
                particle.size() * HIGHLIGHT_SIZE_MULTIPLIER * (0.74f + pulse * 0.22f),
                camRot,
                particle.visualPhase(),
                WorldParticlesGeometry.multiplyAlpha(softColor, highlightAlpha * 0.46f),
                WorldParticlesGeometry.multiplyAlpha(brightColor, highlightAlpha * 0.72f),
                WorldParticlesGeometry.multiplyAlpha(0xFFFFFFFF, highlightAlpha),
                WorldParticlesGeometry.multiplyAlpha(brightColor, highlightAlpha * 0.86f)
        );

        renderSecondaryHighlight(spriteMesh, pos, camRot, radius, particle.size(), lightAngle, particle.visualPhase(), pulse, alpha, baseColor, brightColor, softColor);
    }

    private static void renderDepthShade(MeshBuilder spriteMesh, Vec3 pos, Quaternionf camRot, float radius,
                                         float depthAngle, float visualPhase, float pulse, float alpha,
                                         int baseColor, int brightColor, int softColor, int shadowColor) {
        Vec3 depth = WorldParticlesGeometry.offsetInBillboardPlane(pos, camRot, depthAngle, radius * 0.22f);
        float depthAlpha = alpha * DEPTH_ALPHA_MULTIPLIER * (0.78f + pulse * 0.18f);
        WorldParticlesGeometry.addBillboardQuadGradient(
                spriteMesh,
                depth.x, depth.y, depth.z,
                radius * 0.58f,
                camRot,
                visualPhase * 0.31f,
                WorldParticlesGeometry.multiplyAlpha(shadowColor, depthAlpha * 0.92f),
                WorldParticlesGeometry.multiplyAlpha(baseColor, depthAlpha * 0.76f),
                WorldParticlesGeometry.multiplyAlpha(softColor, depthAlpha * 0.42f),
                WorldParticlesGeometry.multiplyAlpha(shadowColor, depthAlpha * 0.58f)
        );
    }

    private static void renderSecondaryHighlight(MeshBuilder spriteMesh, Vec3 pos, Quaternionf camRot, float radius,
                                                 float size, float lightAngle, float visualPhase, float pulse, float alpha,
                                                 int baseColor, int brightColor, int softColor) {
        Vec3 secondaryHighlight = WorldParticlesGeometry.offsetInBillboardPlane(pos, camRot, lightAngle - 0.92f, radius * 0.18f);
        float secondaryHighlightAlpha = alpha * HIGHLIGHT_ALPHA_MULTIPLIER * 0.42f * (0.84f + pulse * 0.16f);
        WorldParticlesGeometry.addBillboardQuadGradient(
                spriteMesh,
                secondaryHighlight.x, secondaryHighlight.y, secondaryHighlight.z,
                size * SECONDARY_HIGHLIGHT_SIZE_MULTIPLIER,
                camRot,
                visualPhase * -0.38f,
                WorldParticlesGeometry.multiplyAlpha(baseColor, secondaryHighlightAlpha * 0.44f),
                WorldParticlesGeometry.multiplyAlpha(brightColor, secondaryHighlightAlpha * 0.68f),
                WorldParticlesGeometry.multiplyAlpha(0xFFFFFFFF, secondaryHighlightAlpha * 0.82f),
                WorldParticlesGeometry.multiplyAlpha(softColor, secondaryHighlightAlpha * 0.58f)
        );
    }
}
