/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.config.common.CommonSettingSchemas;
import dev.loki.lovisual.config.values.BooleanValue;
import dev.loki.lovisual.config.values.EnumValue;
import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.config.values.RGBAColorValue;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;
import dev.loki.lovisual.features.module.WorldPhase;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.helpers.HeadAnchor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Classic conical Chinese hat (rice hat / coolie hat): a wide shallow cone —
 * apex on top, rim circle around the crown — matching the SoupVisuals profile
 * (radius:height ≈ 2:1). Geometry lives in {@link ChinaHatModel}, colors in
 * {@link ChinaHatPalette}; this class only wires settings to rendering.
 */
@ModuleInfo(
        id = "chinahat",
        displayName = "ChinaHat",
        category = ModuleCategory.VISUALS
)
public final class ChinaHat extends Module {

    private final EnumValue<RenderTarget> renderTarget =
            enumMode(
                    "render_target",
                    RenderTarget.BOTH,
                    RenderTarget.values()
            );
    private final BooleanValue useRelations =
            boolCommon(
                    "use_relations",
                    "use_relations",
                    CommonSettingSchemas.PLAYER_USE_RELATIONS,
                    true
            );
    private final RGBAColorValue primaryColor =
            visibleWhen(common(
                    color(
                            "primary_color",
                            "#CCE8C547"
                    ),
                    CommonSettingSchemas.RENDER_PRIMARY_COLOR.commonI18nKey()
            ), () -> !useRelations.get());
    private final EnumValue<ColorMode> colorMode =
            enumCommon(
                    "color_mode",
                    "color_mode",
                    CommonSettingSchemas.RENDER_COLOR_MODE,
                    ColorMode.STATIC,
                    ColorMode.values()
            );
    private final RGBAColorValue secondaryColor =
            visibleWhen(common(
                    color(
                            "secondary_color",
                            "#AA9A7A1A"
                    ),
                    CommonSettingSchemas.RENDER_SECONDARY_COLOR.commonI18nKey()
            ), this::usesSecondaryManualColor);
    private final NumberValue<Integer> colorSpeed =
            numCommon(
                    "color_speed",
                    "color_speed",
                    CommonSettingSchemas.RENDER_COLOR_SPEED,
                    18,
                    2,
                    54
            );
    private final NumberValue<Float> scale =
            numCommon(
                    "scale",
                    "scale",
                    CommonSettingSchemas.RENDER_SCALE,
                    1.15f,
                    0.45f,
                    2.0f
            );
    private final BooleanValue depthTest =
            boolCommon(
                    "depth_test",
                    "depth_test",
                    CommonSettingSchemas.RENDER_DEPTH_TEST,
                    true
            );
    private final RGBAColorValue rimColor =
            visibleWhen(color(
                    "rim_color",
                    "#EEF3D18A"
            ), () -> !useRelations.get());
    private final NumberValue<Integer> segments =
            num(
                    "segments",
                    40,
                    6,
                    96
            );
    private final NumberValue<Float> brimRadius =
            num(
                    "brim_radius",
                    0.5f,
                    0.2f,
                    0.9f
            );
    private final NumberValue<Float> coneHeight =
            num(
                    "cone_height",
                    0.22f,
                    0.05f,
                    0.6f
            );
    private final NumberValue<Float> headTopOffset =
            num(
                    "head_top_offset",
                    0.3f,
                    0.05f,
                    0.5f
            );
    private final NumberValue<Float> headForwardOffset =
            num(
                    "head_forward_offset",
                    0.0f,
                    -0.25f,
                    0.25f
            );
    private final NumberValue<Float> headRightOffset =
            num(
                    "head_right_offset",
                    0.0f,
                    -0.25f,
                    0.25f
            );

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.BEFORE_TRANSLUCENT;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null) return;

        MeshBuilder tris = renderer.batch(
                depthTest.get()
                        ? LoVisualRenderPipelines.WORLD_COLORED_DEPTH
                        : LoVisualRenderPipelines.WORLD_COLORED,
                depthTest.get()
                        ? Renderer3D.DepthMode.PRE_DEPTH
                        : Renderer3D.DepthMode.NONE
        );

        MeshBuilder lines = renderer.batch(
                depthTest.get()
                        ? LoVisualRenderPipelines.WORLD_COLORED_LINES_DEPTH
                        : LoVisualRenderPipelines.WORLD_COLORED_LINES,
                depthTest.get()
                        ? Renderer3D.DepthMode.PRE_DEPTH
                        : Renderer3D.DepthMode.NONE
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

    private void renderAttached(MeshBuilder tris, MeshBuilder lines, Player player, float tickDelta) {
        HeadAnchor anchor = HeadAnchor.of(
                player,
                tickDelta,
                headTopOffset.get(),
                headForwardOffset.get(),
                headRightOffset.get()
        );

        ChinaHatPalette.Palette palette = ChinaHatPalette.resolve(
                player,
                useRelations.get(),
                primaryColor.getArgb(),
                secondaryColor.getArgb(),
                rimColor.getArgb()
        );

        AnimatedRenderColors.Mode mode = animatedColorMode();
        int speed = colorSpeed.get();
        float size = scale.get();

        ChinaHatModel.build(
                tris,
                lines,
                anchor,
                segments.get(),
                brimRadius.get() * size,
                coneHeight.get() * size,
                0.0f,
                0.0f,
                0.0f,
                (index, light, alphaMul) -> ChinaHatPalette.colorAt(palette, mode, speed, index, light, alphaMul),
                (index, light, alphaMul) -> ChinaHatPalette.rimColorAt(palette, mode, speed, index, alphaMul),
                1.0f
        );
    }

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
