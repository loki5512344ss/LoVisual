package dev.loki.lovisual.render.sodium.terrain.vertex;
import dev.loki.lovisual.render.sodium.terrain.LoVisualChunkMeshAttributes;


import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;

/**
 * Sodium 26.2 terrain vertex format extension.
 * <p>
 * Sodium now feeds terrain through Mojang RenderPipeline, so the old
 * GlVertexFormat/ChunkShaderBindingPoints path is gone. Attribute names must
 * match the GLSL input names exactly for Vulkan SPIR-V reflection.
 */
public final class LoVisualModelVertexType implements ChunkVertexType {
    public static final int STRIDE = 24;
    public static final int SURFACE_FLAGS_OFFSET = 20;

    public static final VertexFormat VERTEX_FORMAT = VertexFormat.builder(0)
            .addAttribute("a_Position", GpuFormat.RG32_UINT)
            .addAttribute("a_Color", GpuFormat.RGBA8_UNORM)
            .addAttribute("a_TexCoord", GpuFormat.RG16_UINT)
            .addAttribute("a_LightAndData", GpuFormat.RGBA8_UINT)
            .addAttribute(LoVisualChunkMeshAttributes.SURFACE_FLAGS, GpuFormat.R32_UINT)
            .build();

    @Override
    public VertexFormat getVertexFormat() {
        return VERTEX_FORMAT;
    }

    @Override
    public ChunkVertexEncoder getEncoder() {
        return new LoVisualTerrainVertex(STRIDE, SURFACE_FLAGS_OFFSET);
    }
}
