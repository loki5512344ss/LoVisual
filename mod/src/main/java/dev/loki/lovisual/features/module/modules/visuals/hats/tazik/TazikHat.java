package dev.loki.lovisual.features.module.modules.visuals.hats.tazik;

import dev.loki.lovisual.config.common.CommonSettingSchemas;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.helpers.player.HeadAnchor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Module facade for the Tazik bowl hat. Owns the ClickGui settings, decides
 * which players should wear the hat each frame and adapts settings to
 * {@link TazikHatModel} (geometry) and {@link TazikHatPalette} (colour math).
 */
@ModuleInfo(
        id = "tazikhat",
        displayName = "TazikHat",
        category = ModuleCategory.VISUALS
)
public final class TazikHat extends Module {

    /** Which entities receive the hat. */
    private final EnumValue<RenderTarget> renderTarget = enumMode("render_target", RenderTarget.BOTH, RenderTarget.values());
    /** Tint by player relationship category instead of manual colors. */
    private final BooleanValue useRelations = boolCommon("use_relations", "use_relations", CommonSettingSchemas.PLAYER_USE_RELATIONS, true);
    /** Manual primary bowl color (only when relations are off). */
    private final RGBAColorValue primaryColor = visibleWhen(common(color("primary_color", "#CC17C765"), CommonSettingSchemas.RENDER_PRIMARY_COLOR.commonI18nKey()), () -> !useRelations.get());
    /** Animated colour mode applied around the bowl. */
    private final EnumValue<ColorMode> colorMode = enumCommon("color_mode", "color_mode", CommonSettingSchemas.RENDER_COLOR_MODE, ColorMode.STATIC, ColorMode.values());
    /** Manual secondary color used by animated modes that need two stops. */
    private final RGBAColorValue secondaryColor = visibleWhen(common(color("secondary_color", "#AA079C46"), CommonSettingSchemas.RENDER_SECONDARY_COLOR.commonI18nKey()), this::usesSecondaryManualColor);
    /** Speed multiplier for animated colour modes. */
    private final NumberValue<Integer> colorSpeed = numCommon("color_speed", "color_speed", CommonSettingSchemas.RENDER_COLOR_SPEED, 18, 2, 54);
    /** Overall hat size (multiplied by helmet scale if the target wears a helmet). */
    private final NumberValue<Float> scale = numCommon("scale", "scale", CommonSettingSchemas.RENDER_SCALE, 1.15f, 0.45f, 2.0f);
    /** Toggle depth-tested rendering vs. always-on-top. */
    private final BooleanValue depthTest = boolCommon("depth_test", "depth_test", CommonSettingSchemas.RENDER_DEPTH_TEST, true);
    /** Rim highlight colour. */
    private final RGBAColorValue rimColor = visibleWhen(color("rim_color", "#EE52FFA0"), () -> !useRelations.get());
    /** Size multiplier when the target is wearing a helmet. */
    private final NumberValue<Float> helmetScale = num("helmet_scale", 1.16f, 1.0f, 1.45f);
    /** Vertical offset from the head anchor to the bowl base. */
    private final NumberValue<Float> headTopOffset = num("head_top_offset", 0.255f, 0.05f, 0.45f);
    /** Forward offset from the head anchor. */
    private final NumberValue<Float> headForwardOffset = num("head_forward_offset", 0.0f, -0.25f, 0.25f);
    /** Right offset from the head anchor. */
    private final NumberValue<Float> headRightOffset = num("head_right_offset", 0.0f, -0.25f, 0.25f);
    /** Amplitude of the idle / hit wobble. */
    private final NumberValue<Float> wobbleStrength = num("wobble_strength", 1.0f, 0.0f, 2.5f);

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.BEFORE_TRANSLUCENT;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null) return;

        MeshBuilder tris = renderer.batch(
                depthTest.get() ? LoVisualRenderPipelines.WORLD_COLORED_DEPTH : LoVisualRenderPipelines.WORLD_COLORED,
                depthTest.get() ? Renderer3D.DepthMode.PRE_DEPTH : Renderer3D.DepthMode.NONE
        );
        MeshBuilder lines = renderer.batch(
                depthTest.get() ? LoVisualRenderPipelines.WORLD_COLORED_LINES_DEPTH : LoVisualRenderPipelines.WORLD_COLORED_LINES,
                depthTest.get() ? Renderer3D.DepthMode.PRE_DEPTH : Renderer3D.DepthMode.NONE
        );
        if (tris == null || lines == null) return;

        if (shouldRenderSelf() && !mc.options.getCameraType().isFirstPerson()) {
            renderAttached(tris, lines, mc.player, tickDelta);
        }
        if (shouldRenderPlayers()) {
            for (Player player : mc.level.players()) {
                if (player == null || player == mc.player || player.isRemoved()) continue;
                if (!player.isAlive() || player.getHealth() <= 0.0f) continue;
                renderAttached(tris, lines, player, tickDelta);
            }
        }
    }

    /** Position, colour and draw a single hat for one player. */
    private void renderAttached(MeshBuilder tris, MeshBuilder lines, Player player, float tickDelta) {
        float size = scale.get() * (TazikHatModel.hasHelmet(player) ? helmetScale.get() : 1.0f);
        HeadAnchor anchor = HeadAnchor.of(player, tickDelta, headTopOffset.get(), headForwardOffset.get(), headRightOffset.get());
        TazikHatPalette.Palette palette = TazikHatPalette.resolve(
                player, useRelations.get(),
                primaryColor.getArgb(), secondaryColor.getArgb(), rimColor.getArgb()
        );
        AnimatedRenderColors.Mode mode = animatedColorMode();
        int speed = colorSpeed.get();
        TazikHatModel.Wobble wobble = TazikHatModel.resolveWobble(player, tickDelta, wobbleStrength.get());
        TazikHatModel.build(
                tris, lines, anchor, size, 0.0f, wobble.pitchDeg(), wobble.rollDeg(),
                (i, light, alphaMul) -> TazikHatPalette.colorAt(palette, i, light, alphaMul, mode, speed),
                (i, light, alphaMul) -> TazikHatPalette.rimColorAt(palette, i, alphaMul, mode, speed),
                1.0f, player.getId()
        );
    }

    private boolean shouldRenderSelf() {
        return renderTarget.get() == RenderTarget.SELF || renderTarget.get() == RenderTarget.BOTH;
    }

    private boolean shouldRenderPlayers() {
        return renderTarget.get() == RenderTarget.PLAYERS || renderTarget.get() == RenderTarget.BOTH;
    }

    private boolean usesSecondaryManualColor() {
        if (useRelations.get()) return false;
        return AnimatedRenderColors.usesSecondary(animatedColorMode());
    }

    /** Bridge from the module-local {@link ColorMode} to the shared animated-colour enum. */
    private AnimatedRenderColors.Mode animatedColorMode() {
        return switch (colorMode.get()) {
            case RAINBOW -> AnimatedRenderColors.Mode.RAINBOW;
            case LIGHT_RAINBOW -> AnimatedRenderColors.Mode.LIGHT_RAINBOW;
            case SKY -> AnimatedRenderColors.Mode.SKY;
            case FADE -> AnimatedRenderColors.Mode.FADE;
            case DOUBLE_COLOR -> AnimatedRenderColors.Mode.DOUBLE_COLOR;
            case ANALOGOUS -> AnimatedRenderColors.Mode.ANALOGOUS;
            case THEME -> AnimatedRenderColors.Mode.THEME;
            case STATIC -> AnimatedRenderColors.Mode.STATIC;
        };
    }

    /** Which entities should wear the hat. */
    public enum RenderTarget implements EnumValue.IdProvider {
        SELF("self"),
        PLAYERS("players"),
        BOTH("both");

        private final String id;

        RenderTarget(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

    /** Colour animation style applied around the bowl. */
    public enum ColorMode implements EnumValue.IdProvider {
        STATIC("static"),
        RAINBOW("rainbow"),
        LIGHT_RAINBOW("light_rainbow"),
        SKY("sky"),
        FADE("fade"),
        DOUBLE_COLOR("double_color"),
        ANALOGOUS("analogous"),
        THEME("theme");

        private final String id;

        ColorMode(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }
}
