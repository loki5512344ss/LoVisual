package dev.loki.lovisual.features.module.modules.combat.prediction;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import dev.loki.lovisual.config.common.CommonSettingSchemas;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.features.module.modules.combat.targetesp.constellation.TargetEspConstellation;
import dev.loki.lovisual.features.module.modules.combat.targetesp.state.TargetEspColors;
import dev.loki.lovisual.features.module.modules.combat.targetesp.sword.TargetEspSwords;
import dev.loki.lovisual.features.module.modules.combat.targetesp.state.TargetEspTracker;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.renderer.Renderer3DCullOptions;
import dev.loki.lovisual.render.engine.renderer.Renderer3DCulling;
import dev.loki.lovisual.util.target.TargetManager;

import java.util.function.IntFunction;
import java.util.function.Supplier;

import static dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspEffects.renderBlackhole;
import static dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspEffects.renderGhosts;
import static dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspModes.drawOldTargetEsp;
import static dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspModes.renderCaptureMark;
import static dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspModes.renderVortex;
import static dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspModes.setCaptureSpeedScale;
import static dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspModes.tickTargetEsp;

//todo Description
@ModuleInfo(id = "targetesp", displayName = "TargetESP", aliases = {"target", "targetrender"}, category = ModuleCategory.COMBAT)
public class TargetESP extends Module {

    private final ModeValue espMode = modeSetting("targetEspMode", "mode", "ghosts",
            "ghosts", "ring", "capture_mark", "crystals", "blackhole", "vortex", "constellation", "swords");
    private final RGBAColorValue colorValue = color("targetEspColor", "color", "#FFFF5555");
    private final ModeValue colorMode = visibleWhen(modeSetting("targetEspColorMode", "color_mode", "Static",
            "Static", "Rainbow", "LightRainbow", "Sky", "Fade", "DoubleColor", "Analogous", "Theme"), () -> true);
    private final NumberValue<Integer> colorSpeed =
            visibleWhen(num("targetEspColorSpeed", "color_speed", 18, 2, 54), () -> true);
    private final NumberValue<Integer> colorPhase = num("targetEspColorPhase", "color_phase", 0, 0, 360);
    private final NumberValue<Integer> colorSpread = num("targetEspColorSpread", "color_spread", 90, -720, 720);
    private final RGBAColorValue colorValue2 =
            visibleWhen(color("targetEspColor2", "color2", "#FF55FFFF"), this::usesSecondaryColor);
    private final BooleanValue useRelationColors = boolCommon("targetEspUseRelationColors", "use_relation_colors",
            CommonSettingSchemas.PLAYER_USE_RELATIONS, false);
    private final NumberValue<Float> relationColorMix = visibleWhen(
            num("targetEspRelationColorMix", "relation_color_mix", 1.0f, 0.0f, 1.0f), useRelationColors::get);
    private final BooleanValue crosshairTargets = bool("targetEspCrosshairTargets", "crosshair_targets", true);
    private final NumberValue<Float> alphaAnimationSpeed =
            num("targetEspAlphaAnimationSpeed", "alpha_animation_speed", 8.0f, 2.0f, 10.0f);
    private final NumberValue<Integer> espLength =
            visibleWhen(num("targetEspLength", "esp_length", 14, 1, 40), onMode("ghosts"));
    private final NumberValue<Integer> espFactor =
            visibleWhen(num("targetEspFactor", "esp_factor", 8, 1, 20), onMode("ghosts"));
    private final NumberValue<Float> espShaking =
            visibleWhen(num("targetEspShaking", "esp_shaking", 1.8f, 1.5f, 10f), onMode("ghosts"));
    private final NumberValue<Float> espAmplitude =
            visibleWhen(num("targetEspAmplitude", "esp_amplitude", 3f, 0.1f, 8f), onMode("ghosts"));
    private final NumberValue<Float> captureSpinSpeed = visibleWhen(
            num("targetEspCaptureSpinSpeed", "capture_spin_speed", 1.0f, 0.2f, 4.0f), onMode("capture_mark"));
    private final BooleanValue crystalHitRed =
            visibleWhen(bool("targetEspCrystalHitRed", "crystal_hit_red", true), onMode("crystals"));
    private final NumberValue<Float> blackholeSize = visibleWhen(
            num("targetEspBlackholeSize", "blackhole_size", 1.1f, 0.5f, 3.0f), onMode("blackhole"));
    private final NumberValue<Float> blackholeSpinSpeed = visibleWhen(
            num("targetEspBlackholeSpinSpeed", "blackhole_spin_speed", 1.0f, 0.2f, 4.0f), onMode("blackhole"));
    private final ModeValue blackholeStyle = visibleWhen(
            modeSetting("targetEspBlackholeStyle", "blackhole_style", "Single", "Single", "Body"), onMode("blackhole"));
    private final NumberValue<Float> vortexSpeed =
            visibleWhen(num("targetEspVortexSpeed", "vortex_speed", 1.2f, 0.2f, 4.0f), onMode("vortex"));
    private final NumberValue<Integer> constellationStars = visibleWhen(
            num("targetEspConstellationStars", "constellation_stars", 9, 3, 24), onMode("constellation"));
    private final NumberValue<Float> constellationSpeed = visibleWhen(
            num("targetEspConstellationSpeed", "constellation_speed", 1.0f, 0.2f, 4.0f), onMode("constellation"));
    private final NumberValue<Float> constellationOrbit = visibleWhen(
            num("targetEspConstellationOrbit", "constellation_orbit", 1.1f, 0.5f, 2.5f), onMode("constellation"));
    private final BooleanValue constellationLines = visibleWhen(
            bool("targetEspConstellationLines", "constellation_lines", true), onMode("constellation"));
    private final NumberValue<Float> constellationPointSize = visibleWhen(
            num("targetEspConstellationPointSize", "constellation_point_size", 0.12f, 0.05f, 0.4f), onMode("constellation"));
    private final NumberValue<Float> constellationGlow = visibleWhen(
            num("targetEspConstellationGlow", "constellation_glow", 1.0f, 0.0f, 2.0f), onMode("constellation"));
    private final NumberValue<Integer> swordsSpeed = visibleWhen(
            num("targetEspSwordsSpeed", "swords_speed", 2200, 600, 8000), onMode("swords"));
    private final NumberValue<Float> swordsRadius = visibleWhen(
            num("targetEspSwordsRadius", "swords_radius", 1.0f, 0.5f, 2.5f), onMode("swords"));
    private final NumberValue<Float> swordsSize = visibleWhen(
            num("targetEspSwordsSize", "swords_size", 1.0f, 0.3f, 2.5f), onMode("swords"));
    private final NumberValue<Float> swordsBob = visibleWhen(
            num("targetEspSwordsBob", "swords_bob", 0.06f, 0.0f, 0.4f), onMode("swords"));
    private final BooleanValue swordsGlow = visibleWhen(bool("targetEspSwordsGlow", "swords_glow", true), onMode("swords"));
    private final BooleanValue swordsPhysics =
            visibleWhen(bool("targetEspSwordsPhysics", "swords_physics", true), onMode("swords"));
    private final TargetEspColors colors = new TargetEspColors(colorMode, colorSpeed, colorPhase, colorSpread,
            colorValue, colorValue2, useRelationColors, relationColorMix);
    private final TargetEspTracker tracker = new TargetEspTracker();
    private final TargetEspCrystalRenderer crystalRenderer = new TargetEspCrystalRenderer();
    private final TargetEspSwords swords = new TargetEspSwords();

