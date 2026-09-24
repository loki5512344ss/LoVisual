package dev.loki.lovisual.features.module.modules.visuals.effects.ambient;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

final class TrailsGeometry {
    int withAlpha(int argb, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    void addBillboardQuad(MeshBuilder mesh, double cx, double cy, double cz,
                          Vector3f right, Vector3f up, int argb) {
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
        int i1 = mesh.vec3(p1x, p1y, p1z).vec2(0, 1).color(new RenderColor(argb)).next();
        int i2 = mesh.vec3(p2x, p2y, p2z).vec2(1, 1).color(new RenderColor(argb)).next();
        int i3 = mesh.vec3(p3x, p3y, p3z).vec2(1, 0).color(new RenderColor(argb)).next();
        int i4 = mesh.vec3(p4x, p4y, p4z).vec2(0, 0).color(new RenderColor(argb)).next();
        mesh.quad(i1, i2, i3, i4);
    }

    void addBillboardQuad(MeshBuilder mesh, double cx, double cy, double cz,
                          float size, Quaternionf camRot, int argb) {
        Vector3f right = new Vector3f(1, 0, 0).rotate(camRot).mul(size);
        Vector3f up = new Vector3f(0, 1, 0).rotate(camRot).mul(size);
        addBillboardQuad(mesh, cx, cy, cz, right, up, argb);
    }

    void addVerticalQuad(MeshBuilder mesh, Vec3 p0, Vec3 p1,
                         float down, float height, int argb0, int argb1) {
        double x1 = p0.x; double z1 = p0.z; double y1 = p0.y + down; double y2 = p0.y + down + height;
        double x2 = p1.x; double z2 = p1.z; double y3 = p1.y + down + height; double y4 = p1.y + down;
        mesh.ensureQuadCapacity();
        int i1 = mesh.vec3(x1, y1, z1).color(new RenderColor(argb0)).next();
        int i2 = mesh.vec3(x1, y2, z1).color(new RenderColor(argb0)).next();
        int i3 = mesh.vec3(x2, y3, z2).color(new RenderColor(argb1)).next();
        int i4 = mesh.vec3(x2, y4, z2).color(new RenderColor(argb1)).next();
        mesh.quad(i1, i2, i3, i4);
    }

    void addLine(MeshBuilder mesh, double x0, double y0, double z0,
                 double x1, double y1, double z1, int argb0, int argb1) {
        mesh.ensureLineCapacity();
        int i1 = mesh.vec3(x0, y0, z0).color(new RenderColor(argb0)).next();
        int i2 = mesh.vec3(x1, y1, z1).color(new RenderColor(argb1)).next();
        mesh.line(i1, i2);
    }
}
