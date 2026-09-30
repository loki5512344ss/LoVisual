package dev.loki.lovisual.util.aiming.rotation.internal;

import dev.loki.lovisual.util.aiming.RequestHandler;
import dev.loki.lovisual.util.aiming.data.Rotation;
import dev.loki.lovisual.util.aiming.rotation.RotationTarget;

/**
 * Mutable rotation state shared by {@link RotationStepper} and the public
 * {@code RotationManager} facade. Fields are package-private on purpose so the
 * stepper can drive them in place, mirroring the original single-class logic.
 */
public final class RotationState {

    final RequestHandler<RotationTarget> handler = new RequestHandler<>();

    RotationTarget previousTarget;
    Object previousProvider;
    Rotation current;
    Rotation previous;
    Object currentProvider;
    boolean smoothReturnActive;
    int smoothReturnTicks;

    public RotationTarget activeTarget() {
        RotationTarget active = handler.getActiveRequestValue();
        return active != null ? active : previousTarget;
    }

    public RotationTarget previousTarget() {
        return previousTarget;
    }

    public Rotation current() {
        return current;
    }

    public Rotation previous() {
        return previous;
    }

    public void request(RotationTarget plan, int priority, Object provider) {
        if (plan == null) return;

        handler.request(new RequestHandler.Request<>(plan.ticksUntilReset, priority, provider, plan));
        smoothReturnActive = false;
        smoothReturnTicks = 0;
    }

    void setCurrent(Rotation rotation, Object provider) {
        if (rotation == null) {
            if (current != null) {
                previous = current;
            }
            current = null;
            currentProvider = null;
            return;
        }

        previous = current;
        current = rotation;
        currentProvider = provider;
    }

    void finishSmoothReturn() {
        setCurrent(null, currentProvider);
        previous = null;
        previousTarget = null;
        previousProvider = null;
        smoothReturnActive = false;
        smoothReturnTicks = 0;
    }

    public void clearAll() {
        handler.clear();
        previousTarget = null;
        previousProvider = null;
        current = null;
        currentProvider = null;
        previous = null;
        smoothReturnActive = false;
        smoothReturnTicks = 0;
    }

    public void clearProvider(Object provider) {
        if (provider == null) return;
        boolean removed = handler.clear(provider);
        boolean ownsVisualState = provider == currentProvider || provider == previousProvider;
        if (!removed && !ownsVisualState) {
            return;
        }

        if (current != null && ownsVisualState) {
            smoothReturnActive = true;
            smoothReturnTicks = 0;
            return;
        }

        if (ownsVisualState) {
            previousTarget = null;
            previousProvider = null;
            previous = null;
        }
        smoothReturnActive = false;
        smoothReturnTicks = 0;
    }

    public void releaseProvider(Object provider, boolean smoothReturn) {
        if (provider == null) return;
        boolean removed = handler.clear(provider);
        boolean ownsVisualState = provider == currentProvider || provider == previousProvider;
        if (!removed && !ownsVisualState) {
            return;
        }
        if (!smoothReturn) {
            if (ownsVisualState) {
                setCurrent(null, currentProvider);
                previousTarget = null;
                previousProvider = null;
                previous = null;
            }
            smoothReturnActive = false;
            smoothReturnTicks = 0;
            return;
        }
        smoothReturnActive = ownsVisualState && current != null;
        smoothReturnTicks = 0;
    }
}
