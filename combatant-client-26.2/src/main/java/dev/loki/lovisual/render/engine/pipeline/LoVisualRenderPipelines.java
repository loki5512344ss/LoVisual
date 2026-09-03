/*
 * This file is part of the LoVisual Client distribution.
 * LoVisual modifications copyright (c) 2026 pivosos2007.
 *
 * Portions of this file are based on, adapted from, or implemented
 * with reference to Meteor Client
 * (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 *
 * Licensed under the GNU General Public License v3.0.
 * See THIRD_PARTY_NOTICES.md for details.
 */

package dev.loki.lovisual.render.engine.pipeline;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import dev.loki.lovisual.mixininterface.IRenderPipeline;
import dev.loki.lovisual.render.engine.rhi.clip.ShapeClipRenderPassContract;
import dev.loki.lovisual.render.engine.rhi.pipeline.RenderPipelineRegistry;
import dev.loki.lovisual.render.engine.shader.LoVisualShaderSources;
import dev.loki.lovisual.render.engine.vertex.LoVisualVertexFormats;
import dev.loki.lovisual.util.logging.DebugLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pipeline registry for LoVisual's custom rendering engine.
 *
 * <p>Keep this class free of module-specific logic: it is the foundation for all custom 3D/2D effects.</p>
 */
public enum LoVisualRenderPipelines {
    ;
    // Shaders (assets/lovisual/shaders/*)
    public static final Identifier SHADER_POS_COLOR_VERT = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_color.vert");
    public static final Identifier SHADER_WIDE_LINE_VERT = Identifier.fromNamespaceAndPath("lovisual", "shaders/wide_line.vert");
    public static final Identifier SHADER_POS_COLOR_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_color.frag");
    public static final Identifier SHADER_POS_COLOR_FOG_VERT = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_color_fog.vert");
    public static final Identifier SHADER_POS_COLOR_FOG_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_color_fog.frag");
    public static final Identifier SHADER_POS_COLOR_RECT_PARAMS_VERT = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_color_rect_params.vert");
    public static final Identifier SHADER_POS_COLOR_RECT_PARAMS2_VERT = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_color_rect_params2.vert");
    public static final Identifier SHADER_POS_LOCAL_COLOR_RECT_PARAMS_VERT = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_local_color_rect_params.vert");
    public static final Identifier SHADER_POS_LOCAL_COLOR_RECT_PARAMS2_VERT = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_local_color_rect_params2.vert");
    public static final Identifier SHADER_POS_LOCAL_COLOR_RECT_PARAMS5_VERT = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_local_color_rect_params5.vert");
    public static final Identifier SHADER_POS_LOCAL_COLOR_RECT_PARAMS5_GENERIC_VERT = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_local_color_rect_params5_generic.vert");
    public static final Identifier SHADER_POS_TEX_COLOR_VERT = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_tex_color.vert");
    public static final Identifier SHADER_POS_TEX_COLOR_PARAMS2_VERT = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_tex_color_params2.vert");
    public static final Identifier SHADER_POS_TEX_COLOR_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_tex_color.frag");
    public static final Identifier SHADER_GUI_TEXTURE_LOOKUP_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/gui_texture_lookup.frag");
    public static final Identifier SHADER_POS_TEX_COLOR_TINT_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_tex_color_tint.frag");
    public static final Identifier SHADER_POS_TEX_COLOR_SKY_FOG_VERT = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_tex_color_sky_fog.vert");
    public static final Identifier SHADER_POS_TEX_COLOR_SKY_FOG_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_tex_color_sky_fog.frag");
    public static final Identifier SHADER_POS_TEX_COLOR_SKY_FOG_ADDITIVE_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_tex_color_sky_fog_additive.frag");
    public static final Identifier SHADER_POS_COLOR_SKYBOX_SHADER_VERT = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_color_skybox_shader.vert");
    public static final Identifier SHADER_REIMAGINED_SKYBOX_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/reimagined_skybox.frag");
    public static final Identifier SHADER_SHADER_SKY_THUNDER_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/shader_sky_thunder.frag");
    public static final Identifier SHADER_SHADER_SKY_PULSAR_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/shader_sky_pulsar.frag");
    public static final Identifier SHADER_POS_TEX_COLOR_RECT_PARAMS_VERT = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_tex_color_rect_params.vert");
    public static final Identifier SHADER_POS_TEX_LOCAL_COLOR_RECT_PARAMS_VERT = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_tex_local_color_rect_params.vert");
    public static final Identifier SHADER_POS_TEX_LOCAL_COLOR_RECT_PARAMS2_VERT = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_tex_local_color_rect_params2.vert");
    public static final Identifier SHADER_POS_TEX_COLOR_RECT_PARAMS2_VERT = Identifier.fromNamespaceAndPath("lovisual", "shaders/pos_tex_color_rect_params2.vert");
    public static final Identifier SHADER_TEXT_VERT = Identifier.fromNamespaceAndPath("lovisual", "shaders/text.vert");
    public static final Identifier SHADER_TEXT_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/text.frag");
    public static final Identifier SHADER_TEXT_MSDF_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/text_msdf.frag");
    public static final Identifier SHADER_SVG_MSDF_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/svg_msdf.frag");
    public static final Identifier SHADER_TEXT_LIQUID_GLASS_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/text_liquid_glass.frag");
    public static final Identifier SHADER_TEXT_LIQUID_GLASS_MSDF_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/text_liquid_glass_msdf.frag");
    public static final Identifier SHADER_CIRCLE_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/circle_batch.frag");
    public static final Identifier SHADER_ARC_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/arc_batch.frag");
    public static final Identifier SHADER_ORBIZ_RING_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/orbiz_ring_batch.frag");
    public static final Identifier SHADER_WORLD_DECAL_SDF_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/world_decal_sdf.frag");
    public static final Identifier SHADER_ROUNDED_RECT_STROKE_CORNERS_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/rounded_rect_stroke_corners_batch.frag");
    public static final Identifier SHADER_ROUNDED_RECT_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/rounded_rect_batch.frag");
    public static final Identifier SHADER_UI_ROUNDED_FILL_SMOKE_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/ui_rounded_fill_smoke_batch.frag");
    public static final Identifier SHADER_ROUNDED_RECT_STROKE_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/rounded_rect_stroke_batch.frag");
    public static final Identifier SHADER_ROUNDED_RECT_STROKE_ANGULAR_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/rounded_rect_stroke_angular_batch.frag");
    public static final Identifier SHADER_CHAMFERED_RECT_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/chamfered_rect_batch.frag");
    public static final Identifier SHADER_UI_SHAPE_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/ui_shape_batch.frag");
    public static final Identifier SHADER_CHAMFERED_RECT_STROKE_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/chamfered_rect_stroke_batch.frag");
    public static final Identifier SHADER_ROUNDED_RECT_CORNERS_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/rounded_rect_corners_batch.frag");
    public static final Identifier SHADER_ROUNDED_RECT_GLOW_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/rounded_rect_glow_batch.frag");
    public static final Identifier SHADER_ROUNDED_RECT_SHADOW_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/rounded_rect_shadow_batch.frag");
    public static final Identifier SHADER_ROUNDED_RECT_SOFT_SHADOW_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/rounded_rect_soft_shadow_batch.frag");
    public static final Identifier SHADER_RADIAL_GLOW_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/radial_glow_batch.frag");
    public static final Identifier SHADER_ROUNDED_RECT_TEX_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/rounded_rect_tex_batch.frag");
    public static final Identifier SHADER_ROUNDED_RECT_TEX_MASK_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/rounded_rect_tex_mask_batch.frag");
    public static final Identifier SHADER_UI_BLUR_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/ui_blur_batch.frag");
    public static final Identifier SHADER_UI_BLUR_BATCH_CORNERS_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/ui_blur_batch_corners.frag");
    public static final Identifier SHADER_UI_LIQUID_GLASS_BATCH_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/ui_liquid_glass_batch.frag");
    public static final Identifier SHADER_DAMAGE_TINT_VERT = Identifier.fromNamespaceAndPath("lovisual", "shaders/damage_tint.vert");
    public static final Identifier SHADER_DAMAGE_TINT_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/damage_tint.frag");
    public static final Identifier SHADER_POST_FX_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/post_fx.frag");
    public static final Identifier SHADER_MOTION_BLUR_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/motion_blur.frag");
    public static final Identifier SHADER_DEPTH_OF_FIELD_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/depth_of_field.frag");
    public static final Identifier SHADER_WET_SURFACE_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/wet_surface.frag");
    public static final Identifier SHADER_HEAT_FX_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/heat_fx.frag");
    public static final Identifier SHADER_ESP_GRADIENT_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/shader_esp_gradient.frag");
    public static final Identifier SHADER_ESP_SHADOW_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/shader_esp_shadow.frag");
    public static final Identifier SHADER_ESP_SMOKE_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/shader_esp_smoke.frag");
    public static final Identifier SHADER_PORTAL_RIFT_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/portal_rift.frag");
    public static final Identifier SHADER_SLEEP_OVERLAY_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/sleep_overlay.frag");
    public static final Identifier SHADER_HAND_SMOKE_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/hand_smoke.frag");
    public static final Identifier SHADER_HAND_METALLIC_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/hand_metallic.frag");
    public static final Identifier SHADER_UI_BLUR_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/ui_blur.frag");
    public static final Identifier SHADER_HAND_GLASS_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/hand_glass.frag");
    public static final Identifier SHADER_HAND_CHAMS_SOLID_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/hand_chams_solid.frag");
    public static final Identifier SHADER_HAND_CHAMS_CHROMA_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/hand_chams_chroma.frag");
    public static final Identifier SHADER_HAND_CHAMS_STRIPES_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/hand_chams_stripes.frag");
    public static final Identifier SHADER_HAND_CHAMS_GLOW_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/hand_chams_glow.frag");
    public static final Identifier SHADER_HAND_CHAMS_SNOW_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/hand_chams_snow.frag");
    public static final Identifier SHADER_HAND_CHAMS_BALATRO_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/hand_chams_balatro.frag");
    public static final Identifier SHADER_HAND_CHAMS_BLEND_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/hand_chams_blend.frag");
    public static final Identifier SHADER_SKY_SUN_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/sky_sun.frag");
    public static final Identifier SHADER_MENU_BACKGROUND_WAVES_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/menu_background_waves.frag");
    public static final Identifier SHADER_MENU_BACKGROUND_AURORA_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/menu_background_aurora.frag");
    public static final Identifier SHADER_SKY_SHADER_SKY_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/sky_shader_sky.frag");
    public static final Identifier SHADER_SKY_SHADER_WATER_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/sky_shader_water.frag");
    public static final Identifier SHADER_SKY_SHADER_CAUSTIC_FRAG = Identifier.fromNamespaceAndPath("lovisual", "shaders/sky_shader_caustic.frag");
    private static final List<RenderPipeline> PIPELINES = new ArrayList<>();
    public static final RenderPipeline GUI_TEXTURE_LOOKUP = add(new ExtendedRenderPipelineBuilder(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/gui_texture_lookup"))
            .withFragmentShader(SHADER_GUI_TEXTURE_LOOKUP_FRAG)
            .build()
    );
    // Snippets
    private static final RenderPipeline.Snippet MESH_UNIFORMS = new ExtendedRenderPipelineBuilder()
            .withUniform("MeshData", UniformType.UNIFORM_BUFFER)
            .buildSnippet();
    /**
     * No depth test; translucent; triangles.
     */
    public static final RenderPipeline WORLD_COLORED = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_colored"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_COLOR_VERT)
            .withFragmentShader(SHADER_POS_COLOR_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );

    // ======================
    // World (3D)
    // ======================
    /**
     * No depth test; translucent; lines.
     */
    public static final RenderPipeline WORLD_COLORED_LINES = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLineSmooth()
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_colored_lines"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, com.mojang.blaze3d.PrimitiveTopology.DEBUG_LINES)
            .withVertexShader(SHADER_POS_COLOR_VERT)
            .withFragmentShader(SHADER_POS_COLOR_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Screen-space wide line expansion for WORLD_COLORED_LINES. Uses triangles; never depends on glLineWidth.
     */
    public static final RenderPipeline WORLD_WIDE_COLORED_LINES = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_wide_colored_lines"))
            .withVertexFormat(LoVisualVertexFormats.POS3_COLOR_LINE, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_WIDE_LINE_VERT)
            .withFragmentShader(SHADER_POS_COLOR_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );

    /**
     * Depth test (GEQUAL); translucent; triangles.
     */
    public static final RenderPipeline WORLD_COLORED_DEPTH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_colored_depth"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_COLOR_VERT)
            .withFragmentShader(SHADER_POS_COLOR_FRAG)
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * No depth test; translucent; textured triangles.
     */
    public static final RenderPipeline WORLD_TEXTURED = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_textured"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_COLOR_VERT)
            .withFragmentShader(SHADER_POS_TEX_COLOR_FRAG)
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Depth test (GEQUAL); translucent; textured triangles.
     */
    public static final RenderPipeline WORLD_TEXTURED_DEPTH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_textured_depth"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_COLOR_VERT)
            .withFragmentShader(SHADER_POS_TEX_COLOR_FRAG)
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Depth test (GEQUAL); additive; colored triangles.
     */
    public static final RenderPipeline WORLD_COLORED_ADDITIVE_DEPTH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_colored_additive_depth"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_COLOR_VERT)
            .withFragmentShader(SHADER_POS_COLOR_FRAG)
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE))
            .withCull(false)
            .build()
    );
    /**
     * No depth test; translucent; world decal quads with SDF fill/stroke.
     */
    public static final RenderPipeline WORLD_DECAL_SDF = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_decal_sdf"))
            .withVertexFormat(LoVisualVertexFormats.POS3_TEXTURE_COLOR_PARAMS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_COLOR_PARAMS2_VERT)
            .withFragmentShader(SHADER_WORLD_DECAL_SDF_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Depth test (GEQUAL); translucent; world decal quads with SDF fill/stroke.
     */
    public static final RenderPipeline WORLD_DECAL_SDF_DEPTH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_decal_sdf_depth"))
            .withVertexFormat(LoVisualVertexFormats.POS3_TEXTURE_COLOR_PARAMS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_COLOR_PARAMS2_VERT)
            .withFragmentShader(SHADER_WORLD_DECAL_SDF_FRAG)
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * No depth test; translucent; world text quads.
     */
    public static final RenderPipeline WORLD_TEXT = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_text"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_TEXT_VERT)
            .withFragmentShader(SHADER_TEXT_FRAG)
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Depth test (GEQUAL); translucent; world text quads.
     */
    public static final RenderPipeline WORLD_TEXT_DEPTH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_text_depth"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_TEXT_VERT)
            .withFragmentShader(SHADER_TEXT_FRAG)
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * No depth test; translucent; world MSDF text quads.
     */
    public static final RenderPipeline WORLD_TEXT_MSDF = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_text_msdf"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_TEXT_VERT)
            .withFragmentShader(SHADER_TEXT_MSDF_FRAG)
            .withSampler("u_Texture")
            .withUniform("MsdfText", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Depth test (GEQUAL); translucent; world MSDF text quads.
     */
    public static final RenderPipeline WORLD_TEXT_MSDF_DEPTH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_text_msdf_depth"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_TEXT_VERT)
            .withFragmentShader(SHADER_TEXT_MSDF_FRAG)
            .withSampler("u_Texture")
            .withUniform("MsdfText", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * No depth test; additive; textured triangles.
     */
    public static final RenderPipeline WORLD_TEXTURED_ADDITIVE = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_textured_additive"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_COLOR_VERT)
            .withFragmentShader(SHADER_POS_TEX_COLOR_FRAG)
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE))
            .withCull(false)
            .build()
    );
    /**
     * No depth test; additive; textured triangles with shader-side tint.
     */
    public static final RenderPipeline WORLD_TEXTURED_TINT_ADDITIVE = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_textured_tint_additive"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_COLOR_VERT)
            .withFragmentShader(SHADER_POS_TEX_COLOR_TINT_FRAG)
            .withSampler("u_Texture")
            .withUniform("TextureTint", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE))
            .withCull(false)
            .build()
    );
    /**
     * Depth test (GEQUAL); additive; textured triangles.
     */
    public static final RenderPipeline WORLD_TEXTURED_ADDITIVE_DEPTH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_textured_additive_depth"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_COLOR_VERT)
            .withFragmentShader(SHADER_POS_TEX_COLOR_FRAG)
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE))
            .withCull(false)
            .build()
    );
    /**
     * Depth test (GEQUAL); translucent; lines.
     */
    public static final RenderPipeline WORLD_COLORED_LINES_DEPTH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLineSmooth()
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_colored_lines_depth"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, com.mojang.blaze3d.PrimitiveTopology.DEBUG_LINES)
            .withVertexShader(SHADER_POS_COLOR_VERT)
            .withFragmentShader(SHADER_POS_COLOR_FRAG)
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Screen-space wide line expansion for WORLD_COLORED_LINES_DEPTH. Uses triangles; never depends on glLineWidth.
     */
    public static final RenderPipeline WORLD_WIDE_COLORED_LINES_DEPTH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_wide_colored_lines_depth"))
            .withVertexFormat(LoVisualVertexFormats.POS3_COLOR_LINE, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_WIDE_LINE_VERT)
            .withFragmentShader(SHADER_POS_COLOR_FRAG)
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );

    /**
     * Depth test (GEQUAL); translucent; triangles; lets liquids blend over (no depth write).
     */
    public static final RenderPipeline WORLD_COLORED_LIQUID_BLEND = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_colored_liquid_blend"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_COLOR_VERT)
            .withFragmentShader(SHADER_POS_COLOR_FRAG)
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Depth test (GEQUAL); translucent; lines; lets liquids blend over (no depth write).
     */
    public static final RenderPipeline WORLD_COLORED_LINES_LIQUID_BLEND = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLineSmooth()
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_colored_lines_liquid_blend"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, com.mojang.blaze3d.PrimitiveTopology.DEBUG_LINES)
            .withVertexShader(SHADER_POS_COLOR_VERT)
            .withFragmentShader(SHADER_POS_COLOR_FRAG)
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Screen-space wide line expansion for WORLD_COLORED_LINES_LIQUID_BLEND. Uses triangles; never depends on glLineWidth.
     */
    public static final RenderPipeline WORLD_WIDE_COLORED_LINES_LIQUID_BLEND = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_wide_colored_lines_liquid_blend"))
            .withVertexFormat(LoVisualVertexFormats.POS3_COLOR_LINE, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_WIDE_LINE_VERT)
            .withFragmentShader(SHADER_POS_COLOR_FRAG)
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );

    /**
     * Depth test (GEQUAL); translucent; textured triangles; lets liquids blend over (no depth write).
     */
    public static final RenderPipeline WORLD_TEXTURED_LIQUID_BLEND = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_textured_liquid_blend"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_COLOR_VERT)
            .withFragmentShader(SHADER_POS_TEX_COLOR_FRAG)
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Depth test (GEQUAL); additive; textured triangles; lets liquids blend over (no depth write).
     */
    public static final RenderPipeline WORLD_TEXTURED_ADDITIVE_LIQUID_BLEND = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_textured_additive_liquid_blend"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_COLOR_VERT)
            .withFragmentShader(SHADER_POS_TEX_COLOR_FRAG)
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE))
            .withCull(false)
            .build()
    );
    /**
     * Depth test (GEQUAL); translucent; triangles; intended for above-liquids pass (no depth write).
     */
    public static final RenderPipeline WORLD_COLORED_LIQUID_IGNORE = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_colored_liquid_ignore"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_COLOR_VERT)
            .withFragmentShader(SHADER_POS_COLOR_FRAG)
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Depth test (GEQUAL); translucent; lines; writes depth so liquids render below (above-liquids).
     */
    public static final RenderPipeline WORLD_COLORED_LINES_LIQUID_IGNORE = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLineSmooth()
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_colored_lines_liquid_ignore"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, com.mojang.blaze3d.PrimitiveTopology.DEBUG_LINES)
            .withVertexShader(SHADER_POS_COLOR_VERT)
            .withFragmentShader(SHADER_POS_COLOR_FRAG)
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Screen-space wide line expansion for WORLD_COLORED_LINES_LIQUID_IGNORE. Uses triangles; never depends on glLineWidth.
     */
    public static final RenderPipeline WORLD_WIDE_COLORED_LINES_LIQUID_IGNORE = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_wide_colored_lines_liquid_ignore"))
            .withVertexFormat(LoVisualVertexFormats.POS3_COLOR_LINE, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_WIDE_LINE_VERT)
            .withFragmentShader(SHADER_POS_COLOR_FRAG)
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );

    /**
     * Depth test (GEQUAL); additive; textured triangles; intended for pre-water depth.
     */
    public static final RenderPipeline WORLD_TEXTURED_ADDITIVE_LIQUID_IGNORE = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_textured_additive_liquid_ignore"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_COLOR_VERT)
            .withFragmentShader(SHADER_POS_TEX_COLOR_FRAG)
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE))
            .withCull(false)
            .build()
    );
    /**
     * Depth test (GEQUAL); additive; textured triangles with shader-side tint; intended for pre-water depth.
     */
    public static final RenderPipeline WORLD_TEXTURED_TINT_ADDITIVE_LIQUID_IGNORE = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_textured_tint_additive_liquid_ignore"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_COLOR_VERT)
            .withFragmentShader(SHADER_POS_TEX_COLOR_TINT_FRAG)
            .withSampler("u_Texture")
            .withUniform("TextureTint", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE))
            .withCull(false)
            .build()
    );
    /**
     * Depth test (GEQUAL); translucent; textured triangles; intended for pre-water depth.
     */
    public static final RenderPipeline WORLD_TEXTURED_LIQUID_IGNORE = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_textured_liquid_ignore"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_COLOR_VERT)
            .withFragmentShader(SHADER_POS_TEX_COLOR_FRAG)
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.GEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * UI triangles (pos2 + color).
     */
    public static final RenderPipeline UI_COLORED = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_colored"))
            .withVertexFormat(LoVisualVertexFormats.POS2_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_COLOR_VERT)
            .withFragmentShader(SHADER_POS_COLOR_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    /**
     * UI lines (pos2 + color).
     */
    public static final RenderPipeline UI_COLORED_LINES = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_colored_lines"))
            .withVertexFormat(LoVisualVertexFormats.POS2_COLOR, com.mojang.blaze3d.PrimitiveTopology.DEBUG_LINES)
            .withVertexShader(SHADER_POS_COLOR_VERT)
            .withFragmentShader(SHADER_POS_COLOR_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    /**
     * UI textured triangles (pos2 + tex + color).
     */
    public static final RenderPipeline UI_TEXTURED = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_textured"))
            .withVertexFormat(LoVisualVertexFormats.POS2_TEXTURE_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_COLOR_VERT)
            .withFragmentShader(SHADER_POS_TEX_COLOR_FRAG)
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    /**
     * UI MSDF SVG quads (pos2 + tex + color).
     */
    public static final RenderPipeline UI_SVG_MSDF = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_svg_msdf"))
            .withVertexFormat(LoVisualVertexFormats.POS2_TEXTURE_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_COLOR_VERT)
            .withFragmentShader(SHADER_SVG_MSDF_FRAG)
            .withSampler("u_Texture")
            .withUniform("MsdfText", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    /**
     * UI textured triangles (pos2 + tex + color), additive blend.
     */
    public static final RenderPipeline UI_TEXTURED_ADDITIVE = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_textured_additive"))
            .withVertexFormat(LoVisualVertexFormats.POS2_TEXTURE_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_COLOR_VERT)
            .withFragmentShader(SHADER_POS_TEX_COLOR_FRAG)
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE))
            .withCull(true)
            .build()
    );

    // ======================
    // UI (2D)
    // ======================
    /**
     * UI text (pos2 + tex + color).
     */
    public static final RenderPipeline UI_TEXT = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_text"))
            .withVertexFormat(LoVisualVertexFormats.POS2_TEXTURE_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_TEXT_VERT)
            .withFragmentShader(SHADER_TEXT_FRAG)
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    /**
     * UI MSDF text (pos2 + tex + color).
     */
    public static final RenderPipeline UI_TEXT_MSDF = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_text_msdf"))
            .withVertexFormat(LoVisualVertexFormats.POS2_TEXTURE_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_TEXT_VERT)
            .withFragmentShader(SHADER_TEXT_MSDF_FRAG)
            .withSampler("u_Texture")
            .withUniform("MsdfText", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_TEXT_LIQUID_GLASS = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_text_liquid_glass"))
            .withVertexFormat(LoVisualVertexFormats.POS2_TEXTURE_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_TEXT_VERT)
            .withFragmentShader(SHADER_TEXT_LIQUID_GLASS_FRAG)
            .withSampler("u_Texture")
            .withSampler("u_SceneTexture")
            .withSampler("u_BlurTexture")
            .withUniform("UIBatch", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_TEXT_LIQUID_GLASS_MSDF = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_text_liquid_glass_msdf"))
            .withVertexFormat(LoVisualVertexFormats.POS2_TEXTURE_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_TEXT_VERT)
            .withFragmentShader(SHADER_TEXT_LIQUID_GLASS_MSDF_FRAG)
            .withSampler("u_Texture")
            .withSampler("u_SceneTexture")
            .withSampler("u_BlurTexture")
            .withUniform("UIBatch", UniformType.UNIFORM_BUFFER)
            .withUniform("MsdfText", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_BLUR = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_blur"))
            .withVertexFormat(LoVisualVertexFormats.POS2_TEXTURE_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_COLOR_VERT)
            .withFragmentShader(SHADER_UI_BLUR_FRAG)
            .withSampler("u_Texture")
            .withUniform("UIBlur", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Fullscreen menu background (pos2).
     */
    public static final RenderPipeline MENU_BACKGROUND_WAVES = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/menu_background_waves"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_MENU_BACKGROUND_WAVES_FRAG)
            .withUniform("MenuBackground", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Fullscreen SkyShader passes (pos2). Sky/water/caustic share the same uniform layout.
     */
    public static final RenderPipeline SKY_SHADER_SKY = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/sky_shader_sky"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_SKY_SHADER_SKY_FRAG)
            .withUniform("SkyShader", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    public static final RenderPipeline SKY_SHADER_WATER = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/sky_shader_water"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_SKY_SHADER_WATER_FRAG)
            .withUniform("SkyShader", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    public static final RenderPipeline SKY_SHADER_CAUSTIC = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/sky_shader_caustic"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_SKY_SHADER_CAUSTIC_FRAG)
            .withUniform("SkyShader", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Fullscreen main menu background (pos2).
     */
    public static final RenderPipeline MENU_BACKGROUND_AURORA = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/menu_background_aurora"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_MENU_BACKGROUND_AURORA_FRAG)
            .withUniform("MenuBackground", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Fullscreen postprocess (pos2).
     */
    public static final RenderPipeline DAMAGE_TINT = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/damage_tint"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_DAMAGE_TINT_FRAG)
            .withSampler("u_Texture")
            .withUniform("PostProcess", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Fullscreen visual grading + LUT (pos2).
     */
    public static final RenderPipeline POST_FX = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/post_fx"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_POST_FX_FRAG)
            .withSampler("u_Texture")
            .withSampler("u_Lut")
            .withUniform("PostFX", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Fullscreen camera-only perceptual motion blur (pos2).
     */
    public static final RenderPipeline MOTION_BLUR = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/motion_blur"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_MOTION_BLUR_FRAG)
            .withSampler("u_Texture")
            .withSampler("u_PreviousColor")
            .withUniform("MotionBlur", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Fullscreen scene-depth-aware far depth of field (pos2).
     */
    public static final RenderPipeline DEPTH_OF_FIELD = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/depth_of_field"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_DEPTH_OF_FIELD_FRAG)
            .withSampler("u_Texture")
            .withSampler("u_MainDepth")
            .withSampler("u_TranslucentDepth")
            .withSampler("u_ItemEntityDepth")
            .withSampler("u_ParticlesDepth")
            .withSampler("u_WeatherDepth")
            .withSampler("u_CloudsDepth")
            .withUniform("DepthOfField", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Fullscreen wet-ground screen-space reflection pass (pos2).
     */
    public static final RenderPipeline WET_SURFACE = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/wet_surface"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_WET_SURFACE_FRAG)
            .withSampler("u_Texture")
            .withSampler("u_MainDepth")
            .withUniform("WetSurface", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Fullscreen heat distortion + vignette (pos2).
     */
    public static final RenderPipeline HEAT_FX = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/heat_fx"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_HEAT_FX_FRAG)
            .withSampler("u_Texture")
            .withUniform("Heat", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Fullscreen shader ESP gradient composite (pos2).
     */
    public static final RenderPipeline SHADER_ESP_GRADIENT = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/shader_esp_gradient"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_ESP_GRADIENT_FRAG)
            .withSampler("u_Texture")
            .withUniform("ShaderEspGradient", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Fullscreen shader ESP separable shadow blur (pos2).
     */
    public static final RenderPipeline SHADER_ESP_SHADOW = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/shader_esp_shadow"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_ESP_SHADOW_FRAG)
            .withSampler("u_Texture")
            .withSampler("u_Mask")
            .withUniform("ShaderEspBlur", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Fullscreen shader ESP procedural smoke fill (pos2).
     */
    public static final RenderPipeline SHADER_ESP_SMOKE = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/shader_esp_smoke"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_ESP_SMOKE_FRAG)
            .withSampler("u_Texture")
            .withUniform("ShaderEspSmoke", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * No depth test; additive; textured-color triangles with procedural sun shading.
     */
    public static final RenderPipeline WORLD_SKY_SUN = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_sky_sun"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_COLOR_VERT)
            .withFragmentShader(SHADER_SKY_SUN_FRAG)
            .withUniform("SkySun", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE))
            .withCull(false)
            .build()
    );
    /**
     * Fullscreen nether portal rift (pos2).
     */
    public static final RenderPipeline PORTAL_RIFT = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/portal_rift"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_PORTAL_RIFT_FRAG)
            .withSampler("u_Texture")
            .withUniform("Heat", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Fullscreen sleep overlay (pos2).
     */
    public static final RenderPipeline SLEEP_OVERLAY = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/sleep_overlay"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_SLEEP_OVERLAY_FRAG)
            .withSampler("u_Texture")
            .withUniform("PostProcess", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Fullscreen hand glass (pos2).
     */
    public static final RenderPipeline HAND_GLASS = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/hand_glass"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_HAND_GLASS_FRAG)
            .withSampler("u_Src")
            .withSampler("u_Mask")
            .withUniform("HandGlass", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    public static final RenderPipeline HAND_SMOKE = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/hand_smoke"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_HAND_SMOKE_FRAG)
            .withSampler("u_Src")
            .withSampler("u_Mask")
            .withUniform("HandSmoke", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    public static final RenderPipeline HAND_METALLIC = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/hand_metallic"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_HAND_METALLIC_FRAG)
            .withSampler("u_Src")
            .withSampler("u_Mask")
            .withUniform("HandMetallic", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    public static final RenderPipeline HAND_CHAMS_SOLID = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/hand_chams_solid"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_HAND_CHAMS_SOLID_FRAG)
            .withSampler("u_Src")
            .withSampler("u_Mask")
            .withUniform("HandChams", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    public static final RenderPipeline HAND_CHAMS_CHROMA = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/hand_chams_chroma"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_HAND_CHAMS_CHROMA_FRAG)
            .withSampler("u_Src")
            .withSampler("u_Mask")
            .withUniform("HandChams", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    public static final RenderPipeline HAND_CHAMS_STRIPES = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/hand_chams_stripes"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_HAND_CHAMS_STRIPES_FRAG)
            .withSampler("u_Src")
            .withSampler("u_Mask")
            .withUniform("HandChams", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    public static final RenderPipeline HAND_CHAMS_GLOW = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/hand_chams_glow"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_HAND_CHAMS_GLOW_FRAG)
            .withSampler("u_Src")
            .withSampler("u_Mask")
            .withUniform("HandChams", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    public static final RenderPipeline HAND_CHAMS_SNOW = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/hand_chams_snow"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_HAND_CHAMS_SNOW_FRAG)
            .withSampler("u_Src")
            .withSampler("u_Mask")
            .withUniform("HandChams", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    public static final RenderPipeline HAND_CHAMS_BALATRO = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/hand_chams_balatro"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_HAND_CHAMS_BALATRO_FRAG)
            .withSampler("u_Src")
            .withSampler("u_Mask")
            .withUniform("HandChams", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    public static final RenderPipeline HAND_CHAMS_BLEND = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/hand_chams_blend"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_HAND_CHAMS_BLEND_FRAG)
            .withSampler("u_Shader1")
            .withSampler("u_Shader2")
            .withSampler("u_Mask")
            .withUniform("HandBlend", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    private static final RenderPipeline.Snippet FOG_UNIFORMS = new ExtendedRenderPipelineBuilder()
            .withUniform("Fog", UniformType.UNIFORM_BUFFER)
            .buildSnippet();
    /**
     * No depth test; translucent; textured skybox with vanilla sky/fog blending.
     */
    public static final RenderPipeline WORLD_SKYBOX_TEXTURED_FOG = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, FOG_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_skybox_textured_fog"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_COLOR_SKY_FOG_VERT)
            .withFragmentShader(SHADER_POS_TEX_COLOR_SKY_FOG_FRAG)
            .withSampler("u_Texture")
            .withUniform("SkyboxShader", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * No depth test; additive; textured skybox with vanilla sky/fog blending.
     */
    public static final RenderPipeline WORLD_SKYBOX_TEXTURED_FOG_ADDITIVE = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, FOG_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_skybox_textured_fog_additive"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_COLOR_SKY_FOG_VERT)
            .withFragmentShader(SHADER_POS_TEX_COLOR_SKY_FOG_ADDITIVE_FRAG)
            .withSampler("u_Texture")
            .withUniform("SkyboxShader", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE))
            .withCull(false)
            .build()
    );
    /**
     * Fullscreen procedural Reimagined skybox with sky/fog blending.
     */
    public static final RenderPipeline WORLD_REIMAGINED_SKYBOX_SHADER = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, FOG_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_reimagined_skybox_shader"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_REIMAGINED_SKYBOX_FRAG)
            .withUniform("SkyboxShader", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Fullscreen procedural ShaderSky (Thunder) with sky/fog blending.
     */
    public static final RenderPipeline WORLD_SHADER_SKY_THUNDER = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_shader_sky_thunder"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_SHADER_SKY_THUNDER_FRAG)
            .withUniform("ShaderSky", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    /**
     * Fullscreen procedural ShaderSky (Pulsar) with sky/fog blending.
     */
    public static final RenderPipeline WORLD_SHADER_SKY_PULSAR = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/world_shader_sky_pulsar"))
            .withVertexFormat(LoVisualVertexFormats.POS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_DAMAGE_TINT_VERT)
            .withFragmentShader(SHADER_SHADER_SKY_PULSAR_FRAG)
            .withUniform("ShaderSky", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    private static final RenderPipeline.Snippet UI_BATCH_UNIFORMS = new ExtendedRenderPipelineBuilder()
            .withUniform("UIBatch", UniformType.UNIFORM_BUFFER)
            .buildSnippet();
    public static final RenderPipeline UI_CIRCLE_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_circle_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_LOCAL_COLOR_RECT_PARAMS_VERT)
            .withFragmentShader(SHADER_CIRCLE_BATCH_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_ARC_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_arc_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_LOCAL_COLOR_RECT_PARAMS2_VERT)
            .withFragmentShader(SHADER_ARC_BATCH_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_ORBIZ_RING_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_orbiz_ring_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS5, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_LOCAL_COLOR_RECT_PARAMS5_GENERIC_VERT)
            .withFragmentShader(SHADER_ORBIZ_RING_BATCH_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_ROUNDED_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_rounded_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_LOCAL_COLOR_RECT_PARAMS_VERT)
            .withFragmentShader(SHADER_ROUNDED_RECT_BATCH_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_ROUNDED_FILL_SMOKE_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_rounded_fill_smoke_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS5, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_LOCAL_COLOR_RECT_PARAMS5_VERT)
            .withFragmentShader(SHADER_UI_ROUNDED_FILL_SMOKE_BATCH_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_ROUNDED_STROKE_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_rounded_stroke_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_LOCAL_COLOR_RECT_PARAMS_VERT)
            .withFragmentShader(SHADER_ROUNDED_RECT_STROKE_BATCH_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_ROUNDED_STROKE_ANGULAR_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_rounded_stroke_angular_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_LOCAL_COLOR_RECT_PARAMS2_VERT)
            .withFragmentShader(SHADER_ROUNDED_RECT_STROKE_ANGULAR_BATCH_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_CHAMFERED_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_chamfered_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_LOCAL_COLOR_RECT_PARAMS2_VERT)
            .withFragmentShader(SHADER_CHAMFERED_RECT_BATCH_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_SHAPE_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_shape_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS5, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_LOCAL_COLOR_RECT_PARAMS5_VERT)
            .withFragmentShader(SHADER_UI_SHAPE_BATCH_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_CHAMFERED_STROKE_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_chamfered_stroke_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_LOCAL_COLOR_RECT_PARAMS2_VERT)
            .withFragmentShader(SHADER_CHAMFERED_RECT_STROKE_BATCH_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_ROUNDED_STROKE_CORNERS_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_rounded_stroke_corners_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_LOCAL_COLOR_RECT_PARAMS2_VERT)
            .withFragmentShader(SHADER_ROUNDED_RECT_STROKE_CORNERS_BATCH_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_ROUNDED_CORNERS_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_rounded_corners_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_LOCAL_COLOR_RECT_PARAMS2_VERT)
            .withFragmentShader(SHADER_ROUNDED_RECT_CORNERS_BATCH_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_ROUNDED_GLOW_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_rounded_glow_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_LOCAL_COLOR_RECT_PARAMS_VERT)
            .withFragmentShader(SHADER_ROUNDED_RECT_GLOW_BATCH_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE))
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_ROUNDED_SHADOW_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_rounded_shadow_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_LOCAL_COLOR_RECT_PARAMS_VERT)
            .withFragmentShader(SHADER_ROUNDED_RECT_SHADOW_BATCH_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_ROUNDED_SOFT_SHADOW_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_rounded_soft_shadow_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_LOCAL_COLOR_RECT_PARAMS_VERT)
            .withFragmentShader(SHADER_ROUNDED_RECT_SOFT_SHADOW_BATCH_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_RADIAL_GLOW_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_radial_glow_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_LOCAL_COLOR_RECT_PARAMS2_VERT)
            .withFragmentShader(SHADER_RADIAL_GLOW_BATCH_FRAG)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE))
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_ROUNDED_TEXTURED_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_rounded_textured_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_TEXTURE_LOCAL_COLOR_RECT_PARAMS, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_LOCAL_COLOR_RECT_PARAMS_VERT)
            .withFragmentShader(SHADER_ROUNDED_RECT_TEX_BATCH_FRAG)
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_ROUNDED_TEXTURED_MASK_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_rounded_textured_mask_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_TEXTURE_LOCAL_COLOR_RECT_PARAMS, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_LOCAL_COLOR_RECT_PARAMS_VERT)
            .withFragmentShader(SHADER_ROUNDED_RECT_TEX_MASK_BATCH_FRAG)
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    );
    public static final RenderPipeline UI_BLUR_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_blur_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_LOCAL_COLOR_RECT_PARAMS_VERT)
            .withFragmentShader(SHADER_UI_BLUR_BATCH_FRAG)
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    public static final RenderPipeline UI_BLUR_BATCH_CORNERS = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_blur_batch_corners"))
            .withVertexFormat(LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_LOCAL_COLOR_RECT_PARAMS2_VERT)
            .withFragmentShader(SHADER_UI_BLUR_BATCH_CORNERS_FRAG)
            .withSampler("u_Texture")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );
    public static final RenderPipeline UI_LIQUID_GLASS_BATCH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS, UI_BATCH_UNIFORMS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/ui_liquid_glass_batch"))
            .withVertexFormat(LoVisualVertexFormats.POS2_TEXTURE_LOCAL_COLOR_RECT_PARAMS2, com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
            .withVertexShader(SHADER_POS_TEX_LOCAL_COLOR_RECT_PARAMS2_VERT)
            .withFragmentShader(SHADER_UI_LIQUID_GLASS_BATCH_FRAG)
            .withSampler("u_Texture")
            .withSampler("u_BlurTexture")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build()
    );

    /**
     * Compile all registered pipelines (use after resource reload).
     */
    public static void precompile(ResourceManager resources) {
        final GpuDevice device;
        try {
            device = RenderSystem.getDevice();
        } catch (Throwable t) {
            DebugLog.warn("[LoVisual] RenderSystem not ready in precompile(ResourceManager)");
            return;
        }

        DebugLog.renderThread("[LoVisual] Precompiling pipelines: " + PIPELINES.size());

        for (RenderPipeline pipeline : PIPELINES) {
            DebugLog.renderThread("[LoVisual] -> pipeline " + pipeline.getLocation());
            DebugLog.renderThread("   VS = " + pipeline.getVertexShader());
            DebugLog.renderThread("   FS = " + pipeline.getFragmentShader());
            DebugLog.renderThread("   VF = " + pipeline.getVertexFormatBinding(0));
            DebugLog.renderThread("   MODE = " + pipeline.getPrimitiveTopology());

            device.precompilePipeline(pipeline, (identifier, shaderType) -> {
                DebugLog.renderThread("[LoVisual]   loading " + shaderType + " " + identifier);
                return LoVisualShaderSources.load(resources, identifier, shaderType);
            });
        }

        DebugLog.renderThread("[LoVisual] Precompile finished");
    }

    /**
     * compile using the current MinecraftClient resource manager.
     */

    public static List<RenderPipeline> all() {
        return Collections.unmodifiableList(PIPELINES);
    }

    public static RenderPipeline.Snippet meshUniforms() {
        return MESH_UNIFORMS;
    }

    public static RenderPipeline registerAddonPipeline(RenderPipeline pipeline) {
        return add(pipeline);
    }

    private static RenderPipeline add(RenderPipeline pipeline) {
        declareDefaultShapeClipContract(pipeline);
        PIPELINES.add(pipeline);
        RenderPipelineRegistry.global().registerNative(pipeline);
        return pipeline;
    }

    private static void declareDefaultShapeClipContract(RenderPipeline pipeline) {
        if (!(pipeline instanceof IRenderPipeline lovisualPipeline) || pipeline.getLocation() == null) return;
        ShapeClipRenderPassContract current = lovisualPipeline.lovisual$getShapeClipContract();
        if (current != ShapeClipRenderPassContract.NONE) return;

        String namespace = pipeline.getLocation().getNamespace();
        String path = pipeline.getLocation().getPath();
        if ("lovisual".equals(namespace) && (path.startsWith("pipeline/ui_") || path.equals("pipeline/gui_texture_lookup"))) {
            lovisualPipeline.lovisual$setShapeClipContract(ShapeClipRenderPassContract.WHEN_ACTIVE);
        }
    }

}
