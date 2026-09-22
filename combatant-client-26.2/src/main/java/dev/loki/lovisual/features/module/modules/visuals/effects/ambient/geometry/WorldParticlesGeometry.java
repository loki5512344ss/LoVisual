package dev.loki.lovisual.features.module.modules.visuals.effects.ambient.geometry;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

/**
 * Геометрия WorldParticles: билдборды, кубы, линии, сферы.
 */
public final class WorldParticlesGeometry {
    private WorldParticlesGeometry() {}

    public static void addBillboardQuad(MeshBuilder mesh, double cx, double cy, double cz, float size, Quaternionf camRot, int argb) {
        addBillboardQuad(mesh, cx, cy, cz, size, camRot, 0.0f, argb);
    }

    public static void addBillboardQuad(MeshBuilder mesh, double cx, double cy, double cz, float size, Quaternionf camRot, float rollRadians, int argb) {
        addBillboardQuadGradient(mesh, cx, cy, cz, size, camRot, rollRadians, argb, argb, argb, argb);
    }

    public static void addBillboardQuadGradient(MeshBuilder mesh, double cx, double cy, double cz, float size, Quaternionf camRot, float rollRadians, int bottomLeftArgb, int bottomRightArgb, int topRightArgb, int topLeftArgb) {
        Vector3f baseRight = new Vector3f(1.0f, 0.0f, 0.0f).rotate(camRot);
        Vector3f baseUp = new Vector3f(0.0f, 1.0f, 0.0f).rotate(camRot);
        float cos = (float) Math.cos(rollRadians);
        float sin = (float) Math.sin(rollRadians);
        Vector3f right = new Vector3f(baseRight).mul(cos).add(new Vector3f(baseUp).mul(sin)).mul(size);
        Vector3f up = new Vector3f(baseUp).mul(cos).sub(new Vector3f(baseRight).mul(sin)).mul(size);
        double p1x = cx - right.x() - up.x(); double p1y = cy - right.y() - up.y(); double p1z = cz - right.z() - up.z();
        double p2x = cx + right.x() - up.x(); double p2y = cy + right.y() - up.y(); double p2z = cz + right.z() - up.z();
        double p3x = cx + right.x() + up.x(); double p3y = cy + right.y() + up.y(); double p3z = cz + right.z() + up.z();
        double p4x = cx - right.x() + up.x(); double p4y = cy - right.y() + up.y(); double p4z = cz - right.z() + up.z();
        mesh.ensureQuadCapacity();
        int i1 = mesh.vec3(p1x, p1y, p1z).vec2(0.0, 1.0).color(new RenderColor(bottomLeftArgb)).next();
        int i2 = mesh.vec3(p2x, p2y, p2z).vec2(1.0, 1.0).color(new RenderColor(bottomRightArgb)).next();
        int i3 = mesh.vec3(p3x, p3y, p3z).vec2(1.0, 0.0).color(new RenderColor(topRightArgb)).next();
        int i4 = mesh.vec3(p4x, p4y, p4z).vec2(0.0, 0.0).color(new RenderColor(topLeftArgb)).next();
        mesh.quad(i1, i2, i3, i4);
    }

    public static void addBillboardRect(MeshBuilder mesh, double cx, double cy, double cz, float halfWidth, float halfHeight, Quaternionf camRot, float rollRadians, int argb) {
        Vector3f baseRight = new Vector3f(1.0f, 0.0f, 0.0f).rotate(camRot);
        Vector3f baseUp = new Vector3f(0.0f, 1.0f, 0.0f).rotate(camRot);
        float cos = (float) Math.cos(rollRadians);
        float sin = (float) Math.sin(rollRadians);
        Vector3f right = new Vector3f(baseRight).mul(cos).add(new Vector3f(baseUp).mul(sin)).mul(halfWidth);
        Vector3f up = new Vector3f(baseUp).mul(cos).sub(new Vector3f(baseRight).mul(sin)).mul(halfHeight);
        double p1x = cx - right.x() - up.x(); double p1y = cy - right.y() - up.y(); double p1z = cz - right.z() - up.z();
        double p2x = cx + right.x() - up.x(); double p2y = cy + right.y() - up.y(); double p2z = cz + right.z() - up.z();
        double p3x = cx + right.x() + up.x(); double p3y = cy + right.y() + up.y(); double p3z = cz + right.z() + up.z();
        double p4x = cx - right.x() + up.x(); double p4y = cy - right.y() + up.y(); double p4z = cz - right.z() + up.z();
        mesh.ensureQuadCapacity();
        int i1 = mesh.vec3(p1x, p1y, p1z).vec2(0.0, 1.0).color(new RenderColor(argb)).next();
        int i2 = mesh.vec3(p2x, p2y, p2z).vec2(1.0, 1.0).color(new RenderColor(argb)).next();
        int i3 = mesh.vec3(p3x, p3y, p3z).vec2(1.0, 0.0).color(new RenderColor(argb)).next();
        int i4 = mesh.vec3(p4x, p4y, p4z).vec2(0.0, 0.0).color(new RenderColor(argb)).next();
        mesh.quad(i1, i2, i3, i4);
    }

    public static void addCube(MeshBuilder mesh, Vec3 pos, Vec3 rotation, float size, int diagonalArgb, int outlineArgb) {
        Quaternionf q = new Quaternionf().rotationXYZ((float) rotation.x, (float) rotation.y, (float) rotation.z);
        Vector3f[] corners = {
                rotatedCorner(-0.5f, -0.5f, -0.5f, q, size, pos),
                rotatedCorner(0.5f, -0.5f, -0.5f, q, size, pos),
                rotatedCorner(0.5f, 0.5f, -0.5f, q, size, pos),
                rotatedCorner(-0.5f, 0.5f, -0.5f, q, size, pos),
                rotatedCorner(-0.5f, -0.5f, 0.5f, q, size, pos),
                rotatedCorner(0.5f, -0.5f, 0.5f, q, size, pos),
                rotatedCorner(0.5f, 0.5f, 0.5f, q, size, pos),
                rotatedCorner(-0.5f, 0.5f, 0.5f, q, size, pos)
        };
        int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        int[][] diags = {{0,6},{1,7},{2,4},{3,5}};
        for (int[] e : edges) addLine(mesh, corners[e[0]], corners[e[1]], outlineArgb);
        for (int[] d : diags) addLine(mesh, corners[d[0]], corners[d[1]], diagonalArgb);
    }

    private static Vector3f rotatedCorner(float x, float y, float z, Quaternionf q, float size, Vec3 pos) {
        return new Vector3f(x, y, z).rotate(q).mul(size).add((float) pos.x, (float) pos.y, (float) pos.z);
    }

    public static void addLine(MeshBuilder mesh, Vector3f from, Vector3f to, int argb) {
        addLineGradient(mesh, from, to, argb, argb);
    }

    public static void addLineGradient(MeshBuilder mesh, Vector3f from, Vector3f to, int fromArgb, int toArgb) {
        mesh.ensureLineCapacity();
        int i1 = mesh.vec3(from.x(), from.y(), from.z()).color(new RenderColor(fromArgb)).next();
        int i2 = mesh.vec3(to.x(), to.y(), to.z()).color(new RenderColor(toArgb)).next();
        mesh.line(i1, i2);
    }
}
