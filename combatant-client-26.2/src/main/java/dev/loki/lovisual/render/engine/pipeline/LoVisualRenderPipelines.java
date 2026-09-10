/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.pipeline;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.loki.lovisual.mixininterface.render.IRenderPipeline;
import dev.loki.lovisual.render.engine.pipeline.pipelines.PipelineTemplates;
import dev.loki.lovisual.render.engine.pipeline.pipelines.ScreenPipelines;
import dev.loki.lovisual.render.engine.pipeline.pipelines.UiPipelines;
import dev.loki.lovisual.render.engine.pipeline.pipelines.WorldPipelines;
import dev.loki.lovisual.render.engine.rhi.clip.ShapeClipRenderPassContract;
import dev.loki.lovisual.render.engine.rhi.pipeline.layout.RenderPipelineRegistry;
import dev.loki.lovisual.render.engine.shader.LoVisualShaderSources;
import dev.loki.lovisual.render.engine.vertex.LoVisualVertexFormats;
import dev.loki.lovisual.util.logging.DebugLog;
import dev.loki.lovisual.features.module.modules.visuals.effects.KillEffect;
import dev.loki.lovisual.features.module.modules.visuals.esp.ESP;
import dev.loki.lovisual.features.module.modules.visuals.fx.MotionBlur;
import dev.loki.lovisual.features.module.modules.visuals.fx.PostFX;
import dev.loki.lovisual.features.module.modules.visuals.scene.BlockHighlight;
import dev.loki.lovisual.features.module.modules.visuals.weather.ShaderSky;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pipeline registry for LoVisual's custom rendering engine.
 *
 * <p>Public API facade: every shader identifier and pipeline constant keeps its original
 * name and value. The repetitive builder chains live in the template classes of
 * {@link dev.loki.lovisual.render.engine.pipeline.pipelines}.</p>
 *
 * <p>Keep this class free of module-specific logic: it is the foundation for all custom 3D/2D effects.</p>
 */
