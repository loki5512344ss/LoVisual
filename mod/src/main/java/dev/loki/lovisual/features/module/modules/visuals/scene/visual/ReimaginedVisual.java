package dev.loki.lovisual.features.module.modules.visuals.scene.visual;

import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.postprocess.blur.DepthOfFieldDepthSource;
import dev.loki.lovisual.render.engine.postprocess.blur.DepthOfFieldQuality;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessManager;
import dev.loki.lovisual.render.iris.runtime.IrisRuntime;
import net.minecraft.client.resources.language.I18n;
import java.util.LinkedHashMap;
import java.util.Map;

@ModuleInfo(id = "reimaginedvisual", displayName = "ReimaginedVisual", category = ModuleCategory.VISUALS)
public class ReimaginedVisual extends Module {
    private final BooleanMapValue effects;
    private final ReimaginedSkyboxHelper skybox;
    private final ReimaginedSunHelper sun;
    private final ReimaginedVegetationHelper vegetation;
    private final ReimaginedDepthOfFieldHelper depthOfField;
    public ReimaginedVisual() {
        Map<String, Boolean> defEffects = createDefaultEffects();
        Map<String, Boolean> defLayers = createDefaultSkyboxLayers();
        BooleanMapValue eff = group("reimaginedVisualEffects", "effects", defEffects);
        this.effects = eff;
        BooleanValue syncTheme = decorateSkybox(visibleWhen(bool("reimaginedVisualSkyboxShaderSyncTheme", "skybox_shader_sync_theme", true), () -> eff.get("shader_sky")), eff);
        RGBColorValue color = decorateSkybox(visibleWhen(colorNoAlpha("reimaginedVisualSkyboxShaderColor", "skybox_shader_color", "#78A7FF"), () -> eff.get("shader_sky") && !syncTheme.get()), eff);
        NumberValue<Float> motionSpeed = decorateSkybox(visibleWhen(num("reimaginedVisualSkyboxShaderMotionSpeed", "skybox_shader_motion_speed", 0.45f, 0.0f, 2.0f), () -> eff.get("shader_sky")), eff);
        BooleanValue auroraEnabled = decorateSkybox(visibleWhen(bool("reimaginedVisualSkyboxShaderAuroraEnabled", "skybox_shader_aurora_enabled", false), () -> eff.get("shader_sky")), eff);
        NumberValue<Float> auroraIntensity = decorateSkybox(visibleWhen(num("reimaginedVisualSkyboxShaderAuroraIntensity", "skybox_shader_aurora_intensity", 0.70f, 0.0f, 1.5f), () -> eff.get("shader_sky") && auroraEnabled.get()), eff);
        NumberValue<Float> auroraSpeed = decorateSkybox(visibleWhen(num("reimaginedVisualSkyboxShaderAuroraSpeed", "skybox_shader_aurora_speed", 0.38f, 0.0f, 2.0f), () -> eff.get("shader_sky") && auroraEnabled.get()), eff);
        NumberValue<Float> smallStars = decorateSkybox(visibleWhen(num("reimaginedVisualSkyboxShaderSmallStars", "skybox_shader_small_stars", 1.60f, 0.0f, 10.0f), () -> eff.get("shader_sky")), eff);
        NumberValue<Float> dustStars = decorateSkybox(visibleWhen(num("reimaginedVisualSkyboxShaderDustStars", "skybox_shader_dust_stars", 1.40f, 0.0f, 10.0f), () -> eff.get("shader_sky")), eff);
        NumberValue<Float> mediumStars = decorateSkybox(visibleWhen(num("reimaginedVisualSkyboxShaderMediumStars", "skybox_shader_medium_stars", 1.30f, 0.0f, 10.0f), () -> eff.get("shader_sky")), eff);
        NumberValue<Float> largeStars = decorateSkybox(visibleWhen(num("reimaginedVisualSkyboxShaderLargeStars", "skybox_shader_large_stars", 1.15f, 0.0f, 10.0f), () -> eff.get("shader_sky")), eff);
        NumberValue<Float> starBrightness = decorateSkybox(visibleWhen(num("reimaginedVisualSkyboxShaderStarBrightness", "skybox_shader_star_brightness", 1.35f, 0.0f, 3.0f), () -> eff.get("shader_sky")), eff);
        NumberValue<Float> twinkle = decorateSkybox(visibleWhen(num("reimaginedVisualSkyboxShaderTwinkleStrength", "skybox_shader_twinkle_strength", 1.35f, 0.0f, 3.0f), () -> eff.get("shader_sky")), eff);
        BooleanMapValue layers = decorateSkybox(visibleWhen(group("reimaginedVisualSkyboxShaderLayers", "skybox_shader_layers", defLayers), () -> eff.get("shader_sky")), eff);
        NumberValue<Float> fogBlend = decorateSkybox(visibleWhen(num("reimaginedVisualSkyboxSkyFogBlend", "skybox_sky_fog_blend", 0.35f, 0.0f, 1.0f), () -> eff.get("shader_sky")), eff);
        this.skybox = new ReimaginedSkyboxHelper(syncTheme, color, motionSpeed, auroraEnabled, auroraIntensity, auroraSpeed, smallStars, dustStars, mediumStars, largeStars, starBrightness, twinkle, layers, fogBlend, defLayers);
        BooleanValue glare = visibleWhen(bool("reimaginedVisualWorldSunGlareEnabled", "world_sun_glare_enabled", true), () -> eff.get("world_sun"));
        NumberValue<Float> size = visibleWhen(num("reimaginedVisualWorldSunSize", "world_sun_size", 0.06f, 0.01f, 0.20f), () -> eff.get("world_sun"));
        NumberValue<Float> glow = visibleWhen(num("reimaginedVisualWorldSunGlow", "world_sun_glow", 0.18f, 0.02f, 0.40f), () -> eff.get("world_sun"));
        NumberValue<Float> intensity = visibleWhen(num("reimaginedVisualWorldSunIntensity", "world_sun_intensity", 1.0f, 0.0f, 3.0f), () -> eff.get("world_sun"));
        this.sun = new ReimaginedSunHelper(glare, size, glow, intensity);
        NumberValue<Float> rootedH = visibleWhen(num("reimaginedVisualWavyVegetationRootedHorizontalAmplitude", "wavy_vegetation_rooted_horizontal_amplitude", 1.0f, 0.0f, 3.0f), () -> eff.get("wavy_vegetation"));
        NumberValue<Float> rootedV = visibleWhen(num("reimaginedVisualWavyVegetationRootedVerticalAmplitude", "wavy_vegetation_rooted_vertical_amplitude", 1.0f, 0.0f, 3.0f), () -> eff.get("wavy_vegetation"));
        NumberValue<Float> freeH = visibleWhen(num("reimaginedVisualWavyVegetationFreeHorizontalAmplitude", "wavy_vegetation_free_horizontal_amplitude", 1.0f, 0.0f, 3.0f), () -> eff.get("wavy_vegetation"));
        NumberValue<Float> freeV = visibleWhen(num("reimaginedVisualWavyVegetationFreeVerticalAmplitude", "wavy_vegetation_free_vertical_amplitude", 1.0f, 0.0f, 3.0f), () -> eff.get("wavy_vegetation"));
        NumberValue<Float> speed = visibleWhen(num("reimaginedVisualWavyVegetationSpeed", "wavy_vegetation_speed", 1.0f, 0.0f, 3.0f), () -> eff.get("wavy_vegetation"));
        this.vegetation = new ReimaginedVegetationHelper(rootedH, rootedV, freeH, freeV, speed);
        EnumValue<DepthOfFieldDepthSource> source = decorateDepth(visibleWhen(enumSetting("reimaginedVisualDofDepthSource", "dof_depth_source", DepthOfFieldDepthSource.WORLD_SCENE, DepthOfFieldDepthSource.values()), () -> eff.get("depth_of_field")), eff);
        NumberValue<Float> farStart = decorateDepth(visibleWhen(num("reimaginedVisualDofFarStart", "dof_far_start", 4.0f, 0.0f, 512.0f), () -> eff.get("depth_of_field")), eff);
        NumberValue<Float> farTrans = decorateDepth(visibleWhen(num("reimaginedVisualDofFarTransition", "dof_far_transition", 24.0f, 1.0f, 1024.0f), () -> eff.get("depth_of_field")), eff);
        NumberValue<Float> strength = decorateDepth(visibleWhen(num("reimaginedVisualDofStrength", "dof_strength", 0.65f, 0.0f, 1.5f), () -> eff.get("depth_of_field")), eff);
        NumberValue<Float> maxRadius = decorateDepth(visibleWhen(num("reimaginedVisualDofMaxRadius", "dof_max_radius", 8.0f, 0.0f, 32.0f), () -> eff.get("depth_of_field")), eff);
        EnumValue<DepthOfFieldQuality> quality = decorateDepth(visibleWhen(enumSetting("reimaginedVisualDofQuality", "dof_quality", DepthOfFieldQuality.MEDIUM, DepthOfFieldQuality.values()), () -> eff.get("depth_of_field")), eff);
        BooleanValue debugCoc = decorateDepth(visibleWhen(bool("reimaginedVisualDofDebugCoc", "dof_debug_coc", false), () -> eff.get("depth_of_field")), eff);
        this.depthOfField = new ReimaginedDepthOfFieldHelper(source, farStart, farTrans, strength, maxRadius, quality, debugCoc);
        PostProcessManager.register(depthOfField);
    }
    private <V extends ConfigValue<?>> V decorateSkybox(V value, BooleanMapValue eff) {
        return notAppliedWhen(visibleWhen(value, () -> eff.get("shader_sky")), () -> eff.get("shader_sky") && IrisRuntime.isModLoaded(), () -> { String t = I18n.get("setting.reimaginedvisual.skybox_shader.iris_blocked"); return "setting.reimaginedvisual.skybox_shader.iris_blocked".equals(t) ? "Iris is loaded; ReimaginedVisual shader skybox is not used." : t; });
    }
    private <V extends ConfigValue<?>> V decorateDepth(V value, BooleanMapValue eff) {
        return notAppliedWhen(visibleWhen(value, () -> eff.get("depth_of_field")), () -> IrisRuntime.isShaderpackRendererActive(), () -> { String t = I18n.get("setting.reimaginedvisual.depth_of_field.iris_blocked"); return "setting.reimaginedvisual.depth_of_field.iris_blocked".equals(t) ? "Iris shaderpack pipeline is active." : t; });
    }
    private Map<String, Boolean> createDefaultEffects() {
        LinkedHashMap<String, Boolean> m = new LinkedHashMap<>();
        m.put("shader_sky", true); m.put("world_sun", true); m.put("wavy_vegetation", false); m.put("depth_of_field", false);
        return m;
    }
    private Map<String, Boolean> createDefaultSkyboxLayers() {
        LinkedHashMap<String, Boolean> m = new LinkedHashMap<>();
        m.put("waves", true); m.put("ribbons", true); m.put("sweeps", true); m.put("veil", true); m.put("nebula", true);
        m.put("detail_curtains", true); m.put("polar_arc", true); m.put("bursts", true); m.put("water_veil", true); m.put("caustics", true);
        m.put("refracted_aurora", true); m.put("northern_aurora", true); m.put("small_stars", true); m.put("dust_stars", true);
        m.put("medium_stars", true); m.put("large_stars", true); m.put("detail_stars", true);
        return m;
    }
    public boolean isEffectSelected(String effect) { return effects.get(effect); }
    public boolean isShaderSkyboxEnabled() { return isEnabled() && isEffectSelected("shader_sky"); }
    public int getShaderSkyboxColor() { return skybox.getShaderSkyboxColor(); }
    public float getSkyboxSkyFogBlend() { return skybox.getSkyboxSkyFogBlend(); }
    public float getSkyboxShaderMotionSpeed() { return skybox.getSkyboxShaderMotionSpeed(); }
    public boolean isSkyboxShaderAuroraEnabled() { return skybox.isSkyboxShaderAuroraEnabled(); }
    public float getSkyboxShaderAuroraIntensity() { return skybox.getSkyboxShaderAuroraIntensity(); }
    public float getSkyboxShaderAuroraSpeed() { return skybox.getSkyboxShaderAuroraSpeed(); }
    public float getSkyboxShaderSmallStars() { return skybox.getSmallStars(); }
    public float getSkyboxShaderDustStars() { return skybox.getDustStars(); }
    public float getSkyboxShaderMediumStars() { return skybox.getMediumStars(); }
    public float getSkyboxShaderLargeStars() { return skybox.getLargeStars(); }
    public float getSkyboxShaderStarBrightness() { return skybox.getStarBrightness(); }
    public float getSkyboxShaderTwinkleStrength() { return skybox.getTwinkleStrength(); }
    public int getSkyboxShaderLayerMask() { return skybox.getLayerMask(); }
    public boolean isWorldSunEnabled() { return isEnabled() && isEffectSelected("world_sun"); }
    public float getWorldSunSize() { return sun.getSize(); }
    public float getWorldSunGlow() { return sun.getGlow(); }
    public float getWorldSunIntensity() { return sun.getIntensity(); }
    public boolean isWorldSunGlareEnabled() { return isEnabled() && isEffectSelected("world_sun") && sun.isGlareEnabled(); }
    public boolean isWavyVegetationEnabled() { return isEnabled() && isEffectSelected("wavy_vegetation"); }
    public int packWavyVegetationSettings() { return vegetation.pack(); }
    public boolean isActive() { return depthOfField.isActive(); }
    @Override public WorldPhase getWorldPhase() { return WorldPhase.END_MAIN; }
    public boolean needsWorldSceneDepthCapture() { return depthOfField.needsWorldSceneDepthCapture(); }
    public boolean needsPreTranslucentDepthCapture() { return depthOfField.needsPreTranslucentDepthCapture(); }
    public boolean needsResolvedMainDepthCapture() { return depthOfField.needsResolvedMainDepthCapture(); }
    @Override public void onTick() { vegetation.onTick(isEnabled(), isEffectSelected("wavy_vegetation")); }
    @Override public void onEnable() { depthOfField.resetSamplerSupport(); vegetation.onEnable(isEffectSelected("wavy_vegetation")); }
    @Override public void onDisable() { vegetation.onDisable(); }
}
