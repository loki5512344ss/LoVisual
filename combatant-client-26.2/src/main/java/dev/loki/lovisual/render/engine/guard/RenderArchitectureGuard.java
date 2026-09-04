/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.guard;

import dev.loki.lovisual.render.engine.core.LoVisualRenderSystem;

/**
 * Final architecture lock for the LoVisual RHI migration.
 * <p>
 * This class is intentionally small and dependency-light: it is used from low-level legacy facades to make sure old
 * render paths cannot silently become production paths again. A path can only be enabled through an explicit system
 * property, and every use is counted in RHI stats.
 */
public enum RenderArchitectureGuard {
    ;
    public static final boolean DEBUG = Boolean.getBoolean("lovisual.render.debug");
    public static final boolean STRICT = !Boolean.getBoolean("lovisual.render.allowLegacyPaths");
    public static final boolean ALLOW_LEGACY_MESH_RENDERER = Boolean.getBoolean("lovisual.render.allowLegacyMeshRenderer");
    public static final boolean ALLOW_LEGACY_FULLSCREEN_MESH = Boolean.getBoolean("lovisual.render.allowLegacyFullscreenMesh");
    public static final boolean ALLOW_IMMEDIATE_UPLOAD = Boolean.getBoolean("lovisual.rhi.allowImmediateFallback")
            || Boolean.getBoolean("lovisual.render.allowImmediateUpload");

    public static void requireAllowed(LegacyRenderPath path, String owner) {
        boolean allowed = switch (path) {
            case IMMEDIATE_MESH_UPLOAD -> ALLOW_IMMEDIATE_UPLOAD;
            case LEGACY_FULLSCREEN_MESH -> ALLOW_LEGACY_FULLSCREEN_MESH;
            case LEGACY_MESH_RENDERER -> ALLOW_LEGACY_MESH_RENDERER;
            default -> !STRICT;
        };

        record(path);
        if (allowed) return;

        throw new IllegalStateException("Legacy render path is locked: " + path.description()
                + " owner=" + owner
                + ". Use LoVisualRHI/SodiumGlBackend path instead. "
                + "Temporary override: -D" + overrideProperty(path) + "=true");
    }

    public static void record(LegacyRenderPath path) {
        try {
            LoVisualRenderSystem.rhi().stats().legacyPath(path);
        } catch (Throwable ignored) {
            // Guard must be safe during bootstrap/static initialization.
        }
    }

    private static String overrideProperty(LegacyRenderPath path) {
        return switch (path) {
            case IMMEDIATE_MESH_UPLOAD -> "lovisual.rhi.allowImmediateFallback";
            case LEGACY_FULLSCREEN_MESH -> "lovisual.render.allowLegacyFullscreenMesh";
            case LEGACY_MESH_RENDERER -> "lovisual.render.allowLegacyMeshRenderer";
            default -> "lovisual.render.allowLegacyPaths";
        };
    }
}
