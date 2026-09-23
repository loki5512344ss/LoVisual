/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.combat.targetesp;

import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspRender.*;
import dev.loki.lovisual.features.module.modules.combat.prediction.TargetESP;

/**
 * Crystal orbit geometry for the TargetESP "crystals" mode.
 */
public final class TargetEspCrystals {
    private TargetEspCrystals() {
    }

    private static final float CRYSTAL_BASE_SIZE = 0.05f;
    private static final int CRYSTAL_SIDES = 8;

    public static final class CrystalInstance {
        private final Vec3 position;
        private final Vec3 rotation;
        private final float rotationSpeed;

        public CrystalInstance(Vec3 position, Vec3 rotation) {
            this.position = position;
            this.rotation = rotation;
            this.rotationSpeed = 0.5f + (float) (Math.random() * 1.5f);
        }

        private float selfRotationDeg() {
            return (GameClock.millis() % 36000L) / 100.0f * rotationSpeed;
        }

        private float pulsation() {
            return 1.0f + (float) (Math.sin(GameClock.millis() / 500.0) * 0.1f);
        }

        public void append(MeshBuilder additiveMesh,
                           MeshBuilder fillMesh,
                           MeshBuilder lineMesh,
                           MeshBuilder bloomMesh,
                           Vec3 targetPos,
                           float orbitYawRad,
                           int baseColor) {
            appendCrystalGeometry(additiveMesh, targetPos, this, orbitYawRad, 1.0f, applyOpacity(baseColor, 0.2f), true);
            appendCrystalGeometry(fillMesh, targetPos, this, orbitYawRad, 1.0f, applyOpacity(baseColor, 0.3f), true);
            appendCrystalGeometry(lineMesh, targetPos, this, orbitYawRad, 1.0f, applyOpacity(baseColor, 0.8f), false);
            appendCrystalGeometry(additiveMesh, targetPos, this, orbitYawRad, 1.2f, applyOpacity(baseColor, 0.3f), true);
            if (bloomMesh != null) {
                appendBloomSphere(bloomMesh, targetPos, this, orbitYawRad, baseColor);
            }
        }
    }

    private static void appendCrystalGeometry(MeshBuilder mesh,
                                              Vec3 targetPos,
                                              CrystalInstance crystal,
                                              float orbitYawRad,
                                              float scale,
                                              int argb,
                                              boolean filled) {
        if (mesh == null || crystal == null || targetPos == null) {
            return;
        }

        float s = CRYSTAL_BASE_SIZE * scale;
        float hPrism = s;
        float hPyramid = s * 1.5f;

        Vector3f[] topVertices = new Vector3f[CRYSTAL_SIDES];
        Vector3f[] bottomVertices = new Vector3f[CRYSTAL_SIDES];
        for (int i = 0; i < CRYSTAL_SIDES; i++) {
            float angle = (float) (2.0 * Math.PI * i / CRYSTAL_SIDES);
            float x = (float) (s * Math.cos(angle));
            float z = (float) (s * Math.sin(angle));
            topVertices[i] = new Vector3f(x, hPrism * 0.5f, z);
            bottomVertices[i] = new Vector3f(x, -hPrism * 0.5f, z);
        }

        Vector3f topPeak = new Vector3f(0.0f, hPrism * 0.5f + hPyramid, 0.0f);
        Vector3f bottomPeak = new Vector3f(0.0f, -hPrism * 0.5f - hPyramid, 0.0f);

        if (filled) {
            for (int i = 0; i < CRYSTAL_SIDES; i++) {
                Vector3f v1 = transformCrystalVertex(bottomVertices[i], crystal, orbitYawRad, targetPos);
                Vector3f v2 = transformCrystalVertex(bottomVertices[(i + 1) % CRYSTAL_SIDES], crystal, orbitYawRad, targetPos);
                Vector3f v3 = transformCrystalVertex(topVertices[(i + 1) % CRYSTAL_SIDES], crystal, orbitYawRad, targetPos);
                Vector3f v4 = transformCrystalVertex(topVertices[i], crystal, orbitYawRad, targetPos);
                addTriangle(mesh, v1, v2, v3, argb);
                addTriangle(mesh, v1, v3, v4, argb);
            }
            for (int i = 0; i < CRYSTAL_SIDES; i++) {
                Vector3f v1 = transformCrystalVertex(topPeak, crystal, orbitYawRad, targetPos);
                Vector3f v2 = transformCrystalVertex(topVertices[i], crystal, orbitYawRad, targetPos);
                Vector3f v3 = transformCrystalVertex(topVertices[(i + 1) % CRYSTAL_SIDES], crystal, orbitYawRad, targetPos);
                addTriangle(mesh, v1, v2, v3, argb);
            }
            for (int i = 0; i < CRYSTAL_SIDES; i++) {
                Vector3f v1 = transformCrystalVertex(bottomPeak, crystal, orbitYawRad, targetPos);
                Vector3f v2 = transformCrystalVertex(bottomVertices[(i + 1) % CRYSTAL_SIDES], crystal, orbitYawRad, targetPos);
                Vector3f v3 = transformCrystalVertex(bottomVertices[i], crystal, orbitYawRad, targetPos);
                addTriangle(mesh, v1, v2, v3, argb);
            }
            return;
        }

        for (int i = 0; i < CRYSTAL_SIDES; i++) {
            Vector3f bottomA = transformCrystalVertex(bottomVertices[i], crystal, orbitYawRad, targetPos);
            Vector3f bottomB = transformCrystalVertex(bottomVertices[(i + 1) % CRYSTAL_SIDES], crystal, orbitYawRad, targetPos);
            Vector3f topA = transformCrystalVertex(topVertices[i], crystal, orbitYawRad, targetPos);
            Vector3f topB = transformCrystalVertex(topVertices[(i + 1) % CRYSTAL_SIDES], crystal, orbitYawRad, targetPos);
            addLine(mesh, bottomA, bottomB, argb);
            addLine(mesh, topA, topB, argb);
            addLine(mesh, bottomA, topA, argb);
            addLine(mesh, transformCrystalVertex(topPeak, crystal, orbitYawRad, targetPos), topA, argb);
            addLine(mesh, transformCrystalVertex(bottomPeak, crystal, orbitYawRad, targetPos), bottomA, argb);
        }
    }

