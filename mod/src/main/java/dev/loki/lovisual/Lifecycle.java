package dev.loki.lovisual;

import dev.loki.lovisual.util.logging.DebugLog;
import dev.loki.lovisual.util.resources.RenderResourceReadiness;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Minimal client lifecycle: INIT until explicitly activated, then ACTIVE.
 * Replaces the former Runtime/JarReplacement state machine.
 */
public enum Lifecycle {
    ;

    private enum State {
        INIT,
        ACTIVE,
        SHUTDOWN
    }

    private static final AtomicReference<State> STATE = new AtomicReference<>(State.INIT);

    private Lifecycle() {
    }

    public static void activate() {
        if (STATE.compareAndSet(State.INIT, State.ACTIVE)) {
            DebugLog.info("Lifecycle: INIT -> ACTIVE");
        }
    }

    public static void shutdown() {
        if (STATE.getAndSet(State.SHUTDOWN) != State.SHUTDOWN) {
            DebugLog.info("Lifecycle: -> SHUTDOWN");
        }
    }

    public static boolean isActive() {
        return STATE.get() == State.ACTIVE;
    }

    public static boolean canRunRender() {
        return isActive() && RenderResourceReadiness.isReady();
    }

    public static boolean canRunHud() {
        return canRunRender();
    }

    public static boolean canRunShaderBridge() {
        return canRunRender();
    }
}
