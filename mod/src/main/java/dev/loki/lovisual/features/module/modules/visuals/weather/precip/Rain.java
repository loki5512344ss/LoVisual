package dev.loki.lovisual.features.module.modules.visuals.weather.precip;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessContext;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessManager;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessPass;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Фасад: делегирует физику/партиклы/рендер в хелперы.
 */
@ModuleInfo(id = "rain", displayName = "Rain", category = ModuleCategory.VISUALS,
        description = "Реалистичный объёмный дождь с брызгами, туманом и молниями")
public final class Rain extends Module implements PostProcessPass {
    static final double GRAVITY = 22.0;
    static final double TERMINAL_VELOCITY = 34.0;
    static final int RIPPLE_SEGMENTS = 14;
    static final int MAX_DROPS = 4200;
    static final int MAX_MIST = 90;

    final Matrix4f wetSurfaceProjection = new Matrix4f();
    final Matrix4f wetSurfaceInvView = new Matrix4f();
    boolean wetSurfaceDepthSupported = true;
    { PostProcessManager.register(this); }

    final EnumValue<Preset> preset = enumMode("preset", Preset.RAIN, Preset.values());
    final NumberValue<Float> density = num("density", 1.0f, 0.1f, 3.0f);
    final NumberValue<Float> radius = num("radius", 26.0f, 8.0f, 64.0f);
    final NumberValue<Float> altitude = num("altitude", 16.0f, 6.0f, 40.0f);
    final NumberValue<Float> dropSize = num("drop_size", 1.0f, 0.3f, 2.5f);
    final NumberValue<Float> fallSpeed = num("fall_speed", 1.0f, 0.3f, 2.5f);
    final NumberValue<Float> wind = num("wind", 1.0f, 0.0f, 3.0f);
    final NumberValue<Float> opacity = num("opacity", 1.0f, 0.1f, 2.0f);
    final BooleanValue splashes = bool("splashes", true);
    final BooleanValue droplets = visibleWhen(bool("splash_droplets", true), splashes::get);
    final BooleanValue groundMist = bool("ground_mist", true);
    final BooleanValue lightning = bool("lightning", false);
    final BooleanValue skyCheck = bool("sky_check", true);
    final BooleanValue onlyWhenRaining = bool("only_when_raining", false);
    final EnumValue<ColorMode> colorMode = enumMode("color_mode", ColorMode.REALISTIC, ColorMode.values());
    final RGBAColorValue customColor = visibleWhen(color("color", "#FFBFD8E6"), () -> colorMode.get() == ColorMode.CUSTOM);
    final BooleanValue wetGround = bool("wet_ground", true);
    final NumberValue<Float> wetness = visibleWhen(num("wetness", 1.25f, 0.1f, 2.0f), wetGround::get);
    final NumberValue<Float> puddleCoverage = visibleWhen(num("puddle_coverage", 0.90f, 0.05f, 1.0f), wetGround::get);
    final NumberValue<Float> reflectionStrength = visibleWhen(num("reflection_strength", 1.55f, 0.1f, 2.0f), wetGround::get);
    final NumberValue<Float> reflectionDistance = visibleWhen(num("reflection_distance", 56.0f, 12.0f, 96.0f), wetGround::get);
    final EnumValue<ReflectionQuality> reflectionQuality = visibleWhen(enumMode("reflection_quality", ReflectionQuality.BALANCED, ReflectionQuality.values()), wetGround::get);
    final BooleanValue puddleRipples = visibleWhen(bool("puddle_ripples", true), wetGround::get);
    final NumberValue<Float> rippleStrength = visibleWhen(num("ripple_strength", 0.8f, 0.0f, 1.6f), () -> wetGround.get() && puddleRipples.get());

    final List<RainParticles.Drop> drops = new ArrayList<>();
    final List<RainParticles.Splash> splashList = new ArrayList<>();
    final List<RainParticles.Droplet> dropletList = new ArrayList<>();
    final List<RainParticles.Mist> mistList = new ArrayList<>();
    final Map<Long, Integer> groundCache = new HashMap<>();
    final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
    final Random random = new Random();
    long lastFrameNanos; float clock; float flash; float nextFlashIn = 9.0f; int cacheTicks;

    @Override public void onEnable() { lastFrameNanos = 0L; wetSurfaceDepthSupported = true; clearAll(); }
    @Override public void onDisable() { clearAll(); }
    void clearAll() { drops.clear(); splashList.clear(); dropletList.clear(); mistList.clear(); groundCache.clear(); flash = 0.0f; }