    private static Vector3f transformCrystalVertex(Vector3f vertex,
                                                   CrystalInstance crystal,
                                                   float orbitYawRad,
                                                   Vec3 targetPos) {
        Vector3f transformed = new Vector3f(vertex).mul(crystal.pulsation());
        transformed.rotateX((float) Math.toRadians(crystal.rotation.x));
        transformed.rotateY((float) Math.toRadians(crystal.rotation.y + crystal.selfRotationDeg()));
        transformed.rotateZ((float) Math.toRadians(crystal.rotation.z));
        transformed.add((float) crystal.position.x, (float) crystal.position.y, (float) crystal.position.z);
        transformed.rotateY(orbitYawRad);
        transformed.add((float) targetPos.x, (float) targetPos.y, (float) targetPos.z);
        return transformed;
    }

    private static void appendBloomSphere(MeshBuilder mesh,
                                          Vec3 targetPos,
                                          CrystalInstance crystal,
                                          float orbitYawRad,
                                          int argb) {
        if (mesh == null || crystal == null || targetPos == null) {
            return;
        }

        Vector3f center = new Vector3f((float) crystal.position.x, (float) crystal.position.y, (float) crystal.position.z);
        center.rotateY(orbitYawRad);
        center.add((float) targetPos.x, (float) targetPos.y, (float) targetPos.z);

        float size = CRYSTAL_BASE_SIZE * 13.0f;
        Quaternionf camRot = RenderState.cameraRotation;
        float yaw = orbitYawRad;
        int bloomColor = applyOpacity(argb, (0.4f * 25.0f) / 255.0f);

        for (int i = 0; i < 6; i++) {
            float angle = (360.0f / 6.0f) * i;
            Quaternionf rotation = new Quaternionf()
                    .rotateY((float) Math.toRadians(angle))
                    .rotateY(yaw)
                    .mul(camRot);
            addTexturedBillboard(mesh, center.x, center.y, center.z, size, rotation, bloomColor);
        }

        for (int i = 0; i < 6; i++) {
            float angle = (360.0f / 6.0f) * i;
            Quaternionf rotation = new Quaternionf()
                    .rotateX((float) Math.toRadians(90.0f))
                    .rotateY((float) Math.toRadians(angle))
                    .rotateY(yaw)
                    .mul(camRot);
            addTexturedBillboard(mesh, center.x, center.y, center.z, size, rotation, bloomColor);
        }
    }
}
