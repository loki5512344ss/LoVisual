package dev.loki.lovisual.features.module.modules.visuals.effects.ambient.geometry;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

/**
 * Сферы WorldParticles (пузыри).
 */
public final class WorldParticlesSphere {
    private WorldParticlesSphere() {}

    public static final int STACKS = 5;
    public static final int SLICES = 12;

    public static Vector3f spherePoint(Vec3 center, Quaternionf rotation, float radius, float theta, float phi) {
        float sinTheta = (float) Math.sin(theta);
        return new Vector3f((float) Math.cos(phi) * sinTheta, (float) Math.cos(theta), (float) Math.sin(phi) * sinTheta).rotate(rotation).mul(radius).add((float) center.x, (float) center.y, (float) center.z);
    }

    public static Vector3f sphereNormal(Quaternionf rotation, float theta, float phi) {
        float sinTheta = (float) Math.sin(theta);
        return new Vector3f((float) Math.cos(phi) * sinTheta, (float) Math.cos(theta), (float) Math.sin(phi) * sinTheta).rotate(rotation).normalize();
    }

    public static void addSphereShell(MeshBuilder mesh, Vec3 center, Quaternionf rotation, float radius, int baseColor, int brightColor, float alpha) {
        Vector3f light = new Vector3f(-0.36f, 0.84f, -0.40f).normalize();
        int stacks = STACKS, slices = SLICES;
        int rowSize = slices + 1;
        int baseVertex = mesh.getVertexCount();
        mesh.ensureCapacity((stacks + 1) * rowSize, stacks * slices * 6);
        for (int stack = 0; stack <= stacks; stack++) {
            float theta = (float) Math.PI * stack / (float) stacks;
            for (int slice = 0; slice <= slices; slice++) {
                float phi = (float) (Math.PI * 2.0) * slice / (float) slices;
                Vector3f point = spherePoint(center, rotation, radius, theta, phi);
                int vertexColor = sphereSurfaceColor(baseColor, brightColor, sphereNormal(rotation, theta, phi), light, alpha);
                mesh.vec3(point.x(), point.y(), point.z()).color(new RenderColor(vertexColor)).next();
            }
        }
        for (int stack = 0; stack < stacks; stack++) {
            int row = baseVertex + stack * rowSize;
            int nextRow = row + rowSize;
            for (int slice = 0; slice < slices; slice++) {
                int a = row + slice, b = nextRow + slice, c = nextRow + slice + 1, d = row + slice + 1;
                mesh.triangle(a, b, c); mesh.triangle(c, d, a);
            }
        }
    }

    private static int sphereSurfaceColor(int baseColor, int brightColor, Vector3f normal, Vector3f light, float alpha) {
        float lit = Mth.clamp(normal.dot(light) * 0.5f + 0.5f, 0.0f, 1.0f);
        float rim = 1.0f - Math.abs(normal.z());
        float vertical = Mth.clamp(normal.y() * 0.5f + 0.5f, 0.0f, 1.0f);
        int shadowColor = AnimatedRenderColors.mixArgb(baseColor, 0xFF000000, 0.38f);
        int midColor = AnimatedRenderColors.mixArgb(shadowColor, baseColor, 0.36f + vertical * 0.24f);
        int mixed = AnimatedRenderColors.mixArgb(midColor, brightColor, Mth.clamp(lit * 0.74f + rim * 0.24f, 0.0f, 1.0f));
        return WorldParticlesGeometry.multiplyAlpha(mixed, alpha * (0.22f + lit * 0.22f + rim * 0.16f));
    }
}
