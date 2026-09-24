package dev.loki.lovisual.features.module.modules.visuals.effects.hits;

import dev.loki.lovisual.util.time.GameClock;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.mixins.accessors.world.ClientLevelAccessor;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.util.wav.engine.CustomSoundEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

//todo Description
@ModuleInfo(id = "killeffect", displayName = "KillEffect", category = ModuleCategory.VISUALS)
public class KillEffect extends Module {
    private static final String SETTING_MODE = "mode";
    private static final String SETTING_Y_SPEED = "y_speed";
    private static final String SETTING_PLAY_SOUND = "play_sound";
    private static final String SETTING_SOUND_VOLUME = "sound_volume";
    private static final String SETTING_COLOR = "color";
    private static final String SETTING_MOBS = "mobs";
    private static final String SETTING_BEAM_INTENSITY = "beam_intensity";
    private static final String SETTING_BEAM_SIZE = "beam_size";
    private static final Identifier ORTHODOX_SOUND =
            Identifier.fromNamespaceAndPath("lovisual", "sounds/misc/orthodox.wav");

    private final ModeValue mode = modeSetting("killEffectMode", SETTING_MODE, "Orthodox",
            "Orthodox", "FallingLava", "EmbersArc", "Lightning", "Portal", "Tornado");
    private final NumberValue<Integer> ySpeed = visibleWhen(
            num("killEffectYSpeed", SETTING_Y_SPEED, 0, -10, 10), () -> isMode("Orthodox"));
    private final BooleanValue playSound = visibleWhen(
            bool("killEffectPlaySound", SETTING_PLAY_SOUND, true), () -> isMode("Orthodox"));
    private final NumberValue<Double> soundVolume = visibleWhen(
            num("killEffectSoundVolume", SETTING_SOUND_VOLUME, 1.0, 0.0, 2.0),
            () -> isMode("Orthodox") && playSound.get());
    private final RGBAColorValue color = visibleWhen(
            color("killEffectColor", SETTING_COLOR, "#FFFF9600"), this::usesColorSetting);
    private final BooleanValue mobs = bool("killEffectMobs", SETTING_MOBS, false);
    private final NumberValue<Float> beamIntensity = visibleWhen(
            num("killEffectBeamIntensity", SETTING_BEAM_INTENSITY, 1.2f, 0.2f, 3.0f), this::usesBeamSettings);
    private final NumberValue<Float> beamSize = visibleWhen(
            num("killEffectBeamSize", SETTING_BEAM_SIZE, 2.0f, 0.5f, 5.0f), this::usesBeamSettings);

    private final Map<Integer, Long> handled = new HashMap<>();
    private final KillOrthodoxHelper orthodoxHelper = new KillOrthodoxHelper();
    private final KillEmberHelper emberHelper = new KillEmberHelper();
    private final KillLightningHelper lightningHelper = new KillLightningHelper();
    private final KillBeamHelper beamHelper = new KillBeamHelper();

    private boolean isMode(String value) {
        return value.equals(mode.get());
    }

    private boolean usesColorSetting() {
        return isMode("Orthodox") || isMode("Lightning") || usesBeamSettings();
    }

    private boolean usesBeamSettings() {
        return isMode("Portal") || isMode("Tornado");
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.END_MAIN;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null) {
            return;
        }
        long now = GameClock.millis();
        handled.entrySet().removeIf(e -> now - e.getValue() > 6000);
        for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class,
                mc.player.getBoundingBox().inflate(128), e -> true)) {
            if (entity == mc.player) {
                continue;
            }
            if (!mobs.get() && !(entity instanceof Player)) {
                continue;
            }
            if (entity.isAlive() || entity.getHealth() > 0) {
                continue;
            }
            int id = entity.getId();
            if (handled.containsKey(id)) {
                continue;
            }
            handled.put(id, now);
            Vec3 pos = entity.position();
            handleKill(pos, entity, now);
        }
        emberHelper.tick(mc.level);
        beamHelper.tick();
        lightningHelper.tick();
        orthodoxHelper.tick(now);
    }

    private void handleKill(Vec3 pos, LivingEntity entity, long now) {
        String current = mode.get();
        if ("Orthodox".equals(current)) {
            orthodoxHelper.add(pos, now);
            if (playSound.get()) {
                CustomSoundEngine.get().play(ORTHODOX_SOUND, soundVolume.get(), 1.0, false, true);
            }
        } else if ("FallingLava".equals(current)) {
            spawnFallingLava(entity);
        } else if ("EmbersArc".equals(current)) {
            emberHelper.spawn(pos);
        } else if ("Lightning".equals(current)) {
            lightningHelper.add(pos, color.getArgb());
        } else if ("Portal".equals(current)) {
            beamHelper.add(pos, color.getArgb(), KillBeamHelper.BeamKind.PORTAL);
        } else if ("Tornado".equals(current)) {
            beamHelper.add(pos, color.getArgb(), KillBeamHelper.BeamKind.TORNADO);
        }
    }

    @Override
    public void onDisable() {
        handled.clear();
        orthodoxHelper.clear();
        emberHelper.clear();
        lightningHelper.clear();
        beamHelper.clear();
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null) {
            return;
        }
        String current = mode.get();
        if ("Orthodox".equals(current)) {
            orthodoxHelper.render(renderer, color.getArgb(), ySpeed.get());
        } else if ("Lightning".equals(current)) {
            lightningHelper.render(renderer);
        } else if ("EmbersArc".equals(current)) {
            emberHelper.render(renderer, tickDelta);
        }
        beamHelper.render(renderer, mc, beamIntensity.get(), beamSize.get());
    }

    private void spawnFallingLava(LivingEntity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        int height = (int) (entity.getBbHeight() * 10);
        int width = (int) (entity.getBbWidth() * 10);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                for (int k = 0; k < width; k++) {
                    ((ClientLevelAccessor) mc.level).invokeAddParticle(
                            ParticleTypes.FALLING_LAVA, false, true,
                            entity.getX() + j * 0.1,
                            entity.getY() + i * 0.1,
                            entity.getZ() + k * 0.1,
                            0, 0, 0);
                }
            }
        }
    }
}
