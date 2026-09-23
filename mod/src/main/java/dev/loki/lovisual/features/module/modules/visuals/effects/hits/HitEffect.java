package dev.loki.lovisual.features.module.modules.visuals.effects.hits;

import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.animation.AnimatedColorModeOptions;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.helpers.util.ParticleTextureMode;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(id = "hiteffect", displayName = "HitEffect", category = ModuleCategory.VISUALS)
public class HitEffect extends Module {
    private static final String SETTING_EFFECT_MODE = "effect_mode";
    private static final String SETTING_LIFE_TIME = "life_time";
    private static final String SETTING_SCALE = "scale";
    private static final String SETTING_ONLY_PLAYERS = "only_players";
    private static final String SETTING_DEPTH_TEST = "depth_test";
    private static final String SETTING_STARS = "stars";
    private static final String SETTING_STARS_COLOR = "stars_color";
    private static final String SETTING_CRIT_STARS_COLOR = "crit_stars_color";
    private static final String SETTING_COLOR_MODE = "color_mode";
    private static final String SETTING_COLOR_SPEED = "color_speed";
    private static final String SETTING_COLOR = "color";
    private static final String SETTING_COLOR2 = "color2";
    private static final String SETTING_WAVE_RADIUS = "wave_radius";
    private static final String SETTING_WAVE_DURATION = "wave_duration";
    private static final String SETTING_IMPACT_RADIUS = "impact_radius";
    private static final String SETTING_IMPACT_DURATION = "impact_duration";
    private static final String SETTING_IMPACT_GLOW_HEIGHT = "impact_glow_height";
    private static final String SETTING_HIT_PARTICLES = "hit_particles";
    private static final String SETTING_HIT_PARTICLE_COUNT = "hit_particle_count";
    private static final String SETTING_HIT_PARTICLE_SIZE = "hit_particle_size";
    private static final String SETTING_HIT_PARTICLE_LIFE = "hit_particle_life_ms";
    private static final String SETTING_HIT_PARTICLE_TEXTURE = "hit_particle_texture";
    private static final String SETTING_HIT_PARTICLE_RANDOM_COLOR = "hit_particle_random_color";
    final EnumValue<EffectMode> effectMode = enumSetting("hitBubblesEffectMode", SETTING_EFFECT_MODE, EffectMode.BUBBLES, EffectMode.values());
    final NumberValue<Integer> lifeTime = visibleWhen(num("hitBubblesLifeTime", SETTING_LIFE_TIME, 30, 1, 150), this::isBubblesMode);
    final NumberValue<Float> bubbleScale = visibleWhen(num("hitBubblesScale", SETTING_SCALE, 1.0f, 0.4f, 2.0f), this::isBubblesMode);
    final ModeValue colorMode = modeSetting("hitBubblesColorMode", SETTING_COLOR_MODE, "Static", "Static", "Rainbow", "LightRainbow", "Sky", "Fade", "DoubleColor", "Analogous", "Theme");
    final NumberValue<Integer> colorSpeed = num("hitBubblesColorSpeed", SETTING_COLOR_SPEED, 18, 2, 54);
    final RGBAColorValue colorValue = color("hitBubblesColor", SETTING_COLOR, "#FF55FFFF");
    final RGBAColorValue colorValue2 = visibleWhen(color("hitBubblesColor2", SETTING_COLOR2, "#FFFF5555"), this::usesSecondaryColor);
    final BooleanValue depthTest = bool("hitBubblesDepthTest", SETTING_DEPTH_TEST, true);
    final BooleanValue onlyPlayers = bool("hitBubblesOnlyPlayers", SETTING_ONLY_PLAYERS, true);
    final BooleanValue starsEnabled = visibleWhen(bool("hitBubblesStarsEnabled", SETTING_STARS, true), this::isBubblesMode);
    final RGBAColorValue starsColor = visibleWhen(color("hitBubblesStarsColor", SETTING_STARS_COLOR, "#FFFFE16B"), () -> isBubblesMode() && starsEnabled.get());
    final RGBAColorValue critStarsColor = visibleWhen(color("hitBubblesCritStarsColor", SETTING_CRIT_STARS_COLOR, "#FFFF3A2E"), () -> isBubblesMode() && starsEnabled.get());
    final NumberValue<Integer> waveRadius = visibleWhen(num("hitBubblesWaveRadius", SETTING_WAVE_RADIUS, 12, 3, 32), this::isWaveMode);
    final NumberValue<Integer> waveDurationMs = visibleWhen(num("hitBubblesWaveDuration", SETTING_WAVE_DURATION, 1500, 200, 6000), this::isWaveMode);
    final NumberValue<Float> impactRadius = visibleWhen(num("hitBubblesImpactRadius", SETTING_IMPACT_RADIUS, 1.4f, 0.4f, 4.0f), this::isImpactMode);
    final NumberValue<Integer> impactDurationMs = visibleWhen(num("hitBubblesImpactDuration", SETTING_IMPACT_DURATION, 450, 150, 1500), this::isImpactMode);
    final NumberValue<Float> impactGlowHeight = visibleWhen(num("hitBubblesImpactGlowHeight", SETTING_IMPACT_GLOW_HEIGHT, 0.8f, 0.2f, 2.5f), this::isImpactMode);
    final BooleanValue hitParticlesEnabled = bool("hitEffectParticlesEnabled", SETTING_HIT_PARTICLES, false);
    final NumberValue<Integer> hitParticleCount = visibleWhen(num("hitEffectParticleCount", SETTING_HIT_PARTICLE_COUNT, 15, 1, 80), hitParticlesEnabled::get);
    final NumberValue<Float> hitParticleSize = visibleWhen(num("hitEffectParticleSize", SETTING_HIT_PARTICLE_SIZE, 0.12f, 0.04f, 0.4f), hitParticlesEnabled::get);
    final NumberValue<Integer> hitParticleLifeMs = visibleWhen(num("hitEffectParticleLifeMs", SETTING_HIT_PARTICLE_LIFE, 2200, 200, 6000), hitParticlesEnabled::get);
    final EnumValue<ParticleTextureMode> hitParticleTexture = visibleWhen(enumSetting("hitEffectParticleTexture", SETTING_HIT_PARTICLE_TEXTURE, ParticleTextureMode.BLOOM, ParticleTextureMode.values()), hitParticlesEnabled::get);
    final BooleanValue hitParticleRandomColor = visibleWhen(bool("hitEffectParticleRandomColor", SETTING_HIT_PARTICLE_RANDOM_COLOR, false), hitParticlesEnabled::get);
    private final HitBubbleHelper bubbleHelper;
    private final HitParticleHelper particleHelper;
    private final HitWaveImpactHelper waveImpactHelper;