public enum LoVisualRenderPipelines {
    ;
    // ======================
    // Shaders (assets/lovisual/shaders/*)
    // ======================
    // Core colored / line / fog shaders
    public static final Identifier SHADER_POS_COLOR_VERT = shader("pos_color.vert");
    public static final Identifier SHADER_WIDE_LINE_VERT = shader("wide_line.vert");
    public static final Identifier SHADER_POS_COLOR_FRAG = shader("pos_color.frag");
    public static final Identifier SHADER_POS_COLOR_FOG_VERT = shader("pos_color_fog.vert");
    public static final Identifier SHADER_POS_COLOR_FOG_FRAG = shader("pos_color_fog.frag");
    // Rect-parameter vertex shaders (batched shapes)
    public static final Identifier SHADER_POS_COLOR_RECT_PARAMS_VERT = shader("pos_color_rect_params.vert");
    public static final Identifier SHADER_POS_COLOR_RECT_PARAMS2_VERT = shader("pos_color_rect_params2.vert");
    public static final Identifier SHADER_POS_LOCAL_COLOR_RECT_PARAMS_VERT = shader("pos_local_color_rect_params.vert");
    public static final Identifier SHADER_POS_LOCAL_COLOR_RECT_PARAMS2_VERT = shader("pos_local_color_rect_params2.vert");
    public static final Identifier SHADER_POS_LOCAL_COLOR_RECT_PARAMS5_VERT = shader("pos_local_color_rect_params5.vert");
    public static final Identifier SHADER_POS_LOCAL_COLOR_RECT_PARAMS5_GENERIC_VERT = shader("pos_local_color_rect_params5_generic.vert");
    // Textured primitives & GUI lookup
    public static final Identifier SHADER_POS_TEX_COLOR_VERT = shader("pos_tex_color.vert");
    public static final Identifier SHADER_POS_TEX_COLOR_PARAMS2_VERT = shader("pos_tex_color_params2.vert");
    public static final Identifier SHADER_POS_TEX_COLOR_FRAG = shader("pos_tex_color.frag");
    public static final Identifier SHADER_GUI_TEXTURE_LOOKUP_FRAG = shader("gui_texture_lookup.frag");
    public static final Identifier SHADER_POS_TEX_COLOR_TINT_FRAG = shader("pos_tex_color_tint.frag");
    public static final Identifier SHADER_POS_TEX_COLOR_SKY_FOG_VERT = shader("pos_tex_color_sky_fog.vert");
    public static final Identifier SHADER_POS_TEX_COLOR_SKY_FOG_FRAG = shader("pos_tex_color_sky_fog.frag");
    public static final Identifier SHADER_POS_TEX_COLOR_SKY_FOG_ADDITIVE_FRAG = shader("pos_tex_color_sky_fog_additive.frag");
    // Skyboxes & shader skies
    public static final Identifier SHADER_POS_COLOR_SKYBOX_SHADER_VERT = shader("pos_color_skybox_shader.vert");
    public static final Identifier SHADER_REIMAGINED_SKYBOX_FRAG = shader("reimagined_skybox.frag");
    public static final Identifier SHADER_SHADER_SKY_THUNDER_FRAG = shader("shader_sky_thunder.frag");
    public static final Identifier SHADER_SHADER_SKY_PULSAR_FRAG = shader("shader_sky_pulsar.frag");
    public static final Identifier SHADER_POS_TEX_COLOR_RECT_PARAMS_VERT = shader("pos_tex_color_rect_params.vert");
    public static final Identifier SHADER_POS_TEX_LOCAL_COLOR_RECT_PARAMS_VERT = shader("pos_tex_local_color_rect_params.vert");
    public static final Identifier SHADER_POS_TEX_LOCAL_COLOR_RECT_PARAMS2_VERT = shader("pos_tex_local_color_rect_params2.vert");
    public static final Identifier SHADER_POS_TEX_COLOR_RECT_PARAMS2_VERT = shader("pos_tex_color_rect_params2.vert");
    // Text & MSDF
    public static final Identifier SHADER_TEXT_VERT = shader("text.vert");
    public static final Identifier SHADER_TEXT_FRAG = shader("text.frag");
    public static final Identifier SHADER_TEXT_MSDF_FRAG = shader("text_msdf.frag");
    public static final Identifier SHADER_SVG_MSDF_FRAG = shader("svg_msdf.frag");
    public static final Identifier SHADER_TEXT_LIQUID_GLASS_FRAG = shader("text_liquid_glass.frag");
    public static final Identifier SHADER_TEXT_LIQUID_GLASS_MSDF_FRAG = shader("text_liquid_glass_msdf.frag");
    // Batched UI shapes (SDF fragments)
    public static final Identifier SHADER_CIRCLE_BATCH_FRAG = shader("circle_batch.frag");
    public static final Identifier SHADER_ARC_BATCH_FRAG = shader("arc_batch.frag");
    public static final Identifier SHADER_ORBIZ_RING_BATCH_FRAG = shader("orbiz_ring_batch.frag");
    public static final Identifier SHADER_WORLD_DECAL_SDF_FRAG = shader("world_decal_sdf.frag");
    public static final Identifier SHADER_ROUNDED_RECT_STROKE_CORNERS_BATCH_FRAG = shader("rounded_rect_stroke_corners_batch.frag");
    public static final Identifier SHADER_ROUNDED_RECT_BATCH_FRAG = shader("rounded_rect_batch.frag");
    public static final Identifier SHADER_UI_ROUNDED_FILL_SMOKE_BATCH_FRAG = shader("ui_rounded_fill_smoke_batch.frag");
    public static final Identifier SHADER_ROUNDED_RECT_STROKE_BATCH_FRAG = shader("rounded_rect_stroke_batch.frag");
    public static final Identifier SHADER_ROUNDED_RECT_STROKE_ANGULAR_BATCH_FRAG = shader("rounded_rect_stroke_angular_batch.frag");
    public static final Identifier SHADER_CHAMFERED_RECT_BATCH_FRAG = shader("chamfered_rect_batch.frag");
    public static final Identifier SHADER_UI_SHAPE_BATCH_FRAG = shader("ui_shape_batch.frag");
    public static final Identifier SHADER_CHAMFERED_RECT_STROKE_BATCH_FRAG = shader("chamfered_rect_stroke_batch.frag");
    public static final Identifier SHADER_ROUNDED_RECT_CORNERS_BATCH_FRAG = shader("rounded_rect_corners_batch.frag");
    public static final Identifier SHADER_ROUNDED_RECT_GLOW_BATCH_FRAG = shader("rounded_rect_glow_batch.frag");
    public static final Identifier SHADER_ROUNDED_RECT_SHADOW_BATCH_FRAG = shader("rounded_rect_shadow_batch.frag");
    public static final Identifier SHADER_ROUNDED_RECT_SOFT_SHADOW_BATCH_FRAG = shader("rounded_rect_soft_shadow_batch.frag");
    public static final Identifier SHADER_RADIAL_GLOW_BATCH_FRAG = shader("radial_glow_batch.frag");
    public static final Identifier SHADER_ROUNDED_RECT_TEX_BATCH_FRAG = shader("rounded_rect_tex_batch.frag");
    public static final Identifier SHADER_ROUNDED_RECT_TEX_MASK_BATCH_FRAG = shader("rounded_rect_tex_mask_batch.frag");
    public static final Identifier SHADER_UI_BLUR_BATCH_FRAG = shader("ui_blur_batch.frag");
    public static final Identifier SHADER_UI_BLUR_BATCH_CORNERS_FRAG = shader("ui_blur_batch_corners.frag");
    public static final Identifier SHADER_UI_LIQUID_GLASS_BATCH_FRAG = shader("ui_liquid_glass_batch.frag");
    // Fullscreen post-processing & overlays
    public static final Identifier SHADER_DAMAGE_TINT_VERT = shader("damage_tint.vert");
    public static final Identifier SHADER_DAMAGE_TINT_FRAG = shader("damage_tint.frag");
    public static final Identifier SHADER_POST_FX_FRAG = shader("post_fx.frag");
    public static final Identifier SHADER_MOTION_BLUR_FRAG = shader("motion_blur.frag");
    public static final Identifier SHADER_DEPTH_OF_FIELD_FRAG = shader("depth_of_field.frag");
    public static final Identifier SHADER_WET_SURFACE_FRAG = shader("wet_surface.frag");
    public static final Identifier SHADER_HEAT_FX_FRAG = shader("heat_fx.frag");
    public static final Identifier SHADER_ESP_GRADIENT_FRAG = shader("shader_esp_gradient.frag");
    public static final Identifier SHADER_ESP_SHADOW_FRAG = shader("shader_esp_shadow.frag");
    public static final Identifier SHADER_ESP_SMOKE_FRAG = shader("shader_esp_smoke.frag");
    public static final Identifier SHADER_PORTAL_RIFT_FRAG = shader("portal_rift.frag");
    public static final Identifier SHADER_SLEEP_OVERLAY_FRAG = shader("sleep_overlay.frag");
    // Hand effects & chams
    public static final Identifier SHADER_HAND_SMOKE_FRAG = shader("hand_smoke.frag");
    public static final Identifier SHADER_HAND_METALLIC_FRAG = shader("hand_metallic.frag");
    public static final Identifier SHADER_HAND_NOISE_FRAG = shader("hand_noise.frag");
    public static final Identifier SHADER_UI_BLUR_FRAG = shader("ui_blur.frag");
    public static final Identifier SHADER_HAND_GLASS_FRAG = shader("hand_glass.frag");
    public static final Identifier SHADER_HAND_CHAMS_SOLID_FRAG = shader("hand_chams_solid.frag");
    public static final Identifier SHADER_HAND_CHAMS_CHROMA_FRAG = shader("hand_chams_chroma.frag");
    public static final Identifier SHADER_HAND_CHAMS_STRIPES_FRAG = shader("hand_chams_stripes.frag");
    public static final Identifier SHADER_HAND_CHAMS_GLOW_FRAG = shader("hand_chams_glow.frag");
    public static final Identifier SHADER_HAND_CHAMS_SNOW_FRAG = shader("hand_chams_snow.frag");
    public static final Identifier SHADER_HAND_CHAMS_BALATRO_FRAG = shader("hand_chams_balatro.frag");
    public static final Identifier SHADER_HAND_CHAMS_BLEND_FRAG = shader("hand_chams_blend.frag");
    // Sky & menu backgrounds
    public static final Identifier SHADER_SKY_SUN_FRAG = shader("sky_sun.frag");
    public static final Identifier SHADER_MENU_BACKGROUND_WAVES_FRAG = shader("menu_background_waves.frag");
    public static final Identifier SHADER_MENU_BACKGROUND_AURORA_FRAG = shader("menu_background_aurora.frag");
    public static final Identifier SHADER_SKY_SHADER_SKY_FRAG = shader("sky_shader_sky.frag");
    public static final Identifier SHADER_SKY_SHADER_WATER_FRAG = shader("sky_shader_water.frag");
    // KillEffect procedural beam shaders
    public static final Identifier SHADER_KILL_EFFECT_PORTAL_FRAG = shader("kill_effect_portal.frag");
    public static final Identifier SHADER_KILL_EFFECT_TORNADO_FRAG = shader("kill_effect_tornado.frag");
    // BlockHighlight procedural shader outlines
    public static final Identifier SHADER_BLOCK_HIGHLIGHT_WATERCAUSTIC_FRAG = shader("block_highlight_watercaustic.frag");
    public static final Identifier SHADER_BLOCK_HIGHLIGHT_SCANLINES_FRAG = shader("block_highlight_scanlines.frag");
    public static final Identifier SHADER_BLOCK_HIGHLIGHT_HOLOGRAM_FRAG = shader("block_highlight_hologram.frag");
    // SkyShader nebula
    public static final Identifier SHADER_SKY_SHADER_NEBULA_FRAG = shader("sky_shader_nebula.frag");
    public static final Identifier SHADER_SKY_SHADER_CAUSTIC_FRAG = shader("sky_shader_caustic.frag");

