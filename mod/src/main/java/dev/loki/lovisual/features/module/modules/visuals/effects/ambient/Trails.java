package dev.loki.lovisual.features.module.modules.visuals.effects.ambient;

import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.mixininterface.entity.IEntity;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.helpers.player.Particle3D;
import dev.loki.lovisual.render.helpers.player.TrailPoint;
import dev.loki.lovisual.render.helpers.util.ParticleTextureMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(id = "trails", displayName = "Trails", category = ModuleCategory.VISUALS)
public class Trails extends Module {
    private final Random random = new Random();
    private final TrailsColor trailColor = new TrailsColor();
    private final TrailsGeometry trailGeometry = new TrailsGeometry();
    private final TrailsParticles trailParticles = new TrailsParticles();
    private final TrailsRenderer trailRenderer = new TrailsRenderer();
    private final BooleanValue onlySelf = bool("trailsOnlySelf", "only_self", false);
    private final BooleanValue hideFirstPerson = bool("trailsHideFirstPerson", "hide_first_person", true);
    private final BooleanValue depthTest = bool("trailsDepthTest", "depth_test", true);
    private final ModeValue trailMode = modeSetting("trailsTrailMode", "trail_mode", "Tail", "Tail", "Trail");
    private final RGBAColorValue colorValue = color("trailsColor", "color", "#8800FF00");
    private final ModeValue colorMode = modeSetting("trailsColorMode", "color_mode", "Static", "Static", "Rainbow", "LightRainbow", "Sky", "Fade", "DoubleColor", "Analogous", "Theme");
    private final RGBAColorValue colorValue2 = color("trailsColor2", "color2", "#FF55FFFF");
    private final NumberValue<Integer> colorSpeed = num("trailsColorSpeed", "color_speed", 18, 2, 54);
    private final NumberValue<Integer> lifeTimeTicks = num("trailsLifeTimeTicks", "life_time_ticks", 10, 1, 40);
    private final NumberValue<Float> trailScale = num("trailsScale", "scale", 0.6f, 0.2f, 1.2f);
    private final NumberValue<Float> trailDown = num("trailsTrailDown", "trail_down", 0.5f, 0.0f, 2.0f);
    private final NumberValue<Float> trailHeight = num("trailsTrailHeight", "trail_height", 1.3f, 0.1f, 2.0f);
    private final BooleanValue trailStroke = bool("trailsTrailStroke", "trail_stroke", true);
    private final NumberValue<Float> trailStrokeWidth = num("trailsTrailStrokeWidth", "trail_stroke_width", 2.0f, 0.5f, 8.0f);
    private final NumberValue<Float> trailStrokeAlpha = num("trailsTrailStrokeAlpha", "trail_stroke_alpha", 0.75f, 0.05f, 1.0f);
    private final NumberValue<Integer> colorPhase = num("trailsColorPhase", "color_phase", 0, 0, 360);
    private final NumberValue<Integer> colorLengthSpread = num("trailsColorLengthSpread", "color_length_spread", 180, -720, 720);
    private final NumberValue<Integer> colorAgeSpread = num("trailsColorAgeSpread", "color_age_spread", 90, -720, 720);
    private final BooleanValue particlesEnabled = bool("trailsParticlesEnabled", "particles_enabled", false);
    private final BooleanValue particlesHideFirstPerson = bool("trailsParticlesHideFirstPerson", "particles_hide_first_person", false);
    private final BooleanMapValue particleTriggers = group("trailsParticleTriggers", "particle_triggers", Map.of("walk", true, "jump", true, "projectile", true));
    private final NumberValue<Integer> particleWalkCount = num("trailsParticleWalkCount", "particle_walk_count", 3, 1, 8);
    private final NumberValue<Integer> particleJumpCount = num("trailsParticleJumpCount", "particle_jump_count", 12, 1, 30);
    private final NumberValue<Integer> particleProjectileCount = num("trailsParticleProjectileCount", "particle_projectile_count", 3, 1, 10);
    private final EnumValue<ParticleTextureMode> particleTexture = enumSetting("trailsParticleTexture", "particle_texture", ParticleTextureMode.BLOOM, ParticleTextureMode.values());
    private final BooleanValue particleRandomColor = bool("trailsParticleRandomColor", "particle_random_color", false);
    private final NumberValue<Float> particleSize = num("trailsParticleSize", "particle_size", 0.12f, 0.04f, 0.4f);
    private final NumberValue<Integer> particleLifeMs = num("trailsParticleLifeMs", "particle_life_ms", 1600, 200, 5000);
    private final BooleanValue connectedPath = bool("trailsConnectedPath", "connected_path", true);
    private final NumberValue<Float> connectStep = num("trailsConnectedStep", "connected_step", 0.35f, 0.1f, 1.0f);
    private final Map<UUID, Boolean> groundState = new HashMap<>();
    private final List<Particle3D> extraParticles = new ArrayList<>();
    private final List<Particle3D>[] particleBuckets = trailParticles.createBuckets();

