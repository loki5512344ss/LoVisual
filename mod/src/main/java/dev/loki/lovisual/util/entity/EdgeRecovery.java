package dev.loki.lovisual.util.entity;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.events.impl.movement.MovementInputEvent;

/**
 * Stateful edge-recovery controller extracted from {@link EagleUtil}. Tracks a safe block center and
 * the remaining overwrite/sneak ticks, and rewrites {@link MovementInputEvent} input to rescue the
 * player from walking off an edge. The pure geometry detection it consumes still lives in
 * {@link EagleUtil}; behaviour is identical to the original nested implementation.
 */
public final class EdgeRecovery {
    private static final double CENTER_DEAD_ANGLE_COS = 0.34;
    private static final int DIAGONAL_RESCUE_TICKS = 2;

    public enum RecoveryMode {
        STOP,
        INVERT,
        CENTER
    }

    private Vec3 center;
    private int overwriteTicks;
    private int sneakTicks;

    private static Vec3 blockBottomCenter(LocalPlayer player) {
        return new Vec3(
                player.blockPosition().getX() + 0.5,
                player.blockPosition().getY(),
                player.blockPosition().getZ() + 0.5
        );
    }

    private static double horizontalDistanceSquared(Vec3 a, Vec3 b) {
        double x = a.x - b.x;
        double z = a.z - b.z;
        return x * x + z * z;
    }

    public boolean handleDiagonalRescue(LocalPlayer player, MovementInputEvent event, double edgeDistance) {
        if (player == null || event == null) {
            return false;
        }

        updateSafeCenter(player);

        EagleUtil.EdgeCheck edgeCheck = EagleUtil.checkEdge(player, event, edgeDistance);
        if (edgeCheck.diagonalRescue() && shouldOverwriteInput(player)) {
            overwriteTicks = Math.max(overwriteTicks, DIAGONAL_RESCUE_TICKS);
        }

        if (overwriteTicks <= 0) {
            return false;
        }

        overwriteTicks--;
        applyInputTowardsCenter(player, event);
        event.setJump(false);
        event.setSneak(true);
        event.setSprint(false);

        updateSafeCenter(player);
        return true;
    }

    public boolean handleOnEdge(
            LocalPlayer player,
            MovementInputEvent event,
            double edgeDistance,
            RecoveryMode mode,
            int keepTicks,
            int requestedSneakTicks,
            boolean jump
    ) {
        if (player == null || event == null) {
            return false;
        }

        updateSafeCenter(player);

        double distance = Math.min(EagleUtil.horizontalSpeed(player), Math.max(0.0, edgeDistance));
        EagleUtil.EdgeCheck edgeCheck = EagleUtil.checkEdge(player, event, distance);
        if (edgeCheck.closeToEdge() && shouldOverwriteInput(player)) {
            overwriteTicks = Math.max(overwriteTicks, Math.max(1, keepTicks));
            sneakTicks = Math.max(sneakTicks, Math.max(0, requestedSneakTicks));
        }

        boolean applied = false;
        if (overwriteTicks > 0) {
            overwriteTicks--;
            applyRecoveryInput(player, event, mode);
            event.setJump(jump);
            event.setSprint(false);
            applied = true;
        }

        if (sneakTicks > 0) {
            sneakTicks--;
            event.setSneak(true);
            applied = true;
        }

        updateSafeCenter(player);
        return applied;
    }

    public void reset() {
        center = null;
        overwriteTicks = 0;
        sneakTicks = 0;
    }

    private boolean shouldOverwriteInput(LocalPlayer player) {
        Vec3 center = this.center;
        if (center == null) {
            return true;
        }

        Vec3 horizontalVelocity = new Vec3(player.getDeltaMovement().x, 0.0, player.getDeltaMovement().z);
        if (horizontalVelocity.horizontalDistanceSqr() <= 0.02 * 0.02) {
            return true;
        }

        double currentDistance = horizontalDistanceSquared(center, player.position());
        double nextDistance = horizontalDistanceSquared(center, player.position().add(horizontalVelocity));
        return nextDistance > currentDistance;
    }

    private void applyRecoveryInput(LocalPlayer player, MovementInputEvent event, RecoveryMode mode) {
        RecoveryMode resolvedMode = mode != null ? mode : RecoveryMode.CENTER;
        if (resolvedMode == RecoveryMode.INVERT) {
            boolean forward = event.isForward();
            boolean backward = event.isBackward();
            boolean left = event.isLeft();
            boolean right = event.isRight();
            event.setForward(backward);
            event.setBackward(forward);
            event.setLeft(right);
            event.setRight(left);
            event.setJump(false);
            return;
        }

        if (resolvedMode == RecoveryMode.STOP && EagleUtil.horizontalSpeed(player) <= 0.05) {
            event.setForward(false);
            event.setBackward(false);
            event.setLeft(false);
            event.setRight(false);
            event.setJump(false);
            return;
        }

        applyInputTowardsCenter(player, event);
    }

    private void applyInputTowardsCenter(LocalPlayer player, MovementInputEvent event) {
        Vec3 center = this.center != null ? this.center : blockBottomCenter(player);
        Vec3 toCenter = center.subtract(player.position());
        Vec3 horizontal = new Vec3(toCenter.x, 0.0, toCenter.z);

        if (horizontal.lengthSqr() < 0.015 * 0.015) {
            event.setForward(false);
            event.setBackward(false);
            event.setLeft(false);
            event.setRight(false);
            return;
        }

        Vec3 direction = horizontal.normalize();
        double rad = Math.toRadians(player.getYRot());
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        double localForward = direction.z * cos - direction.x * sin;
        double localSideways = direction.x * cos + direction.z * sin;

        boolean forward = localForward > CENTER_DEAD_ANGLE_COS;
        boolean backward = localForward < -CENTER_DEAD_ANGLE_COS;
        boolean left = localSideways < -CENTER_DEAD_ANGLE_COS;
        boolean right = localSideways > CENTER_DEAD_ANGLE_COS;

        if (!forward && !backward && !left && !right) {
            if (Math.abs(localForward) >= Math.abs(localSideways)) {
                forward = localForward > 0.0;
                backward = localForward < 0.0;
            } else {
                left = localSideways < 0.0;
                right = localSideways > 0.0;
            }
        }

        event.setForward(forward);
        event.setBackward(backward);
        event.setLeft(left);
        event.setRight(right);
    }

    private void updateSafeCenter(LocalPlayer player) {
        Vec3 blockCenter = blockBottomCenter(player);
        if (!EagleUtil.wouldBeCloseToFallOff(player, blockCenter)) {
            center = blockCenter;
        }
    }
}