    private static final List<RenderPipeline> PIPELINES = new ArrayList<>();

    public static final RenderPipeline GUI_TEXTURE_LOOKUP = add(ScreenPipelines.guiTextureLookup());

    // ======================
    // World (3D)
    // ======================
    /** No depth test; translucent; triangles. */
    public static final RenderPipeline WORLD_COLORED = add(WorldPipelines.colored("world_colored", false, BlendFunction.TRANSLUCENT));
    /** No depth test; translucent; lines. */
    public static final RenderPipeline WORLD_COLORED_LINES = add(WorldPipelines.coloredLines("world_colored_lines", false));
    /** Screen-space wide line expansion for WORLD_COLORED_LINES. Uses triangles; never depends on glLineWidth. */
    public static final RenderPipeline WORLD_WIDE_COLORED_LINES = add(WorldPipelines.wideLines("world_wide_colored_lines", false));
    /** Depth test (GEQUAL); translucent; triangles. */
    public static final RenderPipeline WORLD_COLORED_DEPTH = add(WorldPipelines.colored("world_colored_depth", true, BlendFunction.TRANSLUCENT));
    /** No depth test; translucent; textured triangles. */
    public static final RenderPipeline WORLD_TEXTURED = add(WorldPipelines.textured("world_textured", false, BlendFunction.TRANSLUCENT));
    /** Depth test (GEQUAL); translucent; textured triangles. */
    public static final RenderPipeline WORLD_TEXTURED_DEPTH = add(WorldPipelines.textured("world_textured_depth", true, BlendFunction.TRANSLUCENT));
    /** Depth test (GEQUAL); additive; colored triangles. */
    public static final RenderPipeline WORLD_COLORED_ADDITIVE_DEPTH = add(WorldPipelines.colored("world_colored_additive_depth", true, PipelineTemplates.ADDITIVE));
    /** No depth test; translucent; world decal quads with SDF fill/stroke. */
    public static final RenderPipeline WORLD_DECAL_SDF = add(WorldPipelines.decalSdf("world_decal_sdf", false));
    /** Depth test (GEQUAL); translucent; world decal quads with SDF fill/stroke. */
    public static final RenderPipeline WORLD_DECAL_SDF_DEPTH = add(WorldPipelines.decalSdf("world_decal_sdf_depth", true));
    /** No depth test; translucent; world text quads. */
    public static final RenderPipeline WORLD_TEXT = add(WorldPipelines.worldText("world_text", false, SHADER_TEXT_FRAG, false));
    /** Depth test (GEQUAL); translucent; world text quads. */
    public static final RenderPipeline WORLD_TEXT_DEPTH = add(WorldPipelines.worldText("world_text_depth", true, SHADER_TEXT_FRAG, false));
    /** No depth test; translucent; world MSDF text quads. */
    public static final RenderPipeline WORLD_TEXT_MSDF = add(WorldPipelines.worldText("world_text_msdf", false, SHADER_TEXT_MSDF_FRAG, true));
    /** Depth test (GEQUAL); translucent; world MSDF text quads. */
    public static final RenderPipeline WORLD_TEXT_MSDF_DEPTH = add(WorldPipelines.worldText("world_text_msdf_depth", true, SHADER_TEXT_MSDF_FRAG, true));
    /** No depth test; additive; textured triangles. */
    public static final RenderPipeline WORLD_TEXTURED_ADDITIVE = add(WorldPipelines.textured("world_textured_additive", false, PipelineTemplates.ADDITIVE));
    /** No depth test; additive; textured triangles with shader-side tint. */
    public static final RenderPipeline WORLD_TEXTURED_TINT_ADDITIVE = add(WorldPipelines.tintedTextured("world_textured_tint_additive", false));
    /** Depth test (GEQUAL); additive; textured triangles. */
    public static final RenderPipeline WORLD_TEXTURED_ADDITIVE_DEPTH = add(WorldPipelines.textured("world_textured_additive_depth", true, PipelineTemplates.ADDITIVE));
    /** Depth test (GEQUAL); translucent; lines. */
    public static final RenderPipeline WORLD_COLORED_LINES_DEPTH = add(WorldPipelines.coloredLines("world_colored_lines_depth", true));
    /** Screen-space wide line expansion for WORLD_COLORED_LINES_DEPTH. Uses triangles; never depends on glLineWidth. */
    public static final RenderPipeline WORLD_WIDE_COLORED_LINES_DEPTH = add(WorldPipelines.wideLines("world_wide_colored_lines_depth", true));
    /** Depth test (GEQUAL); translucent; triangles; lets liquids blend over (no depth write). */
    public static final RenderPipeline WORLD_COLORED_LIQUID_BLEND = add(WorldPipelines.colored("world_colored_liquid_blend", true, BlendFunction.TRANSLUCENT));
    /** Depth test (GEQUAL); translucent; lines; lets liquids blend over (no depth write). */
    public static final RenderPipeline WORLD_COLORED_LINES_LIQUID_BLEND = add(WorldPipelines.coloredLines("world_colored_lines_liquid_blend", true));
    /** Screen-space wide line expansion for WORLD_COLORED_LINES_LIQUID_BLEND. Uses triangles; never depends on glLineWidth. */
    public static final RenderPipeline WORLD_WIDE_COLORED_LINES_LIQUID_BLEND = add(WorldPipelines.wideLines("world_wide_colored_lines_liquid_blend", true));
    /** Depth test (GEQUAL); translucent; textured triangles; lets liquids blend over (no depth write). */
    public static final RenderPipeline WORLD_TEXTURED_LIQUID_BLEND = add(WorldPipelines.textured("world_textured_liquid_blend", true, BlendFunction.TRANSLUCENT));
    /** Depth test (GEQUAL); additive; textured triangles; lets liquids blend over (no depth write). */
    public static final RenderPipeline WORLD_TEXTURED_ADDITIVE_LIQUID_BLEND = add(WorldPipelines.textured("world_textured_additive_liquid_blend", true, PipelineTemplates.ADDITIVE));
    /** Depth test (GEQUAL); translucent; triangles; intended for above-liquids pass (no depth write). */
    public static final RenderPipeline WORLD_COLORED_LIQUID_IGNORE = add(WorldPipelines.colored("world_colored_liquid_ignore", true, BlendFunction.TRANSLUCENT));
    /** Depth test (GEQUAL); translucent; lines; writes depth so liquids render below (above-liquids). */
    public static final RenderPipeline WORLD_COLORED_LINES_LIQUID_IGNORE = add(WorldPipelines.coloredLines("world_colored_lines_liquid_ignore", true));
    /** Screen-space wide line expansion for WORLD_COLORED_LINES_LIQUID_IGNORE. Uses triangles; never depends on glLineWidth. */
    public static final RenderPipeline WORLD_WIDE_COLORED_LINES_LIQUID_IGNORE = add(WorldPipelines.wideLines("world_wide_colored_lines_liquid_ignore", true));
    /** Depth test (GEQUAL); additive; textured triangles; intended for pre-water depth. */
    public static final RenderPipeline WORLD_TEXTURED_ADDITIVE_LIQUID_IGNORE = add(WorldPipelines.textured("world_textured_additive_liquid_ignore", true, PipelineTemplates.ADDITIVE));
    /** Depth test (GEQUAL); additive; textured triangles with shader-side tint; intended for pre-water depth. */
    public static final RenderPipeline WORLD_TEXTURED_TINT_ADDITIVE_LIQUID_IGNORE = add(WorldPipelines.tintedTextured("world_textured_tint_additive_liquid_ignore", true));
    /** Depth test (GEQUAL); translucent; textured triangles; intended for pre-water depth. */
    public static final RenderPipeline WORLD_TEXTURED_LIQUID_IGNORE = add(WorldPipelines.textured("world_textured_liquid_ignore", true, BlendFunction.TRANSLUCENT));

