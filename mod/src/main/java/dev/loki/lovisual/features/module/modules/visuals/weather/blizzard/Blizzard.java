package dev.loki.lovisual.features.module.modules.visuals.weather.blizzard;

import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.features.module.modules.visuals.weather.blizzard.view.BlizzardFog;
import dev.loki.lovisual.features.module.modules.visuals.weather.blizzard.view.BlizzardOverlay;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;

/**
 * Метель: снежинки со сносом в мире, белая дымка и обмерзание краёв экрана.
 * Фасад: симуляция в {@link SnowFlakes}, 3D-рендер в {@link BlizzardRenderer},
 * экранное покрытие в {@link BlizzardOverlay}.
 */
@ModuleInfo(
        id = "blizzard",
        displayName = "Blizzard",
        category = ModuleCategory.VISUALS,
        description = "High-altitude snowstorm with wind-driven flakes, whiteout fog and frosted edges"
)
public final class Blizzard extends Module {

    private final NumberValue<Float> intensity = num("blizzardIntensity", "intensity", 0.85f, 0.1f, 1.0f);
    private final NumberValue<Float> density = num("blizzardDensity", "density", 1.0f, 0.2f, 3.0f);
    private final NumberValue<Float> wind = num("blizzardWind", "wind", 0.7f, 0.0f, 1.0f);
    private final NumberValue<Float> turbulence = num("blizzardTurbulence", "turbulence", 0.6f, 0.0f, 1.0f);
    private final NumberValue<Float> flakeSize = num("blizzardFlakeSize", "flake_size", 1.0f, 0.5f, 2.5f);
    private final NumberValue<Float> radius = num("blizzardRadius", "radius", 12.0f, 6.0f, 24.0f);
    private final NumberValue<Float> opacity = num("blizzardOpacity", "opacity", 0.8f, 0.1f, 1.0f);
    private final BooleanValue whiteout = bool("blizzardWhiteout", "whiteout", true);
    private final NumberValue<Float> visibility =
            visibleWhen(num("blizzardVisibility", "visibility", 28.0f, 8.0f, 128.0f), whiteout::get);
    private final BooleanValue frost = bool("blizzardFrost", "frost", true);
    private final BooleanValue onlyWhenSnowing = bool("blizzardOnlyWhenSnowing", "only_when_snowing", false);
    private final BooleanValue hideVanilla = bool("blizzardHideVanilla", "hide_vanilla", true);
    private final RGBAColorValue tint = color("blizzardTint", "tint", "#FFFFFFFF");

    private final SnowFlakes flakes = new SnowFlakes();
    private long lastFrameNanos;
    private float strength;
    private float exposure;
    private boolean frameActive;

    @Override
    public HudPhase getHudPhase() {
        return HudPhase.AFTER_SUBTITLES;
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.END_MAIN;
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    private void reset() {
        lastFrameNanos = 0L;
        strength = 0.0f;
        exposure = 0.0f;
        frameActive = false;
        flakes.clear();
    }

    /** Ванильные полосы дождя и снега скрыты, пока идёт шторм и включена настройка. */
    public boolean hidesVanillaWeather() {
        return isEnabled() && frameActive && hideVanilla.get();
    }

    /** Штормовая пелена: сжимает туман и красит его в цвет снега (вызывается из миксина тумана). */
    public void applyFog(Camera camera, FogData fog) {
        if (!isEnabled() || !whiteout.get() || camera == null || fog == null) return;
        FogType type = camera.getFluidInCamera();
        if (type != FogType.NONE && type != FogType.ATMOSPHERIC) return;
        float f = strength * exposure;
        if (!BlizzardFog.active(f)) return;
        float end = BlizzardFog.endDistance(visibility.get(), f);
        float start = BlizzardFog.startDistance(visibility.get(), f);
        fog.environmentalStart = BlizzardFog.limit(fog.environmentalStart, start);
        fog.environmentalEnd = BlizzardFog.limit(fog.environmentalEnd, end);
        fog.renderDistanceStart = BlizzardFog.limit(fog.renderDistanceStart, start);
        fog.renderDistanceEnd = BlizzardFog.limit(fog.renderDistanceEnd, end);
        fog.skyEnd = BlizzardFog.limit(fog.skyEnd, end * BlizzardFog.SKY_SHARE);
        fog.cloudEnd = BlizzardFog.limit(fog.cloudEnd, end);
        int argb = tint.getArgb();
        float k = BlizzardFog.brightness(fog.color.x, fog.color.y, fog.color.z);
        fog.color.x = BlizzardFog.blend(fog.color.x, ((argb >> 16) & 0xFF) / 255.0f, k, f);
        fog.color.y = BlizzardFog.blend(fog.color.y, ((argb >> 8) & 0xFF) / 255.0f, k, f);
        fog.color.z = BlizzardFog.blend(fog.color.z, (argb & 0xFF) / 255.0f, k, f);
    }

    private float frameDeltaSeconds() {
        long now = System.nanoTime();
        if (lastFrameNanos == 0L) lastFrameNanos = now;
        float dt = (float) ((now - lastFrameNanos) / 1_000_000_000.0);
        lastFrameNanos = now;
        return Mth.clamp(dt, 0.0f, 0.1f);
    }

    private float targetStrength(Minecraft mc) {
        float rain = mc.level.getRainLevel(1.0f);
        BlockPos pos = mc.player.blockPosition();
        boolean snowBiome = mc.level.getBiome(pos).value()
                .getPrecipitationAt(pos, mc.level.getSeaLevel()) == Biome.Precipitation.SNOW;
        return SnowFlakes.targetStrength(intensity.get(), onlyWhenSnowing.get(), rain, snowBiome);
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null || mc.level == null || mc.gameRenderer == null
                || mc.gameRenderer.mainCamera() == null) {
            frameActive = false;
            return;
        }
        float dt = frameDeltaSeconds();
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        ClientLevel level = mc.level;
        SnowFlakes.Ground ground = (x, z) -> level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        strength = SnowFlakes.ramp(strength, targetStrength(mc), dt);
        float open = SnowFlakes.exposureTarget(cam.y, ground.height(Mth.floor(cam.x), Mth.floor(cam.z)));
        exposure = SnowFlakes.approach(exposure, open, SnowFlakes.EXPOSURE_PER_SECOND, dt);
        frameActive = strength > 0.01f;
        if (!frameActive && flakes.count == 0) return;

        double seconds = (System.currentTimeMillis() % 600_000L) / 1000.0;
        SnowFlakes.Wind windValue = SnowFlakes.wind(seconds, wind.get(), strength);
        int target = frameActive ? SnowFlakes.targetCount(radius.get(), density.get(), strength) : 0;
        flakes.update(dt, cam.x, cam.y, cam.z, radius.get(), windValue, SnowFlakes.turbulence(turbulence.get()),
                target, seconds, ground);
        if (flakes.count == 0) return;
        MeshBuilder mesh = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED_DEPTH, Renderer3D.DepthMode.MAIN);
        if (mesh == null) return;
        BlizzardRenderer.render(mesh, flakes, RenderState.cameraRotation, cam.x, cam.y, cam.z,
                tint.getArgb(), strength, intensity.get(), flakeSize.get(), opacity.get());
    }

    @Override
    public void onRenderHudEngine(Renderer2D renderer, TextRenderer textRenderer,
                                  GuiGraphicsExtractor ctx, float tickDelta) {
        if (!isEnabled()) return;
        float current = strength * exposure * intensity.get();
        BlizzardOverlay.render(ctx, current, frost.get(), tint.getArgb());
    }
}