    @Override
    public void onDisable() {
        tracker.reset();
        crystalRenderer.clear();
        swords.reset();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LivingEntity target = isEnabled() && mc.level != null ? TargetManager.getTarget(crosshairTargets.get()) : null;
        if (tracker.update(target, alphaAnimationSpeed.get()) || tracker.renderTarget() == null) {
            crystalRenderer.clear();
            swords.reset();
        }
        if (!isEnabled() || mc.level == null) return;

        setCaptureSpeedScale(espMode.get().equals("capture_mark") ? captureSpinSpeed.get() : 1.0f);
        if (tracker.isVisible()) {
            tickTargetEsp();
        }
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.AFTER_POST_PROCESS;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        LivingEntity target = tracker.renderTarget();
        if (!isEnabled() || !tracker.isVisible()) return;
        if (!Renderer3DCulling.shouldRender(target, tickDelta, resolveCullOptions(target))) return;

        float alpha = tracker.alpha();
        IntFunction<Integer> palette = index -> AnimatedRenderColors.scaleAlpha(getColorArgb(index), alpha);
        switch (espMode.get()) {
            case "ring" -> drawOldTargetEsp(renderer, target, tickDelta, palette);
            case "ghosts" -> renderGhosts(renderer, espLength.get(), espFactor.get(), espShaking.get(),
                    espAmplitude.get(), target, tickDelta, palette);
            case "capture_mark" -> renderCaptureMark(renderer, target, tickDelta, palette);
            case "crystals" -> crystalRenderer.render(renderer, target, tickDelta, crystalHitRed.get(), alpha, this::getColorArgb);
            case "blackhole" -> renderBlackhole(renderer, target, tickDelta, palette,
                    blackholeSize.get(), blackholeSpinSpeed.get(), alpha, blackholeStyle.get());
            case "vortex" -> renderVortex(renderer, target, tickDelta, palette, vortexSpeed.get());
            case "constellation" -> TargetEspConstellation.render(renderer, target, tickDelta, palette,
                    constellationStars.get(), constellationSpeed.get(), constellationOrbit.get(),
                    constellationLines.get(), constellationPointSize.get(), constellationGlow.get());
            case "swords" -> swords.render(renderer, target, tickDelta, palette, swordsSpeed.get(), swordsRadius.get(),
                    swordsSize.get(), swordsBob.get(), swordsGlow.get(), swordsPhysics.get());
            default -> { }
        }
    }

    // Raycast-selected targets may be behind walls: fall back to a look-based frustum check.
    private Renderer3DCullOptions resolveCullOptions(LivingEntity target) {
        Minecraft mc = Minecraft.getInstance();
        boolean visible = mc.player == null || mc.player.hasLineOfSight(target);
        return visible ? Renderer3DCullOptions.SMART_TARGET : Renderer3DCullOptions.FRUSTUM_LOOK;
    }

    private Supplier<Boolean> onMode(String mode) {
        return () -> mode.equals(espMode.get());
    }

    private boolean usesSecondaryColor() {
        return colors.usesSecondary();
    }

    private int getColorArgb(int count) {
        return colors.argb(count, tracker.renderTarget());
    }
}
