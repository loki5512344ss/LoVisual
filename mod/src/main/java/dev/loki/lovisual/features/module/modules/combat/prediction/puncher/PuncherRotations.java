package dev.loki.lovisual.features.module.modules.combat.prediction;

import dev.loki.lovisual.util.aiming.data.Rotation;
import dev.loki.lovisual.util.aiming.data.RotationWithVector;
import dev.loki.lovisual.util.aiming.features.MovementCorrection;
import dev.loki.lovisual.util.aiming.features.processors.RotationProcessor;
import dev.loki.lovisual.util.aiming.features.processors.anglesmooth.AngleSmooth;
import dev.loki.lovisual.util.aiming.features.processors.anglesmooth.impl.LinearAngleSmooth;
import dev.loki.lovisual.util.aiming.features.processors.anglesmooth.impl.curves.SigmoidAngleSmooth;
import dev.loki.lovisual.util.aiming.features.processors.anglesmooth.impl.curves.SmartAngleSmooth;
import dev.loki.lovisual.util.aiming.features.processors.anglesmooth.impl.curves.SpookyAngleSmooth;
import dev.loki.lovisual.util.aiming.preference.LeastDifferencePreference;
import dev.loki.lovisual.util.aiming.preference.RotationPreference;
import dev.loki.lovisual.util.aiming.raytrace.RotationRaytrace;
import dev.loki.lovisual.util.aiming.rotation.RotationManager;
import dev.loki.lovisual.util.aiming.rotation.RotationTarget;
import dev.loki.lovisual.util.raycast.RaycastUtil;
import dev.loki.lovisual.util.target.TargetingUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

final class PuncherRotations {
    private final ProjectilePuncher module;
    private final PuncherTargets targets;
    private final LinearAngleSmooth linearSmooth;
    private final SigmoidAngleSmooth sigmoidSmooth;
    private final SmartAngleSmooth smartSmooth;
    private final SpookyAngleSmooth spookySmooth = new SpookyAngleSmooth();

    PuncherRotations(ProjectilePuncher module, PuncherTargets targets) {
        this.module = module;
        this.targets = targets;
        this.linearSmooth = new LinearAngleSmooth(module.linearHorMin, module.linearHorMax, module.linearVertMin, module.linearVertMax);
        this.sigmoidSmooth = new SigmoidAngleSmooth(module.sigmoidHorMin, module.sigmoidHorMax, module.sigmoidVertMin, module.sigmoidVertMax, module.sigmoidSteepness, module.sigmoidMidpoint);
        this.smartSmooth = new SmartAngleSmooth(module.smartYawMin, module.smartYawMax, module.smartPitchMin, module.smartPitchMax, module.smartSnapThreshold, module.smartJitterYaw, module.smartJitterPitch, module.smartDecelerateEnabled, module.smartDecelerateAngle, module.smartDecelerateMinFactor);
    }

