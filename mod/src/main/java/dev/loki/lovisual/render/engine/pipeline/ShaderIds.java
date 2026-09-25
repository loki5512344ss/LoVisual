package dev.loki.lovisual.render.engine.pipeline;

import net.minecraft.resources.Identifier;

/**
 * Shader asset identifiers for {@code assets/lovisual/shaders/*}.
 *
 * <p>Split out of {@link LoVisualRenderPipelines} to keep the registry small;
 * the registry references them through a static import.</p>
 */
public final class ShaderIds {
    private ShaderIds() {
    }

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
    public static final Identifier SHADER_HAND_CHAMS_INVERT_FRAG = shader("hand_chams_invert.frag");
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
    // SkyShader starry (procedural stars + milky way + round moon)
    public static final Identifier SHADER_SKY_SHADER_STARRY_FRAG = shader("sky_shader_starry.frag");
    /**
     * LoVisual shader asset identifier for "shaders/<name>".
     */
    private static Identifier shader(String name) {
        return Identifier.fromNamespaceAndPath("lovisual", "shaders/" + name);
    }
}
