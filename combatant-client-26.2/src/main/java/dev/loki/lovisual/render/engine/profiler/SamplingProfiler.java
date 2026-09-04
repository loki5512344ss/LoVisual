/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.profiler;

public enum SamplingProfiler {
    ;
    private static final boolean DEV = DevProfilerBridge.available("SamplingProfiler");

    public static void onClientFrame(Target source) {
        if (!DEV) return;
        Object devTarget = devTarget(source);
        if (devTarget == null) return;
        DevProfilerBridge.invoke("SamplingProfiler", "onClientFrame", new Class<?>[]{devTarget.getClass()}, devTarget);
    }

    public static boolean isActive() {
        if (!DEV) return false;
        return DevProfilerBridge.bool("SamplingProfiler", "isActive", false, new Class<?>[0]);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object devTarget(Target source) {
        Class<?> type = DevProfilerBridge.type("SamplingProfiler");
        if (type == null) return null;
        for (Class<?> nested : type.getDeclaredClasses()) {
            if (!"Target".equals(nested.getSimpleName()) || !nested.isEnum()) continue;
            return Enum.valueOf((Class<? extends Enum>) nested.asSubclass(Enum.class), source.name());
        }
        return null;
    }

    public enum Target {
        ALL,
        UI,
        WORLD
    }
}