    RotationWithVector findRotation(Entity target, float range, float wallsRange) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        Vec3 eyes = mc.player.getEyePosition();
        AABB targetBox = targets.predictBox(target);
        RotationPreference pref = module.lastAimEntityId == target.getId() && module.lastAimPoint != null ? LeastDifferencePreference.leastDifferenceToLastPoint(eyes, module.lastAimPoint) : LeastDifferencePreference.LEAST_DISTANCE_TO_CURRENT_ROTATION;
        RotationWithVector rot = RotationRaytrace.raytraceBox(eyes, targetBox, range, wallsRange, RotationRaytrace.OUTLINE_VISIBILITY, pref, true);
        if (rot == null && module.rotationThroughWalls.get()) rot = RotationRaytrace.raytraceBox(eyes, targetBox, range, range, RotationRaytrace.OUTLINE_VISIBILITY, pref, true);
        if (rot != null) {
            rot = stabilizeRotationResult(target, eyes, targetBox, rot);
            module.lastAimPoint = rot.vec();
            module.lastAimEntityId = target.getId();
        }
        return rot;
    }

    RotationWithVector stabilizeRotationResult(Entity target, Vec3 eyes, AABB box, RotationWithVector rotation) {
        if (target == null || eyes == null || box == null || rotation == null) return rotation;
        Vec3 stablePoint = rotation.vec();
        if (module.lastAimEntityId == target.getId() && module.lastAimPoint != null) {
            Vec3 previousPoint = clampToBox(module.lastAimPoint, box);
            double distance = previousPoint.distanceTo(stablePoint);
            double blend = distance > 0.50 ? 0.55 : distance > 0.20 ? 0.35 : 0.20;
            stablePoint = new Vec3(Mth.lerp(blend, previousPoint.x, stablePoint.x), Mth.lerp(blend, previousPoint.y, stablePoint.y), Mth.lerp(blend, previousPoint.z, stablePoint.z));
            stablePoint = clampToBox(stablePoint, box);
        }
        Rotation stableRotation = Rotation.lookingAt(stablePoint, eyes).normalize();
        return new RotationWithVector(stableRotation, stablePoint);
    }

    Vec3 clampToBox(Vec3 point, AABB box) {
        return new Vec3(Mth.clamp(point.x, box.minX, box.maxX), Mth.clamp(point.y, box.minY, box.maxY), Mth.clamp(point.z, box.minZ, box.maxZ));
    }

    boolean passesRaycast(Entity target, float yaw, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        ProjectilePuncher.RaycastMode mode = module.raycast.get();
        if (mode == ProjectilePuncher.RaycastMode.NONE) return true;
        double maxRange = module.range.get();
        if (mode == ProjectilePuncher.RaycastMode.THROUGH_WALLS) return RaycastUtil.isLookingAtEntity(mc.player, target, yaw, pitch, maxRange, module.wallRange.get()) != null;
        return RaycastUtil.isLookingAtEntity(mc.player, target, yaw, pitch, maxRange, 0.0) != null;
    }

    boolean isInRange(Entity target) {
        Minecraft mc = Minecraft.getInstance();
        return TargetingUtil.distanceToEntityBoxSq(mc.player.getEyePosition(), target) <= module.range.get() * module.range.get();
    }

    Rotation resolveAttackRotation(Entity target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        ProjectilePuncher.RotationTiming timing = module.rotationTiming.get();
        if (timing == ProjectilePuncher.RotationTiming.ON_TICK) {
            Rotation rotation = module.lastRotationTick == mc.player.tickCount ? module.lastRotation : null;
            if (rotation == null) {
                RotationWithVector rot = findRotation(target, module.range.get().floatValue(), module.wallRange.get().floatValue());
                rotation = rot != null ? rot.rotation() : null;
                if (rotation == null) return null;
            }
            return rotation.normalize();
        }
        Rotation current = RotationManager.INSTANCE.getCurrentRotation();
        Rotation fallback = current != null ? current : new Rotation(mc.player.getYRot(), mc.player.getXRot(), true);
        return fallback.normalize();
    }

    RotationTarget buildRotationTarget(Rotation rotation, Entity target) {
        List<RotationProcessor> processors = new ArrayList<>();
        AngleSmooth smooth = selectAngleSmooth();
        if (smooth != null) processors.add(smooth);
        return new RotationTarget(rotation, target, processors, module.rotationResetTicks.get(), module.rotationResetThreshold.get(), !module.ignoreOpenInventory.get(), module.movementCorrection.get(), null);
    }

    int calculateTicks(Rotation rotation) {
        AngleSmooth smooth = selectAngleSmooth();
        if (smooth == null) return 0;
        Rotation base = RotationManager.INSTANCE.getServerRotation();
        if (base == null) return 0;
        return smooth.calculateTicks(base, rotation);
    }

    AngleSmooth selectAngleSmooth() {
        return switch (module.rotationMode.get()) {
            case LINEAR -> linearSmooth;
            case SIGMOID -> sigmoidSmooth;
            case SMART -> smartSmooth;
            case SPOOKY -> spookySmooth;
            case NONE -> null;
        };
    }
}