    // ======================
    // UI (2D)
    // ======================
    /** UI triangles (pos2 + color). */
    public static final RenderPipeline UI_COLORED = add(UiPipelines.colored("ui_colored", PrimitiveTopology.TRIANGLES));
    /** UI lines (pos2 + color). */
    public static final RenderPipeline UI_COLORED_LINES = add(UiPipelines.colored("ui_colored_lines", PrimitiveTopology.DEBUG_LINES));
    /** UI textured triangles (pos2 + tex + color). */
    public static final RenderPipeline UI_TEXTURED = add(UiPipelines.textured("ui_textured", SHADER_POS_TEX_COLOR_VERT, SHADER_POS_TEX_COLOR_FRAG, BlendFunction.TRANSLUCENT, true, null, "u_Texture"));
    /** UI MSDF SVG quads (pos2 + tex + color). */
    public static final RenderPipeline UI_SVG_MSDF = add(UiPipelines.textured("ui_svg_msdf", SHADER_POS_TEX_COLOR_VERT, SHADER_SVG_MSDF_FRAG, BlendFunction.TRANSLUCENT, true, "MsdfText", "u_Texture"));
    /** UI textured triangles (pos2 + tex + color), additive blend. */
    public static final RenderPipeline UI_TEXTURED_ADDITIVE = add(UiPipelines.textured("ui_textured_additive", SHADER_POS_TEX_COLOR_VERT, SHADER_POS_TEX_COLOR_FRAG, PipelineTemplates.ADDITIVE, true, null, "u_Texture"));
    /** UI text (pos2 + tex + color). */
    public static final RenderPipeline UI_TEXT = add(UiPipelines.textured("ui_text", SHADER_TEXT_VERT, SHADER_TEXT_FRAG, BlendFunction.TRANSLUCENT, true, null, "u_Texture"));
    /** UI MSDF text (pos2 + tex + color). */
    public static final RenderPipeline UI_TEXT_MSDF = add(UiPipelines.textured("ui_text_msdf", SHADER_TEXT_VERT, SHADER_TEXT_MSDF_FRAG, BlendFunction.TRANSLUCENT, true, "MsdfText", "u_Texture"));
    public static final RenderPipeline UI_TEXT_LIQUID_GLASS = add(UiPipelines.textLiquidGlass("ui_text_liquid_glass", SHADER_TEXT_LIQUID_GLASS_FRAG, false));
    public static final RenderPipeline UI_TEXT_LIQUID_GLASS_MSDF = add(UiPipelines.textLiquidGlass("ui_text_liquid_glass_msdf", SHADER_TEXT_LIQUID_GLASS_MSDF_FRAG, true));
    public static final RenderPipeline UI_BLUR = add(UiPipelines.textured("ui_blur", SHADER_POS_TEX_COLOR_VERT, SHADER_UI_BLUR_FRAG, BlendFunction.TRANSLUCENT, false, "UIBlur", "u_Texture"));
    /** Fullscreen menu background (pos2). */
    public static final RenderPipeline MENU_BACKGROUND_WAVES = add(ScreenPipelines.screen("menu_background_waves", SHADER_MENU_BACKGROUND_WAVES_FRAG, "MenuBackground"));
    /** Fullscreen SkyShader passes (pos2). Sky/water/caustic share the same uniform layout. */
    public static final RenderPipeline SKY_SHADER_SKY = add(ScreenPipelines.screen("sky_shader_sky", SHADER_SKY_SHADER_SKY_FRAG, "SkyShader"));
    public static final RenderPipeline SKY_SHADER_WATER = add(ScreenPipelines.screen("sky_shader_water", SHADER_SKY_SHADER_WATER_FRAG, "SkyShader"));
    public static final RenderPipeline SKY_SHADER_CAUSTIC = add(ScreenPipelines.screen("sky_shader_caustic", SHADER_SKY_SHADER_CAUSTIC_FRAG, "SkyShader"));
    public static final RenderPipeline SKY_SHADER_NEBULA = add(ScreenPipelines.screen("sky_shader_nebula", SHADER_SKY_SHADER_NEBULA_FRAG, "SkyShaderData"));
    /** Fullscreen main menu background (pos2). */
    public static final RenderPipeline MENU_BACKGROUND_AURORA = add(ScreenPipelines.screen("menu_background_aurora", SHADER_MENU_BACKGROUND_AURORA_FRAG, "MenuBackground"));
    /** Fullscreen postprocess (pos2). */
    public static final RenderPipeline DAMAGE_TINT = add(ScreenPipelines.screen("damage_tint", SHADER_DAMAGE_TINT_FRAG, "PostProcess", "u_Texture"));
    /** Fullscreen visual grading + LUT (pos2). */
    public static final RenderPipeline POST_FX = add(ScreenPipelines.screen("post_fx", SHADER_POST_FX_FRAG, "PostFX", "u_Texture", "u_Lut"));
    /** Fullscreen camera-only perceptual motion blur (pos2). */
    public static final RenderPipeline MOTION_BLUR = add(ScreenPipelines.screen("motion_blur", SHADER_MOTION_BLUR_FRAG, "MotionBlur", "u_Texture", "u_PreviousColor"));
    /** Fullscreen scene-depth-aware far depth of field (pos2). */
    public static final RenderPipeline DEPTH_OF_FIELD = add(ScreenPipelines.screen("depth_of_field", SHADER_DEPTH_OF_FIELD_FRAG, "DepthOfField",
            "u_Texture", "u_MainDepth", "u_TranslucentDepth", "u_ItemEntityDepth", "u_ParticlesDepth", "u_WeatherDepth", "u_CloudsDepth"));
    /** Fullscreen wet-ground screen-space reflection pass (pos2). */
    public static final RenderPipeline WET_SURFACE = add(ScreenPipelines.screen("wet_surface", SHADER_WET_SURFACE_FRAG, "WetSurface", "u_Texture", "u_MainDepth"));
    /** Fullscreen heat distortion + vignette (pos2). */
    public static final RenderPipeline HEAT_FX = add(ScreenPipelines.screen("heat_fx", SHADER_HEAT_FX_FRAG, "Heat", "u_Texture"));
    /** Fullscreen shader ESP gradient composite (pos2). */
    public static final RenderPipeline SHADER_ESP_GRADIENT = add(ScreenPipelines.screen("shader_esp_gradient", SHADER_ESP_GRADIENT_FRAG, "ShaderEspGradient", "u_Texture"));
    /** Fullscreen shader ESP separable shadow blur (pos2). */
    public static final RenderPipeline SHADER_ESP_SHADOW = add(ScreenPipelines.screen("shader_esp_shadow", SHADER_ESP_SHADOW_FRAG, "ShaderEspBlur", "u_Texture", "u_Mask"));
    /** Fullscreen shader ESP procedural smoke fill (pos2). */
    public static final RenderPipeline SHADER_ESP_SMOKE = add(ScreenPipelines.screen("shader_esp_smoke", SHADER_ESP_SMOKE_FRAG, "ShaderEspSmoke", "u_Texture"));
    /** No depth test; additive; textured-color triangles with procedural sun shading. */
    public static final RenderPipeline WORLD_SKY_SUN = add(WorldPipelines.skySun("world_sky_sun"));
    /** No depth test; additive; KillEffect "Portal" procedural beam shader. */
    public static final RenderPipeline WORLD_KILL_EFFECT_PORTAL =
            add(WorldPipelines.killEffectBeam("world_kill_effect_portal", SHADER_KILL_EFFECT_PORTAL_FRAG));
    /** No depth test; additive; KillEffect "Tornado" procedural beam shader. */
    public static final RenderPipeline WORLD_KILL_EFFECT_TORNADO =
            add(WorldPipelines.killEffectBeam("world_kill_effect_tornado", SHADER_KILL_EFFECT_TORNADO_FRAG));
    /** No depth test; additive; BlockHighlight "WaterCaustic" shader outline. */
    public static final RenderPipeline WORLD_BLOCK_HIGHLIGHT_WATERCAUSTIC =
            add(WorldPipelines.waterHighlight("world_block_highlight_watercaustic", SHADER_BLOCK_HIGHLIGHT_WATERCAUSTIC_FRAG));
    /** No depth test; additive; BlockHighlight "ScanLines" shader outline. */
    public static final RenderPipeline WORLD_BLOCK_HIGHLIGHT_SCANLINES =
            add(WorldPipelines.waterHighlight("world_block_highlight_scanlines", SHADER_BLOCK_HIGHLIGHT_SCANLINES_FRAG));
    /** No depth test; additive; BlockHighlight "Hologram" shader outline. */
    public static final RenderPipeline WORLD_BLOCK_HIGHLIGHT_HOLOGRAM =
            add(WorldPipelines.waterHighlight("world_block_highlight_hologram", SHADER_BLOCK_HIGHLIGHT_HOLOGRAM_FRAG));
    /** Fullscreen nether portal rift (pos2). */
    public static final RenderPipeline PORTAL_RIFT = add(ScreenPipelines.screen("portal_rift", SHADER_PORTAL_RIFT_FRAG, "Heat", "u_Texture"));
    /** Fullscreen sleep overlay (pos2). */
    public static final RenderPipeline SLEEP_OVERLAY = add(ScreenPipelines.screen("sleep_overlay", SHADER_SLEEP_OVERLAY_FRAG, "PostProcess", "u_Texture"));
    /** Fullscreen hand glass (pos2). */
    public static final RenderPipeline HAND_GLASS = add(ScreenPipelines.handEffect("hand_glass", SHADER_HAND_GLASS_FRAG, "HandGlass"));
    public static final RenderPipeline HAND_SMOKE = add(ScreenPipelines.handEffect("hand_smoke", SHADER_HAND_SMOKE_FRAG, "HandSmoke"));
    public static final RenderPipeline HAND_METALLIC = add(ScreenPipelines.handEffect("hand_metallic", SHADER_HAND_METALLIC_FRAG, "HandMetallic"));
    public static final RenderPipeline HAND_NOISE = add(ScreenPipelines.handEffect("hand_noise", SHADER_HAND_NOISE_FRAG, "HandNoise"));
    public static final RenderPipeline HAND_CHAMS_SOLID = add(ScreenPipelines.handEffect("hand_chams_solid", SHADER_HAND_CHAMS_SOLID_FRAG, "HandChams"));
    public static final RenderPipeline HAND_CHAMS_CHROMA = add(ScreenPipelines.handEffect("hand_chams_chroma", SHADER_HAND_CHAMS_CHROMA_FRAG, "HandChams"));
    public static final RenderPipeline HAND_CHAMS_STRIPES = add(ScreenPipelines.handEffect("hand_chams_stripes", SHADER_HAND_CHAMS_STRIPES_FRAG, "HandChams"));
    public static final RenderPipeline HAND_CHAMS_GLOW = add(ScreenPipelines.handEffect("hand_chams_glow", SHADER_HAND_CHAMS_GLOW_FRAG, "HandChams"));
    public static final RenderPipeline HAND_CHAMS_SNOW = add(ScreenPipelines.handEffect("hand_chams_snow", SHADER_HAND_CHAMS_SNOW_FRAG, "HandChams"));
    public static final RenderPipeline HAND_CHAMS_BALATRO = add(ScreenPipelines.handEffect("hand_chams_balatro", SHADER_HAND_CHAMS_BALATRO_FRAG, "HandChams"));
    public static final RenderPipeline HAND_CHAMS_BLEND = add(ScreenPipelines.handBlendEffect("hand_chams_blend", SHADER_HAND_CHAMS_BLEND_FRAG));
    /** No depth test; translucent; textured skybox with vanilla sky/fog blending. */
    public static final RenderPipeline WORLD_SKYBOX_TEXTURED_FOG = add(WorldPipelines.texturedSkyboxFog("world_skybox_textured_fog", SHADER_POS_TEX_COLOR_SKY_FOG_FRAG, BlendFunction.TRANSLUCENT));
    /** No depth test; additive; textured skybox with vanilla sky/fog blending. */
    public static final RenderPipeline WORLD_SKYBOX_TEXTURED_FOG_ADDITIVE = add(WorldPipelines.texturedSkyboxFog("world_skybox_textured_fog_additive", SHADER_POS_TEX_COLOR_SKY_FOG_ADDITIVE_FRAG, PipelineTemplates.ADDITIVE));
    /** Fullscreen procedural Reimagined skybox with sky/fog blending. */
    public static final RenderPipeline WORLD_REIMAGINED_SKYBOX_SHADER = add(WorldPipelines.proceduralSkyboxFog("world_reimagined_skybox_shader", SHADER_REIMAGINED_SKYBOX_FRAG));
    /** Fullscreen procedural ShaderSky (Thunder) with sky/fog blending. */
    public static final RenderPipeline WORLD_SHADER_SKY_THUNDER = add(ScreenPipelines.screen("world_shader_sky_thunder", SHADER_SHADER_SKY_THUNDER_FRAG, "ShaderSky"));
    /** Fullscreen procedural ShaderSky (Pulsar) with sky/fog blending. */
    public static final RenderPipeline WORLD_SHADER_SKY_PULSAR = add(ScreenPipelines.screen("world_shader_sky_pulsar", SHADER_SHADER_SKY_PULSAR_FRAG, "ShaderSky"));

