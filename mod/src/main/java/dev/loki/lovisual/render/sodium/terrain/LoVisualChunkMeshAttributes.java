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
