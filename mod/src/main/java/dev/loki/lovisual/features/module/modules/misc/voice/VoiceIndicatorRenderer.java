package dev.loki.lovisual.features.module.modules.misc.voice;

import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspRender.*;

/** Draws the billboard glyphs of {@link VoiceChat}: a glowing halo with rings, and a crossed-out mic mark. */
final class VoiceIndicatorRenderer {
    private static final int RING_SEGMENTS = 24;
    private static final float RING_WIDTH = 0.045f;
    private static final float CROSS_WIDTH = 0.05f;

    private VoiceIndicatorRenderer() {
    }

    /** @param ringMesh untextured additive mesh; @param haloMesh soft glow texture mesh */
    static void speaking(MeshBuilder ringMesh, MeshBuilder haloMesh, Vec3 at, float size, float level,
                         int argb, double timeSec, boolean pulse) {
        addBillboardQuad(haloMesh, at.x, at.y, at.z, size * 0.9f, applyOpacity(argb, level * 0.55f));
        Vector3f right = new Vector3f(1, 0, 0).rotate(RenderState.cameraRotation);
        Vector3f up = new Vector3f(0, 1, 0).rotate(RenderState.cameraRotation);
        for (int i = 0; i < VoiceIndicatorPulse.RINGS; i++) {
            VoiceIndicatorPulse.Ring ring = VoiceIndicatorPulse.ring(timeSec, i, pulse);
            ring(ringMesh, at, right, up, size * ring.radius(), applyOpacity(argb, level * ring.alpha()));
        }
    }

    /** Red cross, used over your own head when the microphone is muted or disabled. */
    static void muted(MeshBuilder mesh, Vec3 at, float size, int argb) {
        Vector3f right = new Vector3f(1, 0, 0).rotate(RenderState.cameraRotation).mul(size * 0.5f);
        Vector3f up = new Vector3f(0, 1, 0).rotate(RenderState.cameraRotation).mul(size * 0.5f);
        Vector3f normal = new Vector3f(right).cross(up).normalize();
        bar(mesh, at, new Vector3f(right).add(up), new Vector3f(right).sub(up), normal, size, argb);
        bar(mesh, at, new Vector3f(right).sub(up), new Vector3f(right).add(up), normal, size, argb);
    }

    private static void ring(MeshBuilder mesh, Vec3 at, Vector3f right, Vector3f up, float radius, int argb) {
        float inner = Math.max(0f, radius - RING_WIDTH);
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double a1 = i * Math.PI * 2.0 / RING_SEGMENTS;
            double a2 = (i + 1) * Math.PI * 2.0 / RING_SEGMENTS;
            mesh.ensureQuadCapacity();
            int v1 = point(mesh, at, right, up, a1, radius, argb);
            int v2 = point(mesh, at, right, up, a2, radius, argb);
            int v3 = point(mesh, at, right, up, a2, inner, argb);
            int v4 = point(mesh, at, right, up, a1, inner, argb);
            mesh.quad(v1, v2, v3, v4);
        }
    }

    private static int point(MeshBuilder mesh, Vec3 at, Vector3f right, Vector3f up, double angle, float r, int argb) {
        float x = (float) Math.cos(angle) * r;
        float y = (float) Math.sin(angle) * r;
        return addVertex(mesh, at.x + right.x * x + up.x * y, at.y + right.y * x + up.y * y,
                at.z + right.z * x + up.z * y, argb);
    }

    /** Thin camera-facing quad along {@code from -> to} (offsets from {@code at}). */
    private static void bar(MeshBuilder mesh, Vec3 at, Vector3f from, Vector3f to,
                            Vector3f normal, float size, int argb) {
        Vector3f side = new Vector3f(to).sub(from).cross(normal);
        if (side.lengthSquared() < 1.0E-8f) return;
        side.normalize(CROSS_WIDTH * size);
        mesh.ensureQuadCapacity();
        int v1 = addVertex(mesh, at.x + from.x - side.x, at.y + from.y - side.y, at.z + from.z - side.z, argb);
        int v2 = addVertex(mesh, at.x + from.x + side.x, at.y + from.y + side.y, at.z + from.z + side.z, argb);
        int v3 = addVertex(mesh, at.x + to.x + side.x, at.y + to.y + side.y, at.z + to.z + side.z, argb);
        int v4 = addVertex(mesh, at.x + to.x - side.x, at.y + to.y - side.y, at.z + to.z - side.z, argb);
        mesh.quad(v1, v2, v3, v4);
    }
}