    public HitEffect() {
        bubbleHelper = new HitBubbleHelper(this);
        particleHelper = new HitParticleHelper(this);
        waveImpactHelper = new HitWaveImpactHelper(this);
    }

    @Override public WorldPhase getWorldPhase() { return WorldPhase.END_MAIN; }
    @Override public void onDisable() { bubbleHelper.clear(); particleHelper.clear(); waveImpactHelper.clear(); }
    public void handleHit(Entity target) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null || mc.level == null) return;
        if (!(target instanceof LivingEntity)) return;
        if (onlyPlayers.get() && !(target instanceof Player)) return;
        if (effectMode.get() == EffectMode.WAVE) {
            Vec3 pos = target.position();
            BlockPos basePos = BlockPos.containing(pos.x, pos.y - 0.1, pos.z);
            waveImpactHelper.addWave(basePos);
        } else if (effectMode.get() == EffectMode.IMPACT) {
            waveImpactHelper.addImpact(target.position());
        } else {
            bubbleHelper.handleHit((LivingEntity) target);
        }
        if (hitParticlesEnabled.get()) particleHelper.spawn((LivingEntity) target);
    }
    @Override public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null || mc.level == null) return;
        bubbleHelper.tick();
        particleHelper.tick();
    }
    @Override public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null || mc.level == null) return;
        if (hitParticlesEnabled.get()) particleHelper.render(renderer, tickDelta);
        if (effectMode.get() == EffectMode.WAVE) { waveImpactHelper.renderWave(renderer); return; }
        if (effectMode.get() == EffectMode.IMPACT) { waveImpactHelper.renderImpact(renderer); return; }
        bubbleHelper.render(renderer, tickDelta);
    }
    boolean isBubblesMode() { return effectMode.get() == EffectMode.BUBBLES; }
    boolean isWaveMode() { return effectMode.get() == EffectMode.WAVE; }
    boolean isImpactMode() { return effectMode.get() == EffectMode.IMPACT; }
    boolean usesSecondaryColor() { return AnimatedRenderColors.usesSecondary(animatedColorMode()); }
    int getColorArgb(int count) { return AnimatedRenderColors.resolve(animatedColorMode(), colorSpeed.get(), count, colorValue.getArgb(), colorValue2.getArgb()); }
    private AnimatedRenderColors.Mode animatedColorMode() { return AnimatedColorModeOptions.parse(colorMode.get()); }
    public enum EffectMode { BUBBLES, WAVE, IMPACT }
}