    // ======================
    // Batched UI shapes (2D)
    // ======================
    public static final RenderPipeline UI_CIRCLE_BATCH = add(UiPipelines.batch("ui_circle_batch",
            LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS, SHADER_POS_LOCAL_COLOR_RECT_PARAMS_VERT, SHADER_CIRCLE_BATCH_FRAG, BlendFunction.TRANSLUCENT, true));
    public static final RenderPipeline UI_ARC_BATCH = add(UiPipelines.batch("ui_arc_batch",
            LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS2, SHADER_POS_LOCAL_COLOR_RECT_PARAMS2_VERT, SHADER_ARC_BATCH_FRAG, BlendFunction.TRANSLUCENT, true));
    public static final RenderPipeline UI_ORBIZ_RING_BATCH = add(UiPipelines.batch("ui_orbiz_ring_batch",
            LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS5, SHADER_POS_LOCAL_COLOR_RECT_PARAMS5_GENERIC_VERT, SHADER_ORBIZ_RING_BATCH_FRAG, BlendFunction.TRANSLUCENT, true));
    public static final RenderPipeline UI_ROUNDED_BATCH = add(UiPipelines.batch("ui_rounded_batch",
            LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS, SHADER_POS_LOCAL_COLOR_RECT_PARAMS_VERT, SHADER_ROUNDED_RECT_BATCH_FRAG, BlendFunction.TRANSLUCENT, true));
    public static final RenderPipeline UI_ROUNDED_FILL_SMOKE_BATCH = add(UiPipelines.batch("ui_rounded_fill_smoke_batch",
            LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS5, SHADER_POS_LOCAL_COLOR_RECT_PARAMS5_VERT, SHADER_UI_ROUNDED_FILL_SMOKE_BATCH_FRAG, BlendFunction.TRANSLUCENT, true));
    public static final RenderPipeline UI_ROUNDED_STROKE_BATCH = add(UiPipelines.batch("ui_rounded_stroke_batch",
            LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS, SHADER_POS_LOCAL_COLOR_RECT_PARAMS_VERT, SHADER_ROUNDED_RECT_STROKE_BATCH_FRAG, BlendFunction.TRANSLUCENT, true));
    public static final RenderPipeline UI_ROUNDED_STROKE_ANGULAR_BATCH = add(UiPipelines.batch("ui_rounded_stroke_angular_batch",
            LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS2, SHADER_POS_LOCAL_COLOR_RECT_PARAMS2_VERT, SHADER_ROUNDED_RECT_STROKE_ANGULAR_BATCH_FRAG, BlendFunction.TRANSLUCENT, true));
    public static final RenderPipeline UI_CHAMFERED_BATCH = add(UiPipelines.batch("ui_chamfered_batch",
            LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS2, SHADER_POS_LOCAL_COLOR_RECT_PARAMS2_VERT, SHADER_CHAMFERED_RECT_BATCH_FRAG, BlendFunction.TRANSLUCENT, true));
    public static final RenderPipeline UI_SHAPE_BATCH = add(UiPipelines.batch("ui_shape_batch",
            LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS5, SHADER_POS_LOCAL_COLOR_RECT_PARAMS5_VERT, SHADER_UI_SHAPE_BATCH_FRAG, BlendFunction.TRANSLUCENT, true));
    public static final RenderPipeline UI_CHAMFERED_STROKE_BATCH = add(UiPipelines.batch("ui_chamfered_stroke_batch",
            LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS2, SHADER_POS_LOCAL_COLOR_RECT_PARAMS2_VERT, SHADER_CHAMFERED_RECT_STROKE_BATCH_FRAG, BlendFunction.TRANSLUCENT, true));
    public static final RenderPipeline UI_ROUNDED_STROKE_CORNERS_BATCH = add(UiPipelines.batch("ui_rounded_stroke_corners_batch",
            LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS2, SHADER_POS_LOCAL_COLOR_RECT_PARAMS2_VERT, SHADER_ROUNDED_RECT_STROKE_CORNERS_BATCH_FRAG, BlendFunction.TRANSLUCENT, true));
    public static final RenderPipeline UI_ROUNDED_CORNERS_BATCH = add(UiPipelines.batch("ui_rounded_corners_batch",
            LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS2, SHADER_POS_LOCAL_COLOR_RECT_PARAMS2_VERT, SHADER_ROUNDED_RECT_CORNERS_BATCH_FRAG, BlendFunction.TRANSLUCENT, true));
    public static final RenderPipeline UI_ROUNDED_GLOW_BATCH = add(UiPipelines.batch("ui_rounded_glow_batch",
            LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS, SHADER_POS_LOCAL_COLOR_RECT_PARAMS_VERT, SHADER_ROUNDED_RECT_GLOW_BATCH_FRAG, PipelineTemplates.ADDITIVE, true));
    public static final RenderPipeline UI_ROUNDED_SHADOW_BATCH = add(UiPipelines.batch("ui_rounded_shadow_batch",
            LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS, SHADER_POS_LOCAL_COLOR_RECT_PARAMS_VERT, SHADER_ROUNDED_RECT_SHADOW_BATCH_FRAG, BlendFunction.TRANSLUCENT, true));
    public static final RenderPipeline UI_ROUNDED_SOFT_SHADOW_BATCH = add(UiPipelines.batch("ui_rounded_soft_shadow_batch",
            LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS, SHADER_POS_LOCAL_COLOR_RECT_PARAMS_VERT, SHADER_ROUNDED_RECT_SOFT_SHADOW_BATCH_FRAG, BlendFunction.TRANSLUCENT, true));
    public static final RenderPipeline UI_RADIAL_GLOW_BATCH = add(UiPipelines.batch("ui_radial_glow_batch",
            LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS2, SHADER_POS_LOCAL_COLOR_RECT_PARAMS2_VERT, SHADER_RADIAL_GLOW_BATCH_FRAG, PipelineTemplates.ADDITIVE, true));
    public static final RenderPipeline UI_ROUNDED_TEXTURED_BATCH = add(UiPipelines.batch("ui_rounded_textured_batch",
            LoVisualVertexFormats.POS2_TEXTURE_LOCAL_COLOR_RECT_PARAMS, SHADER_POS_TEX_LOCAL_COLOR_RECT_PARAMS_VERT, SHADER_ROUNDED_RECT_TEX_BATCH_FRAG, BlendFunction.TRANSLUCENT, true, "u_Texture"));
    public static final RenderPipeline UI_ROUNDED_TEXTURED_MASK_BATCH = add(UiPipelines.batch("ui_rounded_textured_mask_batch",
            LoVisualVertexFormats.POS2_TEXTURE_LOCAL_COLOR_RECT_PARAMS, SHADER_POS_TEX_LOCAL_COLOR_RECT_PARAMS_VERT, SHADER_ROUNDED_RECT_TEX_MASK_BATCH_FRAG, BlendFunction.TRANSLUCENT, true, "u_Texture"));
    public static final RenderPipeline UI_BLUR_BATCH = add(UiPipelines.batch("ui_blur_batch",
            LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS, SHADER_POS_LOCAL_COLOR_RECT_PARAMS_VERT, SHADER_UI_BLUR_BATCH_FRAG, BlendFunction.TRANSLUCENT, false, "u_Texture"));
    public static final RenderPipeline UI_BLUR_BATCH_CORNERS = add(UiPipelines.batch("ui_blur_batch_corners",
            LoVisualVertexFormats.POS2_LOCAL_COLOR_RECT_PARAMS2, SHADER_POS_LOCAL_COLOR_RECT_PARAMS2_VERT, SHADER_UI_BLUR_BATCH_CORNERS_FRAG, BlendFunction.TRANSLUCENT, false, "u_Texture"));
    public static final RenderPipeline UI_LIQUID_GLASS_BATCH = add(UiPipelines.batch("ui_liquid_glass_batch",
            LoVisualVertexFormats.POS2_TEXTURE_LOCAL_COLOR_RECT_PARAMS2, SHADER_POS_TEX_LOCAL_COLOR_RECT_PARAMS2_VERT, SHADER_UI_LIQUID_GLASS_BATCH_FRAG, BlendFunction.TRANSLUCENT, false, "u_Texture", "u_BlurTexture"));

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
        return PipelineTemplates.MESH_UNIFORMS;
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

    /**
     * LoVisual shader asset identifier for "shaders/&lt;name&gt;".
     */
    private static Identifier shader(String name) {
        return Identifier.fromNamespaceAndPath("lovisual", "shaders/" + name);
    }

}