    @Override public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { clearAll(); return; }
        if (++cacheTicks > 30) { cacheTicks = 0; groundCache.clear(); }
    }

    float random(float min, float max) { return min + random.nextFloat() * (max - min); }

    int resolveColorArgb() {
        return switch (colorMode.get()) {
            case CUSTOM -> customColor.getArgb();
            case THEME -> Theme.theme().accent();
            case REALISTIC -> 0xFFC8DCE8;
        };
    }

    float ambientLight(Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        try { int light = mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, camera.blockPosition()); return Mth.clamp(light / 15.0f, 0.3f, 1.0f); }
        catch (Throwable ignored) { return 1.0f; }
    }

    @Override public boolean isActive() { return RainWetSurface.isActiveWet(this); }
    @Override public int getPriority() { return 2; }
    @Override public Phase getPhase() { return Phase.PRE_HAND; }
    @Override public boolean render(GpuTextureView src, GpuTextureView dst, float tickDelta) { return false; }
    @Override public boolean render(PostProcessContext ctx, GpuTextureView src, GpuTextureView dst) { return RainWetSurface.renderWet(this, ctx, src, dst); }
    @Override public WorldPhase getWorldPhase() { return WorldPhase.AFTER_POST_PROCESS; }

    @Override public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null || mc.level == null || mc.gameRenderer == null) return;
        if (mc.gameRenderer.mainCamera() == null) return;
        if (onlyWhenRaining.get() && !mc.level.isRaining()) { clearAll(); return; }
        Camera camera = mc.gameRenderer.mainCamera();
        long now = System.nanoTime(); if (lastFrameNanos == 0L) lastFrameNanos = now;
        float dt = (float) ((now - lastFrameNanos) / 1_000_000_000.0); lastFrameNanos = now;
        dt = Mth.clamp(dt, 0.0f, 0.05f); clock += dt;
        Vec3 cam = camera.position();
        RainPhysics.updateDrops(this, cam, dt);
        RainParticles.updateSplashes(this, dt);
        RainParticles.updateDroplets(this, dt);
        RainParticles.updateMist(this, cam, dt);
        RainParticles.updateLightning(this, dt);
        if (drops.isEmpty() && splashList.isEmpty() && dropletList.isEmpty() && mistList.isEmpty()) return;
        int colorArgb = resolveColorArgb();
        float r = ((colorArgb >>> 16) & 0xFF) / 255f; float g = ((colorArgb >>> 8) & 0xFF) / 255f; float b = (colorArgb & 0xFF) / 255f;
        float[] rgb = {r, g, b};
        float ambient = ambientLight(camera);
        float brightnessMul = Mth.clamp(ambient + flash * 0.9f, 0.0f, 1.65f);
        float alphaMul = opacity.get() * brightnessMul;
        var camRot = RenderState.cameraRotation; float camYaw = camera.yRot();
        MeshBuilder blendMesh = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED, Renderer3D.DepthMode.MAIN);
        if (blendMesh != null) {
            if (groundMist.get()) RainRenderer.drawMist(this, blendMesh, camRot, rgb, alphaMul);
            RainRenderer.drawStreaks(this, blendMesh, camYaw, rgb, alphaMul, false);
            if (splashes.get()) RainRenderer.drawRipples(this, blendMesh, rgb, alphaMul);
        }
        MeshBuilder additiveMesh = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED_ADDITIVE_DEPTH, Renderer3D.DepthMode.PRE_DEPTH);
        if (additiveMesh != null) {
            RainRenderer.drawStreaks(this, additiveMesh, camYaw, rgb, alphaMul, true);
            if (splashes.get()) {
                RainRenderer.drawCrowns(this, additiveMesh, camYaw, rgb, alphaMul);
                if (droplets.get()) RainRenderer.drawDroplets(this, additiveMesh, camYaw, rgb, alphaMul);
            }
        }
    }

    public enum Preset implements EnumValue.IdProvider { DRIZZLE("drizzle"), RAIN("rain"), DOWNPOUR("downpour"), STORM("storm"); private final String id; Preset(String id){this.id=id;} @Override public String id(){return id;}}
    public enum ColorMode implements EnumValue.IdProvider { REALISTIC("realistic"), THEME("theme"), CUSTOM("custom"); private final String id; ColorMode(String id){this.id=id;} @Override public String id(){return id;}}
    public enum ReflectionQuality implements EnumValue.IdProvider { PERFORMANCE("performance"), BALANCED("balanced"), HIGH("high"); private final String id; ReflectionQuality(String id){this.id=id;} @Override public String id(){return id;}}
}
