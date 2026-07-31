/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.sodium.terrain;

/**
 * Sodium 26.2 uses Mojang VertexFormat/GpuFormat attributes instead of the old
 * net.caffeinemc.mods.sodium.client.gl.attribute layer. Keep the LoVisual
 * terrain attribute name centralized because Vulkan reflection matches shader
 * inputs by attribute name.
 */
public enum LoVisualChunkMeshAttributes {
    ;
    public static final String SURFACE_FLAGS = "a_LoVisualSurfaceFlags";

}
