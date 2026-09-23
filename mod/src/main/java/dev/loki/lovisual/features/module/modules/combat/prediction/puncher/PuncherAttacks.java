package dev.loki.lovisual.features.module.modules.combat.prediction;

import dev.loki.lovisual.util.aiming.data.Rotation;
import dev.loki.lovisual.util.aiming.data.RotationWithVector;
import dev.loki.lovisual.util.aiming.rotation.RotationManager;
import dev.loki.lovisual.util.aiming.rotation.RotationTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

final class PuncherAttacks {
    private final ProjectilePuncher module;
    private final PuncherRotations rotations;

    PuncherAttacks(ProjectilePuncher module, PuncherRotations rotations) {
        this.module = module;
        this.rotations = rotations;
    }

    boolean processTarget(Entity target, float range, float wallsRange) {
        Minecraft mc = Minecraft.getInstance();
        RotationWithVector rotation = rotations.findRotation(target, range, wallsRange);
        if (rotation == null) return false;
        module.lastRotation = rotation.rotation();
        module.lastRotationTick = mc.player != null ? mc.player.tickCount : -1;
        module.rotationReleased = false;
        ProjectilePuncher.RotationTiming timing = module.rotationTiming.get();
        int rotTicks = rotations.calculateTicks(rotation.rotation());
        if (timing == ProjectilePuncher.RotationTiming.SNAP && !module.clicker.willClickAt(Math.max(1, rotTicks))) return true;
        RotationTarget plan = rotations.buildRotationTarget(rotation.rotation(), target);
        RotationManager.INSTANCE.setRotationTarget(plan, ProjectilePuncher.ROTATION_PRIORITY, module);
        return true;
    }

    void tryAttackPost() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (mc.player.tickCount == module.lastAttackExecutionAge) return;
        Entity target = module.currentTarget;
        if (target == null || target.isRemoved()) return;
        if (!rotations.isInRange(target)) return;
        Rotation rotation = rotations.resolveAttackRotation(target);
        if (rotation == null) {
            clearTarget();
            return;
        }
        if (!rotations.passesRaycast(target, rotation.yaw(), rotation.pitch())) return;
        int clickCount = module.clicker.getClicksAt(0);
        if (clickCount <= 0) return;
        module.lastAttackExecutionAge = mc.player.tickCount;
        for (int i = 0; i < clickCount; i++) {
            if (target.isRemoved()) break;
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    void clearTarget() {
        module.currentTarget = null;
        module.lastAttackExecutionAge = -1;
        clearRotationState(true);
    }

    void clearTargetImmediate() {
        module.currentTarget = null;
        module.lastAttackExecutionAge = -1;
        clearRotationState(false);
    }

    void clearRotationState(boolean smoothReturn) {
        if (smoothReturn && module.rotationReleased) return;
        RotationManager.INSTANCE.release(module, smoothReturn);
        module.lastAimPoint = null;
        module.lastAimEntityId = -1;
        module.lastRotation = null;
        module.lastRotationTick = -1;
        module.rotationReleased = true;
    }
}
