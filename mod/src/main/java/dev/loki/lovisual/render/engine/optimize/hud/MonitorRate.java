package dev.loki.lovisual.render.engine.optimize.hud;

import com.mojang.blaze3d.platform.Monitor;
import dev.loki.lovisual.util.logging.DebugLog;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;

/**
 * Stage 3 step 6: nanoseconds per refresh of the monitor the window is on.
 * The GLFW query runs at most once a second — the window may have moved to
 * another monitor, so the value is re-probed on that interval.
 */
public enum MonitorRate {
    ;

    private static final long DEFAULT_INTERVAL = 1_000_000_000L / 144;

    private static volatile long intervalNs = DEFAULT_INTERVAL;
    private static long nextCheck;
    private static volatile IntSupplier probe = MonitorRate::glfwRefreshRate;

    public static long intervalNs(long now) {
        if (now >= nextCheck) {
            int hz = probe.getAsInt();
            long interval = 1_000_000_000L / Math.max(30, hz);
            if (interval != intervalNs) {
                DebugLog.info("Monitor refresh rate {} Hz", hz);
            }
            intervalNs = interval;
            nextCheck = now + 1_000_000_000L;
        }
        return intervalNs;
    }

    public static void reset() {
        intervalNs = DEFAULT_INTERVAL;
        nextCheck = 0;
        probe = MonitorRate::glfwRefreshRate;
    }

    static void setProbeForTesting(IntSupplier testProbe) {
        probe = testProbe;
    }

    private static int glfwRefreshRate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return 144;
        }
        Monitor monitor = mc.getWindow().findBestMonitor();
        return monitor != null ? monitor.currentMode().getRefreshRate() : 144;
    }
}
