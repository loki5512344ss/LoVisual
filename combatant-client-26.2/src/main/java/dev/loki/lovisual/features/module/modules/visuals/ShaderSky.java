package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.config.values.ModeValue;
import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;
import dev.loki.lovisual.features.module.WorldPhase;
import dev.loki.lovisual.render.engine.postprocess.SkyShaderRenderer;
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

    private final ModeValue mode = modeSetting(
            "shaderSkyMode",
            SETTING_MODE,
            "Thunder",
            "Thunder", "Pulsar", "Sky", "Water", "Caustic"
    );
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

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.AFTER_POST_PROCESS;
    }

    @Override
    public void onEnable() {
        SkyShaderRenderer.reset();
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
        }
    }
}