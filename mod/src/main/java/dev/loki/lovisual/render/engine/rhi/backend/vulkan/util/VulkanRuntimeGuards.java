/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.rhi.backend.vulkan.util;

/**
 * Production Vulkan safety switches.
 *
 * <p>These are intentionally lightweight and do not perform diagnostics or file I/O.
 * They keep unstable Vulkan features behind explicit runtime/system-property switches.
 */
public enum VulkanRuntimeGuards {
    ;

    public static final boolean FORCE_DISABLE_VULKAN_DEBUG_LABELS = bool("lovisual.vulkan.disableDebugLabels", true);
    public static final boolean FORCE_DISABLE_VULKAN_CHECKPOINTS = bool("lovisual.vulkan.disableCheckpoints", true);

    private static boolean bool(String key, boolean fallback) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) return fallback;
        return Boolean.parseBoolean(value);
    }
}
