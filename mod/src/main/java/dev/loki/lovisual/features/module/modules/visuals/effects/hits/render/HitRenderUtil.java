package dev.loki.lovisual.features.module.modules.visuals.effects.hits;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

final class HitRenderUtil {
    private HitRenderUtil() {}

    static int applyOpacity(int argb, float opacity) {
        opacity = Math.min(1f, Math.max(0f, opacity));
        int a = (argb >>> 24) & 0xFF;
        int na = Mth.clamp((int) (a * opacity), 0, 255);
        return (na << 24) | (argb & 0x00FFFFFF);
    }

    static void addBillboardQuad(MeshBuilder mesh, double cx, double cy, double cz,
                                 float size, Quaternionf camRot, int argb) {
        Vector3f right = new Vector3f(1, 0, 0).rotate(camRot).mul(size);
        Vector3f up = new Vector3f(0, 1, 0).rotate(camRot).mul(size);
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

    static void addShapeOutline(MeshBuilder mesh, BlockPos pos, VoxelShape shape, int argb, double eps) {
        if (mesh == null || shape == null) return;
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        VoxelShape offset = shape.move(pos.getX(), pos.getY(), pos.getZ());
        offset.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 0.5;
            double cz = pos.getZ() + 0.5;
            double sx1 = Math.signum(x1 - cx);
            double sy1 = Math.signum(y1 - cy);
            double sz1 = Math.signum(z1 - cz);
            double sx2 = Math.signum(x2 - cx);
            double sy2 = Math.signum(y2 - cy);
            double sz2 = Math.signum(z2 - cz);
            double ax1 = x1 + sx1 * eps;
            double ay1 = y1 + sy1 * eps;
            double az1 = z1 + sz1 * eps;
            double ax2 = x2 + sx2 * eps;
            double ay2 = y2 + sy2 * eps;
            double az2 = z2 + sz2 * eps;
            mesh.ensureLineCapacity();
            int i1 = mesh.vec3(ax1, ay1, az1).color(r, g, b, a).next();
            int i2 = mesh.vec3(ax2, ay2, az2).color(r, g, b, a).next();
            mesh.line(i1, i2);
        });
    }
}
