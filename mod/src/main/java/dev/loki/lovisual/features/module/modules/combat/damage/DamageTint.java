package dev.loki.lovisual.features.module.modules.combat.damage;

import dev.loki.lovisual.util.time.GameClock;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessManager;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessPass;
import dev.loki.lovisual.render.engine.renderer.FullScreenRenderer;
import dev.loki.lovisual.render.engine.uniform.impl.post.PostProcessUniforms;

@ModuleInfo(id = "damagetint", displayName = "DamageTint", category = ModuleCategory.COMBAT)
public class DamageTint extends Module implements PostProcessPass {

    private static final String SETTING_START_HP = "start_hp";
    private static final String SETTING_FULL_HP = "full_hp";
    private static final String SETTING_VIGNETTE = "vignette";
    private static final String SETTING_DESAT = "desat";
    private static final String SETTING_CONTRAST = "contrast";
    private static final String SETTING_PULSE = "pulse";
    private static final String SETTING_PULSE_SPEED = "pulse_speed";
    private static final String SETTING_PULSE_MIN = "pulse_min";
    private static final String SETTING_PULSE_MAX = "pulse_max";
    private static final String SETTING_FADE_IN = "fade_in";
    private static final String SETTING_FADE_OUT = "fade_out";
    private final NumberValue<Integer> startHearts =
            num("damageTintStartHearts", SETTING_START_HP, 20, 1, 80);
    private final NumberValue<Integer> fullHearts =
            num("damageTintFullHearts", SETTING_FULL_HP, 8, 1, 80);
    private final NumberValue<Float> vignetteStrength =
            num("damageTintVignette", SETTING_VIGNETTE, 0.75f, 0.0f, 1.0f);
    private final NumberValue<Float> desatStrength =
            num("damageTintDesat", SETTING_DESAT, 0.55f, 0.0f, 1.0f);
    private final NumberValue<Float> contrastStrength =
            num("damageTintContrast", SETTING_CONTRAST, 0.35f, 0.0f, 1.0f);
    private final BooleanValue pulse =
            bool("damageTintPulse", SETTING_PULSE, true);
    private final NumberValue<Float> pulseSpeed =
            visibleWhen(num("damageTintPulseSpeed", SETTING_PULSE_SPEED, 1.2f, 0.1f, 4.0f), pulse::get);
    private final NumberValue<Float> pulseMin =
            visibleWhen(num("damageTintPulseMin", SETTING_PULSE_MIN, 0.1f, 0.0f, 1.0f), pulse::get);
    private final NumberValue<Float> pulseMax =
            visibleWhen(num("damageTintPulseMax", SETTING_PULSE_MAX, 0.35f, 0.0f, 2.0f), pulse::get);
    private final NumberValue<Float> fadeInSeconds =
            num("damageTintFadeIn", SETTING_FADE_IN, 0.2f, 0.05f, 2.0f);
    private final NumberValue<Float> fadeOutSeconds =
            num("damageTintFadeOut", SETTING_FADE_OUT, 0.35f, 0.05f, 3.0f);

    private float smoothT;
    private long lastUpdateMs;

    {
        PostProcessManager.register(this);
    }

    @Override
    public boolean isActive() {
        Minecraft mc = Minecraft.getInstance();
        return isEnabled() && mc.player != null && mc.level != null;
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public Phase getPhase() {
        return Phase.POST_HAND;
    }

    @Override
    public boolean render(GpuTextureView src, GpuTextureView dst, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null || mc.level == null) return false;
        if (src == null || dst == null) return false;

        float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        float hpGate = Mth.floor(hp * 2.0f) / 2.0f;
        float start = startHearts.get().floatValue();
        float full = fullHearts.get().floatValue();

        if (start < full) {
            float tmp = start;
            start = full;
            full = tmp;
        }

        float range = Math.max(0.1f, start - full);
        float targetT = Mth.clamp((start - hpGate) / range, 0.0f, 1.0f);
        float t = smoothTowards(targetT);

        if (t <= 0.0f) {
            return false;
        }

        float strength = t * vignetteStrength.get();
        float desat = t * desatStrength.get();
        float contrast = t * contrastStrength.get();

        if (pulse.get()) {
            float time = (GameClock.millis() % 100000L) / 1000.0f;
            float wave = Mth.sin(time * pulseSpeed.get() * 6.2831855f);
            float amp = Mth.lerp(t, pulseMin.get(), pulseMax.get());
            strength *= 1.0f + (wave * amp);
        }

        if (strength <= 0.001f && desat <= 0.001f && contrast <= 0.001f) {
            return false;
        }

        PostProcessUniforms.update(strength, desat, contrast, 0.0f);

        FullScreenRenderer.begin("LoVisual Fullscreen Pass")
                .attachment(dst)
                .pipeline(LoVisualRenderPipelines.DAMAGE_TINT)
                .uniform("PostProcess", PostProcessUniforms.get())
                .sampler("u_Texture", src, PostProcessManager.getSampler())
                .end();

        return true;
    }

    @Override
    public void onDisable() {
        smoothT = 0.0f;
        lastUpdateMs = 0L;
    }

    private float smoothTowards(float target) {
        long now = GameClock.millis();
        float dt = (lastUpdateMs == 0L) ? 0.016f : (now - lastUpdateMs) / 1000.0f;
        lastUpdateMs = now;

        float in = Math.max(0.01f, fadeInSeconds.get());
        float out = Math.max(0.01f, fadeOutSeconds.get());

        float rate = target >= smoothT ? (1.0f / in) : (1.0f / out);
        float delta = target - smoothT;
        float step = Mth.clamp(rate * dt, 0.0f, 1.0f);
        smoothT += delta * step;
        smoothT = Mth.clamp(smoothT, 0.0f, 1.0f);
        return smoothT;
    }

}
