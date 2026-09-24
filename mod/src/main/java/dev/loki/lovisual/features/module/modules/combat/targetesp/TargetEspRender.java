package dev.loki.lovisual.features.module.modules.combat.targetesp;

import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.IntFunction;

public final class TargetEspRender {
    private TargetEspRender() {
    }

    public static void addTriangle(MeshBuilder mesh, Vector3f v1, Vector3f v2, Vector3f v3, int argb) {
        mesh.ensureTriCapacity();
        int i1 = addVertex(mesh, v1.x, v1.y, v1.z, argb);
        int i2 = addVertex(mesh, v2.x, v2.y, v2.z, argb);
        int i3 = addVertex(mesh, v3.x, v3.y, v3.z, argb);
        mesh.triangle(i1, i2, i3);
    }

    public static void addLine(MeshBuilder mesh, Vector3f v1, Vector3f v2, int argb) {
        mesh.ensureLineCapacity();
        int i1 = addVertex(mesh, v1.x, v1.y, v1.z, argb);
        int i2 = addVertex(mesh, v2.x, v2.y, v2.z, argb);
        mesh.line(i1, i2);
    }

    public static int addVertex(MeshBuilder mesh, double x, double y, double z, int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        mesh.vec3(x, y, z).color(r, g, b, a);
        return mesh.next();
    }

    public static int addVertex(MeshBuilder mesh, double x, double y, double z, double u, double v, int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        mesh.vec3(x, y, z).vec2(u, v).color(r, g, b, a);
        return mesh.next();
    }

    public static void addBillboardQuad(MeshBuilder mesh,
                                        double cx, double cy, double cz,
                                        float size, int argb) {
        Quaternionf camRot = RenderState.cameraRotation;
        Vector3f right = new Vector3f(1, 0, 0).rotate(camRot).mul(size);
        Vector3f up = new Vector3f(0, 1, 0).rotate(camRot).mul(size);
        addBillboardVertices(mesh, cx, cy, cz, right, up, argb, argb, argb, argb);
    }

    public static void addTexturedBillboard(MeshBuilder mesh,
                                            double cx, double cy, double cz,
                                            float size, Quaternionf rotation, int argb) {
        Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f).rotate(rotation).mul(size * 0.5f);
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f).rotate(rotation).mul(size * 0.5f);
        addBillboardVertices(mesh, cx, cy, cz, right, up, argb, argb, argb, argb);
    }

    public static void addRotatedBillboard(MeshBuilder mesh, double cx, double cy, double cz,
                                           float size, float angle, IntFunction<Integer> palette) {
        Quaternionf camRot = RenderState.cameraRotation;
        Vector3f right = new Vector3f(1, 0, 0).rotate(camRot);
        Vector3f up = new Vector3f(0, 1, 0).rotate(camRot);

        Vector3f r = new Vector3f(right).mul(size);
        Vector3f u = new Vector3f(up).mul(size);

        float sin = Mth.sin((float) Math.toRadians(angle));
        float cos = Mth.cos((float) Math.toRadians(angle));

        Vector3f rRot = new Vector3f(
                r.x() * cos - u.x() * sin,
                r.y() * cos - u.y() * sin,
                r.z() * cos - u.z() * sin
        );
        Vector3f uRot = new Vector3f(
                r.x() * sin + u.x() * cos,
                r.y() * sin + u.y() * cos,
                r.z() * sin + u.z() * cos
        );

        addBillboardVertices(mesh, cx, cy, cz, rRot, uRot,
                palette.apply(90),
                palette.apply(0),
                palette.apply(180),
                palette.apply(270));
    }

    public static void addBillboardVertices(MeshBuilder mesh, double cx, double cy, double cz,
                                            Vector3f right, Vector3f up,
                                            int c1, int c2, int c3, int c4) {
        double p1x = cx - right.x() - up.x();
        double p1y = cy - right.y() - up.y();
        double p1z = cz - right.z() - up.z();
        double p2x = cx + right.x() - up.x();
        double p2y = cy + right.y() - up.y();
        double p2z = cz + right.z() - up.z();
        double p3x = cx + right.x() + up.x();
        double p3y = cy + right.y() + up.y();
        double p3z = cz + right.z() + up.z();
        double p4x = cx - right.x() + up.x();
        double p4y = cy - right.y() + up.y();
        double p4z = cz - right.z() + up.z();

        mesh.ensureQuadCapacity();
        int i1 = addVertex(mesh, p1x, p1y, p1z, 0, 1, c1);
        int i2 = addVertex(mesh, p2x, p2y, p2z, 1, 1, c2);
        int i3 = addVertex(mesh, p3x, p3y, p3z, 1, 0, c3);
        int i4 = addVertex(mesh, p4x, p4y, p4z, 0, 0, c4);
        mesh.quad(i1, i2, i3, i4);
    }

    public static int applyOpacity(int argb, float opacity) {
        opacity = Math.min(1f, Math.max(0f, opacity));
        int baseA = (argb >>> 24) & 0xFF;
        int a = Mth.clamp((int) (baseA * opacity), 0, 255);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    public static int addBlackholeVertex(MeshBuilder mesh,
                                         Vec3 center,
                                         Vector3f right,
                                         Vector3f up,
                                         float r,
                                         float angle,
                                         float horizon,
                                         float size,
                                         float time,
                                         float spinSpeed,
                                         IntFunction<Integer> palette) {
        float dist = r / size;
        float rot = time * 2.5f * spinSpeed + 1.3f / (dist + 0.05f);
        float ca = (float) Math.cos(rot);
        float sa = (float) Math.sin(rot);
        float ux = dist * (float) Math.cos(angle);
        float uy = dist * (float) Math.sin(angle);
        float rx = ux * ca - uy * sa;
        float ry = ux * sa + uy * ca;
        float spiral = Math.abs((float) Math.sin(rx * 14.0f + ry * 4.0f) * 0.5f + 0.5f);
        float falloff = (float) Math.pow(horizon / Math.max(dist, 0.001f), 2.2);
        float outerFade = smoothstep(1.0f, horizon, dist);
        float brightness = falloff * (0.55f + 0.45f * spiral) * outerFade;

        int color = palette.apply((int) ((Math.toDegrees(angle) + time * 90f * spinSpeed) % 360f));
        color = applyOpacity(color, brightness);

        float wx = (float) center.x + right.x() * ux * size + up.x() * uy * size;
        float wy = (float) center.y + right.y() * ux * size + up.y() * uy * size;
        float wz = (float) center.z + right.z() * ux * size + up.z() * uy * size;
        return addVertex(mesh, wx, wy, wz, color);
    }

    public static int addBlackholeCoreVertex(MeshBuilder mesh,
                                             Vec3 center,
                                             Vector3f right,
                                             Vector3f up,
                                             float r,
                                             float angle,
                                             int argb) {
        float ux = r * (float) Math.cos(angle);
        float uy = r * (float) Math.sin(angle);
        float wx = (float) center.x + right.x() * ux + up.x() * uy;
        float wy = (float) center.y + right.y() * ux + up.y() * uy;
        float wz = (float) center.z + right.z() * ux + up.z() * uy;
        return addVertex(mesh, wx, wy, wz, argb);
    }

    public static double absSinAnimation(double input) {
        return Math.abs(1 + Math.sin(input)) / 2;
    }

    public static double interpolate(double oldValue, double newValue, double interpolationValue) {
        return (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static float interpolateFloat(float oldValue, float newValue, double interpolationValue) {
        return (float) interpolate(oldValue, newValue, interpolationValue);
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = (x - edge0) / (edge1 - edge0);
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }
}
