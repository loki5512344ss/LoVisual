package dev.loki.lovisual.features.module.modules.visuals.effects.ambient;

import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import dev.loki.lovisual.features.module.modules.visuals.effects.ambient.geometry.WorldParticlesGeometry;
import dev.loki.lovisual.features.module.modules.visuals.effects.ambient.geometry.WorldParticlesSphere;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

/**
 * Рендер-фасад для WorldParticles: ветвление FUNNEL/BUBBLE/QUADS и подготовка батчей.
 * Вынесено из {@link WorldParticles} для соблюдения лимита ≤200 строк.
 */
final class WorldParticlesRender {
    private static final float SPRITE_SIZE_MULTIPLIER = 4.0f;
    private static final float SPRITE_ALPHA_MULTIPLIER = 0.40f;
    private static final float DIAGONAL_ALPHA_MULTIPLIER = 0.40f;
    private static final float OUTLINE_ALPHA_MULTIPLIER = 205.0f / 255.0f;

    private WorldParticlesRender() {}

    static void render(Renderer3D renderer,
                       float tickDelta,
                       List<WorldParticlesParticle> particles,
                       boolean useDepth,
                       Renderer3D.DepthMode depthMode,
                       boolean funnelMode,
                       boolean bubbleMode,
                       float lineWidth,
                       WorldParticlesFunnel.Tint funnelTint,
                       WorldParticlesFunnel.ColorProvider colors) {
        if (funnelMode) {
            WorldParticlesFunnel.render(renderer, useDepth, depthMode, tickDelta, particles, funnelTint, colors);
            return;
        }

        MeshBuilder spriteMesh = WorldParticlesBatches.sprite(renderer, useDepth, depthMode);
        MeshBuilder lineMesh = bubbleMode
                ? null
                : WorldParticlesBatches.lines(renderer, useDepth, depthMode, lineWidth);
        MeshBuilder bubbleShellMesh = bubbleMode
                ? WorldParticlesBatches.bubbleShell(renderer, useDepth, depthMode)
                : null;

        int particleCount = particles.size();
        if (bubbleShellMesh != null) {
            bubbleShellMesh.ensureCapacity(
                    particleCount * (WorldParticlesSphere.STACKS + 1) * (WorldParticlesSphere.SLICES + 1),
                    particleCount * WorldParticlesSphere.STACKS * WorldParticlesSphere.SLICES * 6
            );
        }
        if (spriteMesh != null) {
            spriteMesh.ensureCapacity(
                    bubbleMode ? particleCount * 20 : particleCount * 4,
                    bubbleMode ? particleCount * 30 : particleCount * 6
            );
        }
        if (lineMesh != null) {
            lineMesh.ensureCapacity(particleCount * 32, particleCount * 32);
        }

        Quaternionf camRot = RenderState.cameraRotation;
        for (int i = 0; i < particles.size(); i++) {
            WorldParticlesParticle particle = particles.get(i);
            float alpha = particle.alpha();
            if (alpha <= 0.003f) {
                continue;
            }

            Vec3 pos = particle.interpolatePos(tickDelta);
            int baseColor = colors.colorFor(particle, i);

            if (bubbleMode) {
                WorldParticlesBubble.renderParticle(bubbleShellMesh, spriteMesh, particle, pos, camRot, alpha, baseColor);
                continue;
            }

            if (spriteMesh != null) {
                int spriteArgb = WorldParticlesGeometry.multiplyAlpha(baseColor, alpha * SPRITE_ALPHA_MULTIPLIER);
                WorldParticlesGeometry.addBillboardQuad(spriteMesh, pos.x, pos.y, pos.z,
                        particle.size() * SPRITE_SIZE_MULTIPLIER, camRot, spriteArgb);
            }

            if (lineMesh != null) {
                Vec3 rotation = particle.interpolateRotation(tickDelta);
                int diagonalArgb = WorldParticlesGeometry.multiplyAlpha(baseColor, alpha * DIAGONAL_ALPHA_MULTIPLIER);
                int outlineArgb = WorldParticlesGeometry.multiplyAlpha(baseColor, alpha * OUTLINE_ALPHA_MULTIPLIER);
                WorldParticlesGeometry.addCube(lineMesh, pos, rotation, particle.size(), diagonalArgb, outlineArgb);
            }
        }
    }
}
