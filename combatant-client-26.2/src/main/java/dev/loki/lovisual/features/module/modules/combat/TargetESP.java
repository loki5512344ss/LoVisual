/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.config.common.CommonSettingSchemas;
import dev.loki.lovisual.config.values.BooleanValue;
import dev.loki.lovisual.config.values.ModeValue;
import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.config.values.RGBAColorValue;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;
import dev.loki.lovisual.features.module.WorldPhase;
import dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspCrystals;
import dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspEffects;
import dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspModes;
import dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspCrystals.CrystalInstance;
import dev.loki.lovisual.features.relations.CategoryService;
import dev.loki.lovisual.mixininterface.IEntity;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.TextureStorage;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.util.target.TargetManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

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

    private static final String SETTING_MODE = "mode";
    private static final String SETTING_COLOR = "color";
    private static final String SETTING_COLOR_MODE = "color_mode";
    private static final String SETTING_COLOR_SPEED = "color_speed";
    private static final String SETTING_COLOR_PHASE = "color_phase";
    private static final String SETTING_COLOR_SPREAD = "color_spread";
    private static final String SETTING_COLOR2 = "color2";
    private static final String SETTING_USE_RELATION_COLORS = "use_relation_colors";
    private static final String SETTING_RELATION_COLOR_MIX = "relation_color_mix";
    private static final String SETTING_CROSSHAIR_TARGETS = "crosshair_targets";
    private static final String SETTING_ALPHA_ANIMATION_SPEED = "alpha_animation_speed";
    private static final String SETTING_ESP_LENGTH = "esp_length";
    private static final String SETTING_ESP_FACTOR = "esp_factor";
    private static final String SETTING_ESP_SHAKING = "esp_shaking";
    private static final String SETTING_ESP_AMPLITUDE = "esp_amplitude";
    private static final String SETTING_CAPTURE_SPIN_SPEED = "capture_spin_speed";
    private static final String SETTING_CRYSTAL_HIT_RED = "crystal_hit_red";
    private static final String SETTING_BLACKHOLE_SIZE = "blackhole_size";
    private static final String SETTING_BLACKHOLE_SPIN_SPEED = "blackhole_spin_speed";
    private static final String SETTING_BLACKHOLE_STYLE = "blackhole_style";
    private static final String SETTING_VORTEX_SPEED = "vortex_speed";
    private static final Identifier CRYSTAL_BLOOM = TextureStorage.BLOOM;
    private static final float ALPHA_TICK_DT = 1.0f / 20.0f;
    private static final float ALPHA_SNAP_EPSILON = 0.02f;
    private final Minecraft mc = Minecraft.getInstance();
    private final ModeValue espMode = modeSetting(
            "targetEspMode",
            SETTING_MODE,
            "ghosts",
            "ghosts", "ring", "capture_mark", "crystals", "blackhole", "vortex"
    );
    private final RGBAColorValue colorValue = color("targetEspColor", SETTING_COLOR, "#FFFF5555");
    private final ModeValue colorMode = visibleWhen(modeSetting(
            "targetEspColorMode",
            SETTING_COLOR_MODE,
            "Static",
            "Static", "Rainbow", "LightRainbow", "Sky", "Fade", "DoubleColor", "Analogous", "Theme"
    ), () -> true);
    private final NumberValue<Integer> colorSpeed =
            visibleWhen(num("targetEspColorSpeed", SETTING_COLOR_SPEED, 18, 2, 54), () -> true);
    private final NumberValue<Integer> colorPhase =
            num("targetEspColorPhase", SETTING_COLOR_PHASE, 0, 0, 360);
    private final NumberValue<Integer> colorSpread =
            num("targetEspColorSpread", SETTING_COLOR_SPREAD, 90, -720, 720);
    private final RGBAColorValue colorValue2 =
            visibleWhen(color("targetEspColor2", SETTING_COLOR2, "#FF55FFFF"), this::usesSecondaryColor);
    private final BooleanValue useRelationColors =
            boolCommon("targetEspUseRelationColors", SETTING_USE_RELATION_COLORS, CommonSettingSchemas.PLAYER_USE_RELATIONS, false);
    private final NumberValue<Float> relationColorMix =
            visibleWhen(num("targetEspRelationColorMix", SETTING_RELATION_COLOR_MIX, 1.0f, 0.0f, 1.0f), useRelationColors::get);
    private final BooleanValue crosshairTargets =
            bool("targetEspCrosshairTargets", SETTING_CROSSHAIR_TARGETS, true);
    private final NumberValue<Float> alphaAnimationSpeed =
            num("targetEspAlphaAnimationSpeed", SETTING_ALPHA_ANIMATION_SPEED, 8.0f, 2.0f, 10.0f);
    private final NumberValue<Integer> espLength =
            visibleWhen(num("targetEspLength", SETTING_ESP_LENGTH, 14, 1, 40), this::isGhostMode);
    private final NumberValue<Integer> espFactor =
            visibleWhen(num("targetEspFactor", SETTING_ESP_FACTOR, 8, 1, 20), this::isGhostMode);
    private final NumberValue<Float> espShaking =
            visibleWhen(num("targetEspShaking", SETTING_ESP_SHAKING, 1.8f, 1.5f, 10f), this::isGhostMode);
    private final NumberValue<Float> espAmplitude =
            visibleWhen(num("targetEspAmplitude", SETTING_ESP_AMPLITUDE, 3f, 0.1f, 8f), this::isGhostMode);
    private final NumberValue<Float> captureSpinSpeed =
            visibleWhen(num("targetEspCaptureSpinSpeed", SETTING_CAPTURE_SPIN_SPEED, 1.0f, 0.2f, 4.0f), this::isCaptureMarkMode);
    private final BooleanValue crystalHitRed =
            visibleWhen(bool("targetEspCrystalHitRed", SETTING_CRYSTAL_HIT_RED, true), this::isCrystalsMode);
    private final NumberValue<Float> blackholeSize =
            visibleWhen(num("targetEspBlackholeSize", SETTING_BLACKHOLE_SIZE, 1.1f, 0.5f, 3.0f), this::isBlackholeMode);
    private final NumberValue<Float> blackholeSpinSpeed =
            visibleWhen(num("targetEspBlackholeSpinSpeed", SETTING_BLACKHOLE_SPIN_SPEED, 1.0f, 0.2f, 4.0f), this::isBlackholeMode);
    private final ModeValue blackholeStyle =
            visibleWhen(modeSetting("targetEspBlackholeStyle", SETTING_BLACKHOLE_STYLE, "Single", "Single", "Body"), this::isBlackholeMode);
    private final NumberValue<Float> vortexSpeed =
            visibleWhen(num("targetEspVortexSpeed", SETTING_VORTEX_SPEED, 1.2f, 0.2f, 4.0f), this::isVortexMode);
    private final List<CrystalInstance> crystalList = new ArrayList<>();
    private LivingEntity target;
    private LivingEntity renderTarget;
    private float targetAlpha;
    private Entity lastCrystalTarget;
    private float crystalRotationAngle;

    @Override
    public void onDisable() {
        target = null;
        renderTarget = null;
        targetAlpha = 0.0f;
        crystalList.clear();
        lastCrystalTarget = null;
    }

    @Override
    public void onTick() {
        if (!isEnabled() || mc.level == null) {
            target = null;
            updateRenderTarget(null);
            crystalList.clear();
            lastCrystalTarget = null;
            return;
        }
        target = TargetManager.getTarget(crosshairTargets.get());
        updateRenderTarget(target);
        if (renderTarget == null) {
            crystalList.clear();
            lastCrystalTarget = null;
        }

        setCaptureSpeedScale(isCaptureMarkMode() ? captureSpinSpeed.get() : 1.0f);

        if (renderTarget != null && targetAlpha > 0.0f) {
            tickTargetEsp();
        }
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.AFTER_POST_PROCESS;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        if (!isEnabled() || renderTarget == null || targetAlpha <= 0.001f) return;
        Renderer3D.CullOptions cull = resolveCullOptions(renderTarget);
        if (!Renderer3D.Culling.shouldRender(renderTarget, tickDelta, cull)) return;

        IntFunction<Integer> palette = index -> AnimatedRenderColors.scaleAlpha(getColorArgb(index), targetAlpha);

        String mode = espMode.get();
        if ("ring".equals(mode)) {
            drawOldTargetEsp(renderer, renderTarget, tickDelta, palette);
        } else if ("ghosts".equals(mode)) {
            renderGhosts(
                    renderer,
                    espLength.get(),
                    espFactor.get(),
                    espShaking.get(),
                    espAmplitude.get(),
                    renderTarget,
                    tickDelta,
                    palette
            );
        } else if ("capture_mark".equals(mode)) {
            renderCaptureMark(renderer, renderTarget, tickDelta, palette);
        } else if ("crystals".equals(mode)) {
            renderCrystals(renderer, renderTarget, tickDelta);
        } else if ("blackhole".equals(mode)) {
            renderBlackhole(renderer, renderTarget, tickDelta, palette,
                    blackholeSize.get(), blackholeSpinSpeed.get(), targetAlpha, blackholeStyle.get());
        } else if ("vortex".equals(mode)) {
            renderVortex(renderer, renderTarget, tickDelta, palette, vortexSpeed.get());
        }
    }

    private void updateRenderTarget(LivingEntity next) {
        float dt = Math.max(AnimationUtility.deltaTime(), ALPHA_TICK_DT);
        float speed = alphaAnimationSpeed.get();
        if (next != null) {
            if (renderTarget == null) {
                renderTarget = next;
                targetAlpha = AnimationUtility.snap(
                        AnimationUtility.approach(targetAlpha, 1.0f, dt, speed),
                        1.0f,
                        ALPHA_SNAP_EPSILON
                );
                return;
            }
            if (renderTarget != next) {
                renderTarget = next;
                targetAlpha = 1.0f;
                crystalList.clear();
                lastCrystalTarget = null;
                return;
            }
            targetAlpha = AnimationUtility.snap(
                    AnimationUtility.approach(targetAlpha, 1.0f, dt, speed),
                    1.0f,
                    ALPHA_SNAP_EPSILON
            );
            return;
        }

        targetAlpha = AnimationUtility.snap(
                AnimationUtility.approach(targetAlpha, 0.0f, dt, speed),
                0.0f,
                ALPHA_SNAP_EPSILON
        );
        if (targetAlpha <= 0.0f) {
            renderTarget = null;
            crystalList.clear();
            lastCrystalTarget = null;
        }
    }

    private Renderer3D.CullOptions resolveCullOptions(LivingEntity target) {
        if (target == null || mc.player == null) {
            return Renderer3D.CullOptions.SMART_TARGET;
        }
        // Allow rendering through walls if the current target is already selected,
        // so raycast-based targeting doesn't get culled by line-of-sight checks.
        if (!mc.player.hasLineOfSight(target)) {
            return Renderer3D.CullOptions.FRUSTUM_LOOK;
        }
        return Renderer3D.CullOptions.SMART_TARGET;
    }

    private Vec3 obtainEntityLerpedPos(Entity e, float f) {
        try {
            return e.getPosition(f);
        } catch (NoSuchMethodError ex) {
            if (e instanceof IEntity access) {
                Vec3 last = access.get$InstantRenderPos();
                return last.lerp(e.position(), f);
            }
            return e.position();
        }
    }

    private void renderCrystals(Renderer3D renderer, LivingEntity target, float tickDelta) {
        if (renderer == null || target == null) {
            return;
        }
        if (crystalList.isEmpty() || target != lastCrystalTarget) {
            createCrystals(target);
            lastCrystalTarget = target;
        }
        if (crystalList.isEmpty()) {
            return;
        }

        crystalRotationAngle = (crystalRotationAngle + 0.5f) % 360.0f;
        float orbitYawRad = (float) Math.toRadians(crystalRotationAngle);
        float red = crystalHitRed.get()
                ? Mth.clamp((target.hurtTime - tickDelta) / 20.0f, 0.0f, 1.0f)
                : 0.0f;

        Renderer3D.DepthMode depthMode = Renderer3D.DepthMode.PRE_DEPTH;
        MeshBuilder additiveMesh = renderer.batch(
                LoVisualRenderPipelines.WORLD_COLORED_ADDITIVE_DEPTH,
                depthMode
        );
        MeshBuilder fillMesh = renderer.batch(
                LoVisualRenderPipelines.WORLD_COLORED_DEPTH,
                depthMode
        );
        MeshBuilder lineMesh = renderer.batch(
                LoVisualRenderPipelines.WORLD_COLORED_LINES_DEPTH,
                depthMode
        );
        MeshBuilder bloomMesh = renderer.batchTextured(
                LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE_DEPTH,
                CRYSTAL_BLOOM,
                depthMode
        );
        if (additiveMesh == null || fillMesh == null || lineMesh == null || bloomMesh == null) {
            return;
        }

        float prevWidth = RenderState.lineWidth;
        RenderState.lineWidth = 1.0f;
        try {
            Vec3 targetPos = obtainEntityLerpedPos(target, tickDelta);
            for (int i = 0; i < crystalList.size(); i++) {
                CrystalInstance crystal = crystalList.get(i);
                int baseColor = getCrystalColor(i, red);
                crystal.append(additiveMesh, fillMesh, lineMesh, bloomMesh, targetPos, orbitYawRad, baseColor);
            }
        } finally {
            RenderState.lineWidth = prevWidth;
        }
    }

    private int getCrystalColor(int index, float redFactor) {
        int baseArgb = getColorArgb(index);
        int out;
        if (redFactor <= 0.0f) {
            out = baseArgb;
        } else {
            int redArgb = (baseArgb & 0xFF000000) | 0x00FF0000;
            out = AnimatedRenderColors.mixArgb(baseArgb, redArgb, redFactor);
        }
        return AnimatedRenderColors.scaleAlpha(out, targetAlpha);
    }

    private void createCrystals(Entity target) {
        crystalList.clear();
        if (target == null) {
            return;
        }
        crystalList.add(new CrystalInstance(new Vec3(0, 0.85, 0.8), new Vec3(-49, 0, 40)));
        crystalList.add(new CrystalInstance(new Vec3(0.2, 0.85, -0.675), new Vec3(35, 0, -30)));
        crystalList.add(new CrystalInstance(new Vec3(0.6, 1.35, 0.6), new Vec3(-30, 0, 35)));
        crystalList.add(new CrystalInstance(new Vec3(-0.74, 1.05, 0.4), new Vec3(-25, 0, -30)));
        crystalList.add(new CrystalInstance(new Vec3(0.74, 0.95, -0.4), new Vec3(0, 0, 0)));
        crystalList.add(new CrystalInstance(new Vec3(-0.475, 0.85, -0.375), new Vec3(30, 0, -25)));
        crystalList.add(new CrystalInstance(new Vec3(0, 1.35, -0.6), new Vec3(45, 0, 0)));
        crystalList.add(new CrystalInstance(new Vec3(0.85, 0.7, 0.1), new Vec3(-30, 0, 30)));
        crystalList.add(new CrystalInstance(new Vec3(-0.7, 1.35, -0.3), new Vec3(0, 0, 0)));
        crystalList.add(new CrystalInstance(new Vec3(-0.3, 1.35, 0.55), new Vec3(0, 0, 0)));
        crystalList.add(new CrystalInstance(new Vec3(-0.5, 0.7, 0.7), new Vec3(0, 0, 0)));
        crystalList.add(new CrystalInstance(new Vec3(0.5, 0.7, 0.7), new Vec3(0, 0, 0)));
        crystalList.add(new CrystalInstance(new Vec3(-0.7, 0.75, 0), new Vec3(0, 0, 0)));
        crystalList.add(new CrystalInstance(new Vec3(-0.2, 0.65, -0.7), new Vec3(0, 0, 0)));
    }

    private boolean isGhostMode() {
        return "ghosts".equals(espMode.get());
    }

    private boolean isCaptureMarkMode() {
        return "capture_mark".equals(espMode.get());
    }

    private boolean isCrystalsMode() {
        return "crystals".equals(espMode.get());
    }

    private boolean isBlackholeMode() {
        return "blackhole".equals(espMode.get());
    }

    private boolean isVortexMode() {
        return "vortex".equals(espMode.get());
    }

    private boolean usesSecondaryColor() {
        return AnimatedRenderColors.usesSecondary(animatedColorMode());
    }

    private int getColorArgb(int count) {
        int phase = colorPhase.get() + Math.round(count * colorSpread.get() / 90.0f);
        int animated = AnimatedRenderColors.resolve(
                animatedColorMode(),
                colorSpeed.get(),
                phase,
                colorValue.getArgb(),
                colorValue2.getArgb(),
                true
        );
        if (!useRelationColors.get() || renderTarget == null) {
            return animated;
        }

        int relation = relationColor(renderTarget, animated);
        relation = AnimatedRenderColors.withAlpha(relation, (animated >>> 24) & 0xFF);
        return AnimatedRenderColors.mixArgb(animated, relation, relationColorMix.get());
    }

    private int relationColor(LivingEntity entity, int fallback) {
        if (entity instanceof Player player) {
            return CategoryService.getColor(player);
        }
        String name = entity.getName() != null ? entity.getName().getString() : null;
        if (name == null || name.isBlank()) {
            return fallback;
        }
        return CategoryService.getColor(name);
    }

    private AnimatedRenderColors.Mode animatedColorMode() {
        return switch (colorMode.get()) {
            case "Rainbow" -> AnimatedRenderColors.Mode.RAINBOW;
            case "LightRainbow" -> AnimatedRenderColors.Mode.LIGHT_RAINBOW;
            case "Sky" -> AnimatedRenderColors.Mode.SKY;
            case "Fade" -> AnimatedRenderColors.Mode.FADE;
            case "DoubleColor" -> AnimatedRenderColors.Mode.DOUBLE_COLOR;
            case "Analogous" -> AnimatedRenderColors.Mode.ANALOGOUS;
            case "Theme" -> AnimatedRenderColors.Mode.THEME;
            default -> AnimatedRenderColors.Mode.STATIC;
        };
    }
}
