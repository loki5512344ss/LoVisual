package dev.loki.lovisual.util.aiming.rotation.internal;

import dev.loki.lovisual.util.aiming.RequestHandler;
import dev.loki.lovisual.util.aiming.data.Rotation;
import dev.loki.lovisual.util.aiming.rotation.RotationTarget;
import dev.loki.lovisual.util.aiming.rotation.RotationUtil;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * Per-tick rotation state machine: resolves the active target, advances the
 * current rotation and drives the smooth return back to the player view.
 */
public final class RotationStepper {

    private RotationStepper() {
    }

    public static void step(RotationState state, LocalPlayer player, Rotation serverRotation) {
        Rotation playerRotation = new Rotation(player.getYRot(), player.getXRot(), true);
        RequestHandler.Request<RotationTarget> activeRequest = state.handler.getActiveRequest();
        RotationTarget activeRotationTarget = activeRequest != null ? activeRequest.value() : state.previousTarget;
        Object activeProvider = activeRequest != null ? activeRequest.provider() : state.previousProvider;

        if (activeRotationTarget == null) {
            if (state.current == null || !state.smoothReturnActive) {
                return;
            }

            float diffToPlayer = computeRotationDifference(state.current, playerRotation);
            if (diffToPlayer <= 0.5f) {
                state.finishSmoothReturn();
                return;
            }

            float speed = 0.25f + 0.4f * Math.min(1.0f, diffToPlayer / 30.0f);
            float yawDiff = RotationUtil.angleDifference(playerRotation.yaw(), state.current.yaw());
            float newYaw = state.current.yaw() + yawDiff * speed;
            float newPitch = Mth.lerp(speed, state.current.pitch(), playerRotation.pitch());
            state.setCurrent(new Rotation(newYaw, newPitch, false).normalize(), state.currentProvider);
            return;
        }

        if (isRotatingAllowed(activeRotationTarget)) {
            Rotation fromRotation = resolveBaseRotation(state, playerRotation, serverRotation);
            if (activeRequest == null) {
                if (state.current == null) {
                    state.finishSmoothReturn();
                    return;
                }
                if (!state.smoothReturnActive) {
                    state.smoothReturnActive = true;
                    state.smoothReturnTicks = 0;
                }

                Rotation resetRotation = activeRotationTarget.towards(fromRotation, true).normalize();
                state.setCurrent(resetRotation, activeProvider);
                state.previousTarget = activeRotationTarget;
                state.previousProvider = activeProvider;

                state.smoothReturnTicks++;
                float diffToPlayer = computeRotationDifference(resetRotation, playerRotation);
                int maxTicks = Math.max(1, activeRotationTarget.ticksUntilReset);
                float threshold = Math.max(0.05f, activeRotationTarget.resetThreshold);
                if (diffToPlayer <= threshold || state.smoothReturnTicks >= maxTicks) {
                    state.finishSmoothReturn();
                    state.handler.tick();
                    return;
                }
            } else {
                Rotation rotation = activeRotationTarget.towards(fromRotation, false).normalize();
                state.setCurrent(rotation, activeProvider);
                state.previousTarget = activeRotationTarget;
                state.previousProvider = activeProvider;
                state.smoothReturnActive = false;
                state.smoothReturnTicks = 0;

                if (activeRotationTarget.whenReached != null) {
                    activeRotationTarget.whenReached.invoke();
                }
            }
        }

        state.handler.tick();
    }

    private static float computeRotationDifference(Rotation a, Rotation b) {
        if (a == null || b == null) {
            return Float.MAX_VALUE;
        }
        float yawDiff = Math.abs(RotationUtil.angleDifference(a.yaw(), b.yaw()));
        float pitchDiff = Math.abs(a.pitch() - b.pitch());
        return (float) Math.hypot(yawDiff, pitchDiff);
    }

    private static boolean isRotatingAllowed(RotationTarget target) {
        if (!target.considerInventory) return true;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return true;
        return !(ClientScreen.current() instanceof AbstractContainerScreen<?>);
    }

    private static Rotation resolveBaseRotation(RotationState state, Rotation playerRotation, Rotation serverRotation) {
        if (state.current != null) {
            return state.current;
        }
        if (serverRotation != null && serverRotation != Rotation.ZERO) {
            return serverRotation;
        }
        return playerRotation;
    }
}
