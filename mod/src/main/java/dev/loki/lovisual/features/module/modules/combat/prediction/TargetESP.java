package dev.loki.lovisual.features.module.modules.combat.prediction;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import dev.loki.lovisual.config.common.CommonSettingSchemas;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspEffects;
import dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspModes;
import dev.loki.lovisual.features.relations.category.CategoryService;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.renderer.Renderer3DCullOptions;
import dev.loki.lovisual.render.engine.renderer.Renderer3DCulling;
import dev.loki.lovisual.util.target.TargetManager;

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
    private static final float ALPHA_TICK_DT = 1.0f / 20.0f;
    private static final float ALPHA_SNAP_EPSILON = 0.02f;
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
    private final TargetEspCrystalRenderer crystalRenderer = new TargetEspCrystalRenderer();
    private LivingEntity target;
    private LivingEntity renderTarget;
    private float targetAlpha;

    @Override
    public void onDisable() {
        target = null;
        renderTarget = null;
        targetAlpha = 0.0f;
        crystalRenderer.clear();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null) {
            target = null;
            updateRenderTarget(null);
            crystalRenderer.clear();
            return;
        }
        target = TargetManager.getTarget(crosshairTargets.get());
        updateRenderTarget(target);
        if (renderTarget == null) {
            crystalRenderer.clear();
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
        Renderer3DCullOptions cull = resolveCullOptions(renderTarget);
        if (!Renderer3DCulling.shouldRender(renderTarget, tickDelta, cull)) return;

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
            crystalRenderer.render(renderer, renderTarget, tickDelta, crystalHitRed.get(), targetAlpha, this::getColorArgb);
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
                crystalRenderer.clear();
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
            crystalRenderer.clear();
        }
    }

    private Renderer3DCullOptions resolveCullOptions(LivingEntity target) {
        Minecraft mc = Minecraft.getInstance();
        if (target == null || mc.player == null) {
            return Renderer3DCullOptions.SMART_TARGET;
        }
        // Allow rendering through walls if the current target is already selected,
        // so raycast-based targeting doesn't get culled by line-of-sight checks.
        if (!mc.player.hasLineOfSight(target)) {
            return Renderer3DCullOptions.FRUSTUM_LOOK;
        }
        return Renderer3DCullOptions.SMART_TARGET;
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
