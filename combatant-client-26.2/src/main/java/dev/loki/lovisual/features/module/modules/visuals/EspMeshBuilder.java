/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.world.phys.AABB;

final class EspMeshBuilder {

    private EspMeshBuilder() {
    }

    static void addFilledBox(MeshBuilder mesh,
                             AABB box,
                             int c000, int c001, int c010, int c011,
                             int c100, int c101, int c110, int c111) {
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        quad(mesh, minX, minY, minZ, c000, maxX, minY, minZ, c100, maxX, minY, maxZ, c101, minX, minY, maxZ, c001);
        quad(mesh, minX, maxY, minZ, c010, minX, maxY, maxZ, c011, maxX, maxY, maxZ, c111, maxX, maxY, minZ, c110);
        quad(mesh, minX, minY, maxZ, c001, maxX, minY, maxZ, c101, maxX, maxY, maxZ, c111, minX, maxY, maxZ, c011);
        quad(mesh, minX, minY, minZ, c000, minX, maxY, minZ, c010, maxX, maxY, minZ, c110, maxX, minY, minZ, c100);
        quad(mesh, maxX, minY, minZ, c100, maxX, maxY, minZ, c110, maxX, maxY, maxZ, c111, maxX, minY, maxZ, c101);
        quad(mesh, minX, minY, minZ, c000, minX, minY, maxZ, c001, minX, maxY, maxZ, c011, minX, maxY, minZ, c010);
    }

    static void addOutlineBox(MeshBuilder mesh,
                              AABB box,
                              int c000, int c001, int c010, int c011,
                              int c100, int c101, int c110, int c111) {
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        line(mesh, minX, minY, minZ, c000, maxX, minY, minZ, c100);
        line(mesh, maxX, minY, minZ, c100, maxX, minY, maxZ, c101);
        line(mesh, maxX, minY, maxZ, c101, minX, minY, maxZ, c001);
        line(mesh, minX, minY, maxZ, c001, minX, minY, minZ, c000);

        line(mesh, minX, maxY, minZ, c010, maxX, maxY, minZ, c110);
        line(mesh, maxX, maxY, minZ, c110, maxX, maxY, maxZ, c111);
        line(mesh, maxX, maxY, maxZ, c111, minX, maxY, maxZ, c011);
        line(mesh, minX, maxY, maxZ, c011, minX, maxY, minZ, c010);

        line(mesh, minX, minY, minZ, c000, minX, maxY, minZ, c010);
        line(mesh, maxX, minY, minZ, c100, maxX, maxY, minZ, c110);
        line(mesh, maxX, minY, maxZ, c101, maxX, maxY, maxZ, c111);
        line(mesh, minX, minY, maxZ, c001, minX, maxY, maxZ, c011);
    }

    static void quad(MeshBuilder mesh,
                     double x1, double y1, double z1, int c1,
                     double x2, double y2, double z2, int c2,
                     double x3, double y3, double z3, int c3,
                     double x4, double y4, double z4, int c4) {
        mesh.ensureQuadCapacity();
        putVertex(mesh, x1, y1, z1, c1);
        int i1 = mesh.next();
        putVertex(mesh, x2, y2, z2, c2);
        int i2 = mesh.next();
        putVertex(mesh, x3, y3, z3, c3);
        int i3 = mesh.next();
        putVertex(mesh, x4, y4, z4, c4);
        int i4 = mesh.next();
        mesh.quad(i1, i2, i3, i4);
    }

    static void quad(MeshBuilder mesh,
                     double x1, double y1, double z1,
                     double x2, double y2, double z2,
                     double x3, double y3, double z3,
                     double x4, double y4, double z4,
                     int r, int g, int b, int a) {
        mesh.ensureQuadCapacity();
        int i1 = mesh.vec3(x1, y1, z1).color(r, g, b, a).next();
        int i2 = mesh.vec3(x2, y2, z2).color(r, g, b, a).next();
        int i3 = mesh.vec3(x3, y3, z3).color(r, g, b, a).next();
        int i4 = mesh.vec3(x4, y4, z4).color(r, g, b, a).next();
        mesh.quad(i1, i2, i3, i4);
    }

    static void line(MeshBuilder mesh,
                     double x1, double y1, double z1, int c1,
                     double x2, double y2, double z2, int c2) {
        mesh.ensureLineCapacity();
        putVertex(mesh, x1, y1, z1, c1);
        int i1 = mesh.next();
        putVertex(mesh, x2, y2, z2, c2);
        int i2 = mesh.next();
        mesh.line(i1, i2);
    }

    static void line(MeshBuilder mesh,
                     double x1, double y1, double z1,
                     double x2, double y2, double z2,
                     int r, int g, int b, int a) {
        mesh.ensureLineCapacity();
        int i1 = mesh.vec3(x1, y1, z1).color(r, g, b, a).next();
        int i2 = mesh.vec3(x2, y2, z2).color(r, g, b, a).next();
        mesh.line(i1, i2);
    }

    static void putVertex(MeshBuilder mesh, double x, double y, double z, int argb) {
        mesh.vec3(x, y, z).color(
                (argb >>> 16) & 0xFF,
                (argb >>> 8) & 0xFF,
                argb & 0xFF,
                (argb >>> 24) & 0xFF
        );
    }
}
