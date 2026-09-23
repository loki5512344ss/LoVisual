/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.helpers.particle;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Плоский квад, ориентированный по yaw/pitch/spin вокруг своей точки — общая
 * геометрия для "пузырь"-эффектов (HitEffect.BUBBLES, HitBubbles), вынесена
 * из HitEffect чтобы не дублировать между модулями.
 */
public enum OrientedQuadRenderer {
    ;

    public static void addOrientedQuad(MeshBuilder mesh, Vec3 center, float yaw, float pitch,
                                       float spinDeg, float scale, int c1, int c2, int c3, int c4) {
        Quaternionf rot = new Quaternionf()
                .rotateY((float) Math.toRadians(yaw))
                .rotateX((float) Math.toRadians(pitch));

        Vector3f forward = new Vector3f(0, 0, 1).rotate(rot);
        Vector3f right = new Vector3f(1, 0, 0).rotate(rot);
        Vector3f up = new Vector3f(0, 1, 0).rotate(rot);

        Quaternionf spin = new Quaternionf().fromAxisAngleRad(forward, (float) Math.toRadians(spinDeg));
        right.rotate(spin);
        up.rotate(spin);

        right.mul(scale / 2f);
        up.mul(scale / 2f);

        double p1x = center.x - right.x() - up.x();
        double p1y = center.y - right.y() - up.y();
        double p1z = center.z - right.z() - up.z();
        double p2x = center.x + right.x() - up.x();
        double p2y = center.y + right.y() - up.y();
        double p2z = center.z + right.z() - up.z();
        double p3x = center.x + right.x() + up.x();
        double p3y = center.y + right.y() + up.y();
        double p3z = center.z + right.z() + up.z();
        double p4x = center.x - right.x() + up.x();
        double p4y = center.y - right.y() + up.y();
        double p4z = center.z - right.z() + up.z();

        mesh.ensureQuadCapacity();
        int i1 = mesh.vec3(p1x, p1y, p1z).vec2(0, 1).color(new RenderColor(c1)).next();
        int i2 = mesh.vec3(p2x, p2y, p2z).vec2(1, 1).color(new RenderColor(c2)).next();
        int i3 = mesh.vec3(p3x, p3y, p3z).vec2(1, 0).color(new RenderColor(c3)).next();
        int i4 = mesh.vec3(p4x, p4y, p4z).vec2(0, 0).color(new RenderColor(c4)).next();
        mesh.quad(i1, i2, i3, i4);
    }
}
