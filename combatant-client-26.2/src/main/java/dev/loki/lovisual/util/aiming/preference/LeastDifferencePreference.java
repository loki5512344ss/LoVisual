/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.aiming.preference;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.util.aiming.RotationManager;
import dev.loki.lovisual.util.aiming.RotationUtil;
import dev.loki.lovisual.util.aiming.data.Rotation;

import java.util.Objects;
import java.util.function.Supplier;

public final class LeastDifferencePreference implements RotationPreference {

    public static final LeastDifferencePreference LEAST_DISTANCE_TO_CURRENT_ROTATION =
            new LeastDifferencePreference(LeastDifferencePreference::currentRotation);
    private final Supplier<Rotation> baseSupplier;
    private final Vec3 basePoint;

    public LeastDifferencePreference(Rotation baseRotation) {
        this(() -> baseRotation, null);
    }

    private LeastDifferencePreference(Supplier<Rotation> supplier) {
        this(supplier, null);
    }

    private LeastDifferencePreference(Supplier<Rotation> supplier, Vec3 basePoint) {
        this.baseSupplier = Objects.requireNonNull(supplier, "supplier");
        this.basePoint = basePoint;
    }

    public static RotationPreference leastDifferenceToLastPoint(Vec3 eyes, Vec3 lastPoint) {
        if (eyes == null || lastPoint == null) {
            return LEAST_DISTANCE_TO_CURRENT_ROTATION;
        }
        return new LeastDifferencePreference(() -> Rotation.lookingAt(lastPoint, eyes), lastPoint);
    }

    private static Rotation currentRotation() {
        Rotation current = RotationManager.INSTANCE.getCurrentRotation();
        if (current != null) {
            return current;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            return new Rotation(mc.player.getYRot(), mc.player.getXRot(), false).normalize();
        }
        return Rotation.ZERO;
    }

    @Override
    public int compare(Rotation a, Rotation b) {
        Rotation base = baseSupplier.get();
        return Float.compare(base.angleTo(a), base.angleTo(b));
    }

    @Override
    public Vec3 getPreferredSpotOnBox(AABB box, Vec3 eyes, double range) {
        if (basePoint != null) {
            return basePoint;
        }

        Rotation base = baseSupplier.get();
        Vec3 dir = RotationUtil.getRotationVector(base.pitch(), base.yaw()).normalize();
        return eyes.add(dir.scale(range));
    }
}
