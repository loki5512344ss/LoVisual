package dev.loki.lovisual.features.module.modules.visuals.weather;

import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.postprocess.render.SkyShaderRenderer;
import dev.loki.lovisual.render.engine.postprocess.render.SkyShaderStarryRenderer;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import net.minecraft.client.Minecraft;

//todo Description
@ModuleInfo(id = "shadersky", displayName = "ShaderSky", category = ModuleCategory.VISUALS)
public class ShaderSky extends Module {

    private static final String SETTING_MODE = "mode";
    private static final String SETTING_SPEED = "speed";
    private static final String SETTING_SCALE = "scale";
    private static final String SETTING_INTENSITY = "intensity";
    private static final String SETTING_ALPHA = "alpha";
    private static final String SETTING_THUNDER_INTERVAL = "thunder_interval";
    private static final String SETTING_THUNDER_CHANCE = "thunder_chance";
    private static final String SETTING_THUNDER_GLOW = "thunder_glow";
    private static final String SETTING_NEBULA_INTENSITY = "nebula_intensity";
    private static final String SETTING_NEBULA_COLOR1 = "nebula_color1";
    private static final String SETTING_NEBULA_COLOR2 = "nebula_color2";
    private static final String SETTING_NEBULA_STAR_COLOR = "nebula_star_color";
    private static final String SETTING_STARRY_INTENSITY = "starry_intensity";
    private static final String SETTING_STARRY_DENSITY = "starry_density";
    private static final String SETTING_STARRY_MILKYWAY = "starry_milkyway";
    private static final String SETTING_STARRY_MOON_SIZE = "starry_moon_size";
    private static final String SETTING_STARRY_MOON_COLOR = "starry_moon_color";
    private static final String SETTING_STARRY_STAR_COLOR = "starry_star_color";

    private final ModeValue mode = modeSetting(
            "shaderSkyMode",
            SETTING_MODE,
            "Thunder",
            "Thunder", "Pulsar", "Sky", "Water", "Caustic", "Nebula", "Starry"
    );
    private final NumberValue<Float> nebulaIntensity =
            visibleWhen(num("shaderSkyNebulaIntensity", SETTING_NEBULA_INTENSITY, 1.2f, 0.2f, 3.0f), this::isNebulaMode);
    private final RGBAColorValue nebulaColor1 =
            visibleWhen(color("shaderSkyNebulaColor1", SETTING_NEBULA_COLOR1, "#FFFF7A1A"), this::isNebulaMode);
    private final RGBAColorValue nebulaColor2 =
            visibleWhen(color("shaderSkyNebulaColor2", SETTING_NEBULA_COLOR2, "#FFDC1466"), this::isNebulaMode);
    private final RGBAColorValue nebulaStarColor =
            visibleWhen(color("shaderSkyNebulaStarColor", SETTING_NEBULA_STAR_COLOR, "#FFFFD27A"), this::isNebulaMode);
    private final NumberValue<Float> speed =
            num("shaderSkySpeed", SETTING_SPEED, 1.0f, 0.1f, 5.0f);
    private final NumberValue<Float> scale =
            num("shaderSkyScale", SETTING_SCALE, 5.0f, 1.0f, 20.0f);
    private final NumberValue<Float> intensity =
            num("shaderSkyIntensity", SETTING_INTENSITY, 0.01f, 0.001f, 0.05f);
    private final NumberValue<Float> alpha =
            num("shaderSkyAlpha", SETTING_ALPHA, 1.0f, 0.3f, 1.0f);
    private final NumberValue<Float> thunderInterval =
            visibleWhen(num("shaderSkyThunderInterval", SETTING_THUNDER_INTERVAL, 4.0f, 1.0f, 10.0f), this::isThunderMode);
    private final NumberValue<Float> thunderChance =
            visibleWhen(num("shaderSkyThunderChance", SETTING_THUNDER_CHANCE, 0.65f, 0.0f, 1.0f), this::isThunderMode);
    private final NumberValue<Float> thunderGlow =
            visibleWhen(num("shaderSkyThunderGlow", SETTING_THUNDER_GLOW, 1.0f, 0.1f, 3.0f), this::isThunderMode);
    private final NumberValue<Float> starryIntensity =
            visibleWhen(num("shaderSkyStarryIntensity", SETTING_STARRY_INTENSITY, 1.0f, 0.1f, 5.0f), this::isStarryMode);
    private final NumberValue<Float> starryDensity =
            visibleWhen(num("shaderSkyStarryDensity", SETTING_STARRY_DENSITY, 64.0f, 8.0f, 256.0f), this::isStarryMode);
    private final NumberValue<Float> starryMilkyWay =
            visibleWhen(num("shaderSkyStarryMilkyWay", SETTING_STARRY_MILKYWAY, 0.6f, 0.0f, 1.5f), this::isStarryMode);
    private final NumberValue<Float> starryMoonSize =
            visibleWhen(num("shaderSkyStarryMoonSize", SETTING_STARRY_MOON_SIZE, 0.055f, 0.01f, 0.15f), this::isStarryMode);
    private final BooleanValue starryMoon =
            visibleWhen(bool("shaderSkyStarryMoon", "starry_moon", true), this::isStarryMode);
    private final RGBAColorValue starryMoonColor =
            visibleWhen(color("shaderSkyStarryMoonColor", SETTING_STARRY_MOON_COLOR, "#FFF5F0DC"), this::isStarryMode);
    private final RGBAColorValue starryStarColor =
            visibleWhen(color("shaderSkyStarryStarColor", SETTING_STARRY_STAR_COLOR, "#FFFFFFFF"), this::isStarryMode);