    @Override public WorldPhase getWorldPhase() { return WorldPhase.END_MAIN; }

    @Override public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (Player p : mc.level.players()) ((IEntity) p).lovisual$getTrails().clear();
        extraParticles.clear(); groundState.clear();
    }

    @Override public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null) return;
        int colorIndex = mc.player.tickCount % 360;
        boolean on = particlesEnabled.get();
        boolean walk = on && particleTriggers.get("walk");
        boolean jump = on && particleTriggers.get("jump");
        boolean proj = on && particleTriggers.get("projectile");
        trailParticles.updateExtra(extraParticles);
        Set<UUID> active = new HashSet<>();
        for (Player player : mc.level.players()) {
            if (onlySelf.get() && player != mc.player) continue;
            active.add(player.getUUID());
            List<TrailPoint> trails = ((IEntity) player).lovisual$getTrails();
            Vec3 last = ((IEntity) player).get$InstantRenderPos();
            boolean skip = hideFirstPerson.get() && player == mc.player && mc.options.getCameraType().isFirstPerson();
            boolean moved = player.getX() != last.x || player.getZ() != last.z;
            if (!skip && moved) {
                int argb = trailColor.getColorArgb(colorMode.get(), colorSpeed.get(), colorIndex, colorValue.getArgb(), colorValue2.getArgb());
                trails.add(new TrailPoint(last, player.position(), argb, lifeTimeTicks.get()));
            }
            trails.removeIf(TrailPoint::update);
            boolean skipP = particlesHideFirstPerson.get() && player == mc.player && mc.options.getCameraType().isFirstPerson();
            if (on && !skipP) {
                if (walk && moved) trailParticles.spawnWalk(player, colorIndex, random, extraParticles, particleWalkCount.get(), trailColor, colorMode.get(), colorSpeed.get(), colorValue.getArgb(), colorValue2.getArgb(), particleSize.get(), particleLifeMs.get(), particleTexture.get(), particleRandomColor.get());
                boolean prev = groundState.getOrDefault(player.getUUID(), player.onGround());
                boolean now = player.onGround();
                if (jump && prev && !now) trailParticles.spawnJump(player, colorIndex, random, extraParticles, particleJumpCount.get(), trailColor, colorMode.get(), colorSpeed.get(), colorValue.getArgb(), colorValue2.getArgb(), particleSize.get(), particleLifeMs.get(), particleTexture.get(), particleRandomColor.get());
                groundState.put(player.getUUID(), now);
            }
        }
        if (on && proj) trailParticles.spawnProjectile(colorIndex, extraParticles, random, mc, particleProjectileCount.get(), trailColor, colorMode.get(), colorSpeed.get(), colorValue.getArgb(), colorValue2.getArgb(), particleSize.get(), particleLifeMs.get(), particleTexture.get(), particleRandomColor.get());
        groundState.keySet().removeIf(id -> !active.contains(id));
    }

    @Override public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null) return;
        boolean useDepth = depthTest.get();
        if (particlesEnabled.get() && !extraParticles.isEmpty()) trailParticles.renderExtra(renderer, tickDelta, useDepth, particleTexture.get(), extraParticles, particleBuckets, trailGeometry);
        if (isTrailMode()) {
            int phase = trailColor.currentColorPhase(tickDelta, colorPhase.get());
            trailRenderer.renderRibbon(renderer, tickDelta, useDepth, trailDown.get(), trailHeight.get(), phase, trailColor, trailGeometry, colorMode.get(), colorSpeed.get(), colorValue.getArgb(), colorValue2.getArgb(), colorLengthSpread.get(), colorAgeSpread.get(), onlySelf.get(), hideFirstPerson.get(), trailStroke.get(), trailStrokeWidth.get(), trailStrokeAlpha.get());
            return;
        }
        int phase = trailColor.currentColorPhase(tickDelta, colorPhase.get());
        trailRenderer.renderTail(renderer, tickDelta, useDepth, trailScale.get(), connectedPath.get(), connectStep.get(), phase, trailColor, trailGeometry, colorMode.get(), colorSpeed.get(), colorValue.getArgb(), colorValue2.getArgb(), colorLengthSpread.get(), colorAgeSpread.get(), onlySelf.get(), hideFirstPerson.get());
    }

    private boolean isTrailMode() { return "Trail".equals(trailMode.get()); }
}
