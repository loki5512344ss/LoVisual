package dev.loki.lovisual.features.module.modules.combat.prediction;

import dev.loki.lovisual.config.common.CommonSettingSchemas;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.events.impl.input.RotationUpdateEvent;
import dev.loki.lovisual.events.meta.EventHandler;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.util.aiming.data.Rotation;
import dev.loki.lovisual.util.aiming.features.MovementCorrection;
import dev.loki.lovisual.util.click.ClickScheduler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

@ModuleInfo(id = "projectilepuncher", displayName = "ProjectilePuncher", category = ModuleCategory.COMBAT)
public final class ProjectilePuncher extends Module {
    public enum RaycastMode { STRICT, THROUGH_WALLS, NONE }
    public enum RotationTiming { NORMAL, SNAP, ON_TICK }
    public enum AngleSmoothMode { LINEAR, SIGMOID, SMART, SPOOKY, NONE }
    static final int ROTATION_PRIORITY = 10;
    final NumberValue<Double> range = numCommon("projectilePuncherRange", "range", CommonSettingSchemas.COMBAT_RANGE, 3.0, 2.0, 6.0);
    final NumberValue<Double> wallRange = numCommon("projectilePuncherWallRange", "wall_range", CommonSettingSchemas.COMBAT_WALL_RANGE, 0.0, 0.0, 6.0);
    final BooleanMapValue projectiles = common(group("projectilePuncherProjectiles", "projectiles", PuncherTargets.projectileDefaults()), CommonSettingSchemas.ITEMS_PROJECTILES.commonI18nKey());
    final BooleanValue ignoreOpenInventory = boolCommon("projectilePuncherIgnoreOpenInventory", "ignore_open_inventory", CommonSettingSchemas.INTERACTION_IGNORE_OPEN_INVENTORY, true);
    final EnumValue<RaycastMode> raycast = enumCommon("projectilePuncherRaycast", "raycast", CommonSettingSchemas.COMBAT_RAYCAST, RaycastMode.STRICT, RaycastMode.values());
    final EnumValue<RotationTiming> rotationTiming = enumCommon("projectilePuncherRotationTiming", "rotation_timing", CommonSettingSchemas.ROTATION_TIMING, RotationTiming.NORMAL, RotationTiming.values());
    final EnumValue<MovementCorrection> movementCorrection = enumCommon("projectilePuncherMovementCorrection", "movement_correction", CommonSettingSchemas.ROTATION_MOVEMENT_CORRECTION, MovementCorrection.STRICT, MovementCorrection.values());
    final EnumValue<AngleSmoothMode> rotationMode = enumCommon("projectilePuncherRotationMode", "rotation_mode", CommonSettingSchemas.ROTATION_MODE, AngleSmoothMode.SMART, AngleSmoothMode.values());
    final NumberValue<Integer> rotationResetTicks = numCommon("projectilePuncherRotationResetTicks", "rotation_reset_ticks", CommonSettingSchemas.ROTATION_RESET_TICKS, 2, 1, 20);
    final NumberValue<Float> rotationResetThreshold = numCommon("projectilePuncherRotationResetThreshold", "rotation_reset_threshold", CommonSettingSchemas.ROTATION_RESET_THRESHOLD, 2.0f, 0.1f, 15.0f);
    final BooleanValue rotationThroughWalls = boolCommon("projectilePuncherRotationThroughWalls", "rotation_through_walls", CommonSettingSchemas.ROTATION_THROUGH_WALLS, false);
    final NumberValue<Integer> predictTicks = num("projectilePuncherPredictTicks", "predict_ticks", 1, 0, 5);
    final NumberValue<Integer> cpsMin = numCommon("projectilePuncherCpsMin", "cps_min", CommonSettingSchemas.COMBAT_CPS_MIN, 20, 1, 20);
    final NumberValue<Integer> cpsMax = numCommon("projectilePuncherCpsMax", "cps_max", CommonSettingSchemas.COMBAT_CPS_MAX, 20, 1, 20);
    final ClickScheduler clicker = new ClickScheduler(cpsMin.get(), cpsMax.get());
    final NumberValue<Float> linearHorMin = numCommon("projectilePuncherLinearHorizontalMin", "linear_horizontal_min", CommonSettingSchemas.ANGLESMOOTH_HORIZONTAL_MIN, 180f, 0f, 180f);
    final NumberValue<Float> linearHorMax = numCommon("projectilePuncherLinearHorizontalMax", "linear_horizontal_max", CommonSettingSchemas.ANGLESMOOTH_HORIZONTAL_MAX, 180f, 0f, 180f);
    final NumberValue<Float> linearVertMin = numCommon("projectilePuncherLinearVerticalMin", "linear_vertical_min", CommonSettingSchemas.ANGLESMOOTH_VERTICAL_MIN, 180f, 0f, 180f);
    final NumberValue<Float> linearVertMax = numCommon("projectilePuncherLinearVerticalMax", "linear_vertical_max", CommonSettingSchemas.ANGLESMOOTH_VERTICAL_MAX, 180f, 0f, 180f);
    final NumberValue<Float> sigmoidHorMin = numCommon("projectilePuncherSigmoidHorizontalMin", "sigmoid_horizontal_min", CommonSettingSchemas.ANGLESMOOTH_HORIZONTAL_MIN, 180f, 0f, 180f);
    final NumberValue<Float> sigmoidHorMax = numCommon("projectilePuncherSigmoidHorizontalMax", "sigmoid_horizontal_max", CommonSettingSchemas.ANGLESMOOTH_HORIZONTAL_MAX, 180f, 0f, 180f);
    final NumberValue<Float> sigmoidVertMin = numCommon("projectilePuncherSigmoidVerticalMin", "sigmoid_vertical_min", CommonSettingSchemas.ANGLESMOOTH_VERTICAL_MIN, 180f, 0f, 180f);
    final NumberValue<Float> sigmoidVertMax = numCommon("projectilePuncherSigmoidVerticalMax", "sigmoid_vertical_max", CommonSettingSchemas.ANGLESMOOTH_VERTICAL_MAX, 180f, 0f, 180f);
    final NumberValue<Float> sigmoidSteepness = numCommon("projectilePuncherSigmoidSteepness", "sigmoid_steepness", CommonSettingSchemas.ANGLESMOOTH_STEEPNESS, 10f, 0f, 20f);
    final NumberValue<Float> sigmoidMidpoint = numCommon("projectilePuncherSigmoidMidpoint", "sigmoid_midpoint", CommonSettingSchemas.ANGLESMOOTH_MIDPOINT, 0.3f, 0f, 1f);
    final NumberValue<Float> smartYawMin = numCommon("projectilePuncherSmartYawMin", "smart_yaw_min", CommonSettingSchemas.ANGLESMOOTH_YAW_MIN, 70f, 1f, 180f);
    final NumberValue<Float> smartYawMax = numCommon("projectilePuncherSmartYawMax", "smart_yaw_max", CommonSettingSchemas.ANGLESMOOTH_YAW_MAX, 95f, 1f, 180f);
    final NumberValue<Float> smartPitchMin = numCommon("projectilePuncherSmartPitchMin", "smart_pitch_min", CommonSettingSchemas.ANGLESMOOTH_PITCH_MIN, 45f, 1f, 180f);
    final NumberValue<Float> smartPitchMax = numCommon("projectilePuncherSmartPitchMax", "smart_pitch_max", CommonSettingSchemas.ANGLESMOOTH_PITCH_MAX, 70f, 1f, 180f);
    final NumberValue<Float> smartSnapThreshold = numCommon("projectilePuncherSmartSnapThreshold", "smart_snap_threshold", CommonSettingSchemas.ANGLESMOOTH_SNAP_THRESHOLD, 1.2f, 0.05f, 10.0f);
    final NumberValue<Float> smartJitterYaw = numCommon("projectilePuncherSmartJitterYaw", "smart_jitter_yaw", CommonSettingSchemas.ANGLESMOOTH_JITTER_YAW, 0.8f, 0.0f, 6.0f);
    final NumberValue<Float> smartJitterPitch = numCommon("projectilePuncherSmartJitterPitch", "smart_jitter_pitch", CommonSettingSchemas.ANGLESMOOTH_JITTER_PITCH, 0.4f, 0.0f, 4.0f);
    final BooleanValue smartDecelerateEnabled = boolCommon("projectilePuncherSmartDecelerateEnabled", "smart_decelerate_enabled", CommonSettingSchemas.ANGLESMOOTH_DECELERATE_ENABLED, true);
    final NumberValue<Float> smartDecelerateAngle = numCommon("projectilePuncherSmartDecelerateAngle", "smart_decelerate_angle", CommonSettingSchemas.ANGLESMOOTH_DECELERATE_ANGLE, 35.0f, 1.0f, 120.0f);
    final NumberValue<Float> smartDecelerateMinFactor = numCommon("projectilePuncherSmartDecelerateMinFactor", "smart_decelerate_min_factor", CommonSettingSchemas.ANGLESMOOTH_DECELERATE_MIN_FACTOR, 0.22f, 0.05f, 1.0f);
    Entity currentTarget;
    Vec3 lastAimPoint;
    int lastAimEntityId = -1;
    Rotation lastRotation;
    int lastRotationTick = -1;
    int lastAttackExecutionAge = -1;
    boolean rotationReleased = true;
    final PuncherTargets targets = new PuncherTargets(this);
    final PuncherRotations rotations = new PuncherRotations(this, targets);
    final PuncherAttacks attacks = new PuncherAttacks(this, rotations);

    @Override public void onEnable() {
        clicker.reset();
        currentTarget = null;
        lastRotation = null;
        lastRotationTick = -1;
        lastAttackExecutionAge = -1;
        rotationReleased = true;
    }

    @Override public void onDisable() { attacks.clearTargetImmediate(); }

    @Override public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) attacks.clearTargetImmediate();
    }

    @EventHandler(priority = ROTATION_PRIORITY)
    public void onRotationUpdate(RotationUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled()) return;
        if (mc.player == null || mc.level == null) { attacks.clearTargetImmediate(); return; }
        if (event.getType() == RotationUpdateEvent.Type.POST) { attacks.tryAttackPost(); return; }
        clicker.setCps(cpsMin.get(), cpsMax.get());
        clicker.tick();
        List<Entity> candidates = targets.findCandidates();
        for (Entity entity : candidates) {
            if (attacks.processTarget(entity, range.get().floatValue(), wallRange.get().floatValue())) { currentTarget = entity; return; }
        }
        attacks.clearTarget();
    }
}