    public String getMode() {
        return mode.get();
    }

    public float getSpeed() {
        return speed.get();
    }

    public float getScale() {
        return scale.get();
    }

    public float getIntensity() {
        return intensity.get();
    }

    public float getAlpha() {
        return alpha.get();
    }

    public float getThunderInterval() {
        return thunderInterval.get();
    }

    public float getThunderChance() {
        return thunderChance.get();
    }

    public float getThunderGlow() {
        return thunderGlow.get();
    }

    private boolean isThunderMode() {
        return "Thunder".equals(mode.get());
    }

    private boolean isSkyShaderMode() {
        String m = mode.get();
        return "Sky".equals(m) || "Water".equals(m) || "Caustic".equals(m);
    }

    private boolean isNebulaMode() {
        return "Nebula".equals(mode.get());
    }

    private boolean isStarryMode() {
        return "Starry".equals(mode.get());
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.AFTER_POST_PROCESS;
    }

    @Override
    public void onEnable() {
        SkyShaderRenderer.reset();
        SkyShaderStarryRenderer.reset();
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null) return;

        if (isSkyShaderMode()) {
            SkyShaderRenderer.Mode skyMode = switch (mode.get()) {
                case "Water" -> SkyShaderRenderer.Mode.WATER;
                case "Caustic" -> SkyShaderRenderer.Mode.CAUSTIC;
                default -> SkyShaderRenderer.Mode.SKY;
            };
            SkyShaderRenderer.render(
                    mc,
                    skyMode,
                    speed.get(),
                    scale.get(),
                    intensity.get(),
                    alpha.get(),
                    0xFF6FA8DC & 0x00FFFFFF
            );
        } else if (isNebulaMode()) {
            SkyShaderRenderer.renderNebula(
                    mc,
                    speed.get(),
                    nebulaIntensity.get(),
                    alpha.get(),
                    nebulaColor1.getArgb(),
                    nebulaColor2.getArgb(),
                    nebulaStarColor.getArgb()
            );
        } else if (isStarryMode()) {
            SkyShaderStarryRenderer.render(
                    mc,
                    speed.get(),
                    starryIntensity.get(),
                    alpha.get(),
                    starryDensity.get(),
                    starryMilkyWay.get(),
                    starryMoonSize.get(),
                    starryMoon.get(),
                    starryMoonColor.getArgb(),
                    starryStarColor.getArgb()
            );
        }
    }
}
