package dev.loki.lovisual.features.module.modules.visuals.weather.blizzard;

import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
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
        description = "Wind-driven snowstorm with white haze and frosted screen edges"
)
public final class Blizzard extends Module {

    private final NumberValue<Float> intensity = num("blizzardIntensity", "intensity", 0.85f, 0.1f, 1.0f);
    private final NumberValue<Float> density = num("blizzardDensity", "density", 1.0f, 0.2f, 3.0f);
    private final NumberValue<Float> speed = num("blizzardSpeed", "speed", 1.0f, 0.3f, 3.0f);
    private final NumberValue<Float> wind = num("blizzardWind", "wind", 0.7f, 0.0f, 2.0f);
    private final NumberValue<Float> radius = num("blizzardRadius", "radius", 14.0f, 6.0f, 32.0f);
    private final NumberValue<Float> flakeSize = num("blizzardFlakeSize", "flake_size", 1.0f, 0.3f, 2.5f);
    private final NumberValue<Float> opacity = num("blizzardOpacity", "opacity", 0.7f, 0.05f, 1.0f);
    private final RGBAColorValue tint = color("blizzardTint", "tint", "#F2FFFFFF");
    private final NumberValue<Float> haze = num("blizzardHaze", "haze", 0.5f, 0.0f, 1.0f);
    private final BooleanValue frost = bool("blizzardFrost", "frost", true);
    private final NumberValue<Float> frostIntensity =
            visibleWhen(num("blizzardFrostIntensity", "frost_intensity", 0.6f, 0.0f, 1.0f), frost::get);
    private final BooleanValue onlyWhenSnowing = bool("blizzardOnlyWhenSnowing", "only_when_snowing", false);

    private final SnowFlakes flakes = new SnowFlakes();
    private long lastFrameNanos;
    private float seconds;
    private float strength;

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
        lastFrameNanos = 0L;
        seconds = 0.0f;
        strength = 0.0f;
        flakes.clear();
    }

    @Override
    public void onDisable() {
        flakes.clear();
    }

    private float frameDeltaSeconds() {
        long now = System.nanoTime();
        if (lastFrameNanos == 0L) lastFrameNanos = now;
        float dt = (float) ((now - lastFrameNanos) / 1_000_000_000.0);
        lastFrameNanos = now;
        return Mth.clamp(dt, 0.0f, 0.05f);
    }

    private float targetStrength() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return 0.0f;
        if (!onlyWhenSnowing.get()) return intensity.get();
        if (mc.level.getRainLevel(1.0f) < 0.02f) return 0.0f;
        BlockPos pos = mc.player.blockPosition();
        Biome.Precipitation precipitation =
                mc.level.getBiome(pos).value().getPrecipitationAt(pos, mc.level.getSeaLevel());
        return precipitation == Biome.Precipitation.SNOW ? intensity.get() : 0.0f;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null || mc.level == null || mc.gameRenderer == null) return;
        if (mc.gameRenderer.mainCamera() == null) return;
        float dt = frameDeltaSeconds();
        seconds += dt;
        strength = SnowFlakes.ramp(strength, targetStrength(), dt);
        float norm = strength;
        if (norm <= 0.001f) {
            flakes.clear();
            return;
        }
        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 cam = camera.position();
        SnowFlakes.Wind windValue = SnowFlakes.wind(seconds, wind.get(), norm);
        int target = SnowFlakes.targetCount(radius.get(), density.get(), norm);
        flakes.update(dt, cam.x, cam.y, cam.z, radius.get(), windValue,
                SnowFlakes.turbulence(wind.get()), speed.get(), target, seconds);
        if (flakes.count == 0) return;
        MeshBuilder mesh = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED_DEPTH, Renderer3D.DepthMode.MAIN);
        if (mesh == null) return;
        BlizzardRenderer.render(mesh, flakes, RenderState.cameraRotation,
                cam.x, cam.y, cam.z, radius.get(), tint.getArgb(), norm, flakeSize.get(), opacity.get());
    }

    @Override
    public void onRenderHudEngine(Renderer2D renderer, TextRenderer textRenderer,
                                  GuiGraphicsExtractor ctx, float tickDelta) {
        if (!isEnabled()) return;
        float norm = strength;
        if (norm <= 0.01f) return;
        BlizzardOverlay.render(ctx, norm, haze.get(), frost.get(), frostIntensity.get(), tint.getArgb());
    }
}
