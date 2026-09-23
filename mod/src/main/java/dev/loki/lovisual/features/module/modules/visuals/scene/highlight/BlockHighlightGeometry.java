package dev.loki.lovisual.features.module.modules.visuals.scene;

import net.minecraft.world.phys.AABB;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

final class BlockHighlightGeometry {

    void addGradientLine(MeshBuilder mesh,
                         double x1, double y1, double z1,
                         double x2, double y2, double z2,
                         int argb1, int argb2) {
        mesh.ensureLineCapacity();
        putColorVertex(mesh, x1, y1, z1, argb1);
        int i1 = mesh.next();
        putColorVertex(mesh, x2, y2, z2, argb2);
        int i2 = mesh.next();
        mesh.line(i1, i2);
    }

    void addGradientQuad(MeshBuilder mesh,
                         double x1, double y1, double z1, int c1,
                         double x2, double y2, double z2, int c2,
                         double x3, double y3, double z3, int c3,
                         double x4, double y4, double z4, int c4) {
        mesh.ensureQuadCapacity();
        putColorVertex(mesh, x1, y1, z1, c1);
        int i1 = mesh.next();
        putColorVertex(mesh, x2, y2, z2, c2);
        int i2 = mesh.next();
        putColorVertex(mesh, x3, y3, z3, c3);
        int i3 = mesh.next();
        putColorVertex(mesh, x4, y4, z4, c4);
        int i4 = mesh.next();
        mesh.quad(i1, i2, i3, i4);
    }

    void putColorVertex(MeshBuilder mesh, double x, double y, double z, int argb) {
        mesh.vec3(x, y, z).color(red(argb), green(argb), blue(argb), alpha(argb));
    }

    void addFilledBox(Renderer3D renderer, AABB box, int argb) {
        int a = alpha(argb);
        int r = red(argb);
        int g = green(argb);
        int b = blue(argb);
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;
        renderer.quad(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        renderer.quad(minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a);
        renderer.quad(minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        renderer.quad(minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a);
        renderer.quad(maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a);
        renderer.quad(minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
    }

    void addTexturedBox(MeshBuilder mesh,
                        double x1, double y1, double z1,
                        double x2, double y2, double z2) {
        addTexturedQuad(mesh, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1);
        addTexturedQuad(mesh, x2, y1, z2, x1, y1, z2, x1, y2, z2, x2, y2, z2);
        addTexturedQuad(mesh, x1, y1, z2, x1, y1, z1, x1, y2, z1, x1, y2, z2);
        addTexturedQuad(mesh, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1);
        addTexturedQuad(mesh, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2);
        addTexturedQuad(mesh, x1, y1, z2, x2, y1, z2, x2, y1, z1, x1, y1, z1);
    }

    void addTexturedQuad(MeshBuilder mesh,
                         double x1, double y1, double z1,
                         double x2, double y2, double z2,
                         double x3, double y3, double z3,
                         double x4, double y4, double z4) {
        mesh.ensureQuadCapacity();
        int i1 = mesh.vec3(x1, y1, z1).vec2(0, 0).color(255, 255, 255, 255).next();
        int i2 = mesh.vec3(x2, y2, z2).vec2(1, 0).color(255, 255, 255, 255).next();
        int i3 = mesh.vec3(x3, y3, z3).vec2(1, 1).color(255, 255, 255, 255).next();
        int i4 = mesh.vec3(x4, y4, z4).vec2(0, 1).color(255, 255, 255, 255).next();
        mesh.quad(i1, i2, i3, i4);
    }

    void addGradientBox(MeshBuilder mesh,
                        double x1, double y1, double z1,
                        double x2, double y2, double z2,
                        int c000, int c001, int c010, int c011,
                        int c100, int c101, int c110, int c111) {
        addGradientQuad(mesh, x1, y1, z1, c000, x2, y1, z1, c100, x2, y2, z1, c110, x1, y2, z1, c010);
        addGradientQuad(mesh, x2, y1, z2, c101, x1, y1, z2, c001, x1, y2, z2, c011, x2, y2, z2, c111);
        addGradientQuad(mesh, x1, y1, z2, c001, x1, y1, z1, c000, x1, y2, z1, c010, x1, y2, z2, c011);
        addGradientQuad(mesh, x2, y1, z1, c100, x2, y1, z2, c101, x2, y2, z2, c111, x2, y2, z1, c110);
        addGradientQuad(mesh, x1, y2, z1, c010, x2, y2, z1, c110, x2, y2, z2, c111, x1, y2, z2, c011);
        addGradientQuad(mesh, x1, y1, z2, c001, x2, y1, z2, c101, x2, y1, z1, c100, x1, y1, z1, c000);
    }

    void addEdgeRibbon(MeshBuilder mesh,
                       double x1, double y1, double z1,
                       double x2, double y2, double z2,
                       double overlap,
                       int argb1, int argb2,
                       float thickness) {
        double mx = (x1 + x2) * 0.5;
        double my = (y1 + y2) * 0.5;
        double mz = (z1 + z2) * 0.5;
        double ex = x2 - x1;
        double ey = y2 - y1;
        double ez = z2 - z1;
        double edgeLenSq = ex * ex + ey * ey + ez * ez;
        if (edgeLenSq <= 1.0e-10) return;
        double toCamX = RenderState.cameraPos.x - mx;
        double toCamY = RenderState.cameraPos.y - my;
        double toCamZ = RenderState.cameraPos.z - mz;
        double toCamLenSq = toCamX * toCamX + toCamY * toCamY + toCamZ * toCamZ;
        if (toCamLenSq <= 1.0e-10) return;
        double sideX = ey * toCamZ - ez * toCamY;
        double sideY = ez * toCamX - ex * toCamZ;
        double sideZ = ex * toCamY - ey * toCamX;
        double sideLenSq = sideX * sideX + sideY * sideY + sideZ * sideZ;
        if (sideLenSq <= 1.0e-10) return;
        double sideInv = 1.0 / Math.sqrt(sideLenSq);
        double halfWidth = computeRibbonHalfWidth(mx, my, mz, thickness);
        sideX *= sideInv * halfWidth;
        sideY *= sideInv * halfWidth;
        sideZ *= sideInv * halfWidth;
        double camInv = 1.0 / Math.sqrt(toCamLenSq);
        double biasX = toCamX * camInv * overlap;
        double biasY = toCamY * camInv * overlap;
        double biasZ = toCamZ * camInv * overlap;
        addGradientQuad(
                mesh,
                x1 - sideX + biasX, y1 - sideY + biasY, z1 - sideZ + biasZ, argb1,
                x1 + sideX + biasX, y1 + sideY + biasY, z1 + sideZ + biasZ, argb1,
                x2 + sideX + biasX, y2 + sideY + biasY, z2 + sideZ + biasZ, argb2,
                x2 - sideX + biasX, y2 - sideY + biasY, z2 - sideZ + biasZ, argb2
        );
    }

    double computeRibbonHalfWidth(double x, double y, double z, float thickness) {
        double dx = RenderState.cameraPos.x - x;
        double dy = RenderState.cameraPos.y - y;
        double dz = RenderState.cameraPos.z - z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return 0.003 + thickness * (0.0025 + dist * 0.00018);
    }

    int red(int argb) {
        return (argb >>> 16) & 255;
    }

    int green(int argb) {
        return (argb >>> 8) & 255;
    }

    int blue(int argb) {
        return argb & 255;
    }

    int alpha(int argb) {
        return (argb >>> 24) & 255;
    }
}
