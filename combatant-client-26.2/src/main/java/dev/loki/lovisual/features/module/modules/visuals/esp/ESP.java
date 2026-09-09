/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.esp;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.config.common.CommonSettingSchemas;
import dev.loki.lovisual.config.values.*;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;
import dev.loki.lovisual.features.module.*;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.render.engine.msaa.MsaaFramebuffer;
import dev.loki.lovisual.render.engine.postprocess.PostProcessManager;
import dev.loki.lovisual.render.engine.postprocess.PostProcessPass;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.features.module.modules.visuals.esp.render.Esp2DRenderer;
import dev.loki.lovisual.features.module.modules.visuals.esp.render.Esp3DRenderer;
import dev.loki.lovisual.features.module.modules.visuals.esp.render.EspSearchOverlay;
import dev.loki.lovisual.features.module.modules.visuals.esp.shader.EspShaderRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.*;

//todo Description
@ModuleInfo(id = "esp", displayName = "ESP", aliases = {"wallhack", "wh", "outline"}, category = ModuleCategory.VISUALS)
public class ESP extends Module {

    static final String MODE_FULL = "Full";
    static final String MODE_CHAMFERED = "Chamfered";
    static final String MODE_CORNERS = "Corners";
    static final String MODE_3D = "3D";
    static final String MODE_SHADER = "Шейдер";
    static final String SHADER_FILL_SOLID = "Solid";
    static final String SHADER_FILL_SMOKE = "Smoke";
    static final String SHADER_COLOR_ENTITY = "Entity";
    public static final String SHADER_COLOR_THEME = "Theme";
    public static final String SHADER_COLOR_CUSTOM = "Custom";
    public static final int OUTLINE_ALPHA = 145;
    public static final int COLOR_ALPHA = 210;
    public static final int HEALTH_ALPHA = 220;
    public static final int ABSORB_RGB = 0x3AA7FF;
    public static final float OUTLINE_EXPAND = 1.5f;
    public static final float OUTLINE_THICKNESS = 1.5f;
    public static final float COLOR_THICKNESS = 2.0f;
    public static final float GRAD_LIGHTEN = 0.30f;
    public static final float GRAD_DARKEN = 0.42f;
    public static final float HEALTH_BAR_WIDTH = 5.0f;
    public static final float HEALTH_BAR_GAP = 8.0f;
    public static final float HEALTH_BAR_INSET = 1.0f;
    public static final double AABB_TOP_OFFSET = 0.18;
    public static final float CORNER_SEGMENT_FRACTION = 0.27f;
    public static final float CHAMFER_FRACTION = 0.16f;
    public static final boolean PIXEL_SNAP = true;
    static final double SHADER_MAX_DISTANCE = 128.0;
    static final float BOX_3D_GRADIENT_SPEED = 0.00115f;

    final BooleanValue renderEntities =
            bool("espRenderEntities", "render_entities", false);
    final RGBColorValue entityColor =
            visibleWhen(colorNoAlpha("espEntityColor", "entity_color", "#78C8FF"), renderEntities::get);
    final ItemIdSetValue searchEntities =
            visibleWhen(itemList("espSearchEntities", "search_entities", TextListSetting.PickerMode.ENTITIES), renderEntities::get);
    final BooleanValue renderSelf =
            bool("espRenderSelf", "render_self", false);
    final BooleanValue onlyVisible =
            bool("espOnlyVisible", "only_visible", true);
    final BooleanValue skipInvisible =
            bool("espSkipInvisible", "skip_invisible", true);
    final BooleanValue onlyArmored =
            bool("espOnlyArmor", "only_armor", true);
    final ModeValue boxMode =
            modeCommon("espBoxMode", "box_mode", CommonSettingSchemas.ESP_BOX_MODE, MODE_FULL, MODE_FULL, MODE_CORNERS, MODE_3D, MODE_CHAMFERED, MODE_SHADER);
    final SetValue shaderChamsEntities =
            visibleWhen(textList("espShaderChamsEntities", "shader_chams_entities",
                    TextListSetting.PickerMode.ENTITIES, Set.of("minecraft:end_crystal")), this::isShaderBox);
    final RGBColorValue shaderChamsEntityColor =
            visibleWhen(colorNoAlpha("espShaderChamsEntityColor", "shader_chams_entity_color", "#B064FF"), this::isShaderBox);
    public final BooleanValue healthBar =
            visibleWhen(boolCommon("espHealthBar", "health_bar_left", CommonSettingSchemas.ESP_HEALTH_BAR, true), () -> !is3DBox() && !isShaderBox());
    public final BooleanValue healthBarGradient =
            visibleWhen(boolCommon("espHealthBarGradient", "health_bar_gradient", CommonSettingSchemas.ESP_HEALTH_BAR_GRADIENT, true), () -> healthBar.get() && !is3DBox() && !isShaderBox());
    public final NumberValue<Integer> healthBarDistance =
            visibleWhen(numCommon("espHealthBarDistance", "health_bar_distance", CommonSettingSchemas.ESP_HEALTH_BAR_DISTANCE, 80, 5, 256), () -> healthBar.get() && !is3DBox() && !isShaderBox());
    public final NumberValue<Float> box3dLineWidth =
            visibleWhen(numCommon("esp3dLineWidth", "line_width", CommonSettingSchemas.ESP_LINE_WIDTH, 1.5f, 0.5f, 6.0f), this::is3DBox);
    public final NumberValue<Float> box3dGradientStrength =
            visibleWhen(num("esp3dGradientStrength", "gradient_strength", 1.35f, 0.0f, 2.5f), this::is3DBox);
    public final NumberValue<Float> box3dFillAlpha =
            visibleWhen(num("esp3dFillAlpha", "fill_alpha", 0.30f, 0.0f, 1.0f), this::is3DBox);
    public final NumberValue<Float> box3dOutlineAlpha =
            visibleWhen(num("esp3dOutlineAlpha", "outline_alpha", 1.0f, 0.0f, 1.0f), this::is3DBox);
    public final BooleanValue shaderGlow =
            visibleWhen(bool("espShaderGlowEnabled", "shader_glow_enabled", true), this::isShaderBox);
    public final NumberValue<Float> shaderGlowStrength =
            visibleWhen(num("espShaderGlowStrength", "shader_glow_strength", 8.0f, 1.0f, 48.0f), () -> isShaderBox() && shaderGlow.get());
    public final NumberValue<Float> shaderGlowAlpha =
            visibleWhen(num("espShaderGlowAlpha", "shader_glow_alpha", 0.72f, 0.0f, 1.0f), () -> isShaderBox() && shaderGlow.get());
    public final NumberValue<Float> shaderGlowIntensity =
            visibleWhen(num("espShaderGlowIntensity", "shader_glow_intensity", 1.25f, 0.10f, 4.0f), () -> isShaderBox() && shaderGlow.get());
    public final ModeValue shaderGlowColorMode =
            visibleWhen(mode("espShaderGlowColorMode", "shader_glow_color_mode", SHADER_COLOR_ENTITY,
                            SHADER_COLOR_ENTITY, SHADER_COLOR_THEME, SHADER_COLOR_CUSTOM),
                    () -> isShaderBox() && shaderGlow.get());
    public final RGBColorValue shaderGlowColor =
            visibleWhen(colorNoAlpha("espShaderGlowColor", "shader_glow_color", "#78C8FF"),
                    () -> isShaderBox() && shaderGlow.get() && isCustomShaderColor(shaderGlowColorMode));
    public final BooleanValue shaderOutline =
            visibleWhen(bool("espShaderOutlineEnabled", "shader_outline_enabled", true), this::isShaderBox);
    public final NumberValue<Float> shaderOutlineWidth =
            visibleWhen(num("espShaderOutlineWidth", "shader_outline_width", 2.0f, 0.5f, 18.0f), () -> isShaderBox() && shaderOutline.get());
    public final NumberValue<Float> shaderOutlineAlpha =
            visibleWhen(num("espShaderOutlineAlpha", "shader_outline_alpha", 0.92f, 0.0f, 1.0f), () -> isShaderBox() && shaderOutline.get());
    public final NumberValue<Float> shaderOutlineIntensity =
            visibleWhen(num("espShaderOutlineIntensity", "shader_outline_intensity", 1.15f, 0.10f, 4.0f), () -> isShaderBox() && shaderOutline.get());
    public final ModeValue shaderShadowColorMode =
            visibleWhen(mode("espShaderShadowColorMode", "shader_shadow_color_mode", SHADER_COLOR_ENTITY,
                            SHADER_COLOR_ENTITY, SHADER_COLOR_THEME, SHADER_COLOR_CUSTOM),
                    () -> isShaderBox() && shaderOutline.get());
    public final RGBColorValue shaderShadowColor =
            visibleWhen(colorNoAlpha("espShaderShadowColor", "shader_shadow_color", "#78C8FF"),
                    () -> isShaderBox() && shaderOutline.get() && isCustomShaderColor(shaderShadowColorMode));
    public final BooleanValue shaderFill =
            visibleWhen(bool("espShaderFillEnabled", "shader_fill_enabled", true), this::isShaderBox);
    final ModeValue shaderFillStyle =
            visibleWhen(mode("espShaderFillStyle", "shader_fill_style", SHADER_FILL_SOLID, SHADER_FILL_SOLID, SHADER_FILL_SMOKE),
                    () -> isShaderBox() && shaderFill.get());
    public final NumberValue<Float> shaderSmokeScale =
            visibleWhen(num("espShaderSmokeScale", "shader_smoke_scale", 2.60f, 0.35f, 8.0f),
                    () -> isShaderBox() && shaderFill.get() && isShaderSmokeFill());
    public final NumberValue<Float> shaderSmokeSpeed =
            visibleWhen(num("espShaderSmokeSpeed", "shader_smoke_speed", 0.55f, 0.0f, 3.0f),
                    () -> isShaderBox() && shaderFill.get() && isShaderSmokeFill());
    public final NumberValue<Integer> shaderSmokeOctaves =
            visibleWhen(num("espShaderSmokeOctaves", "shader_smoke_octaves", 4, 1, 6),
                    () -> isShaderBox() && shaderFill.get() && isShaderSmokeFill());
    public final NumberValue<Float> shaderSmokeContrast =
            visibleWhen(num("espShaderSmokeContrast", "shader_smoke_contrast", 1.20f, 0.35f, 3.0f),
                    () -> isShaderBox() && shaderFill.get() && isShaderSmokeFill());
    public final NumberValue<Float> shaderFillAlpha =
            visibleWhen(num("espShaderFillAlpha", "shader_fill_alpha", 0.41f, 0.0f, 1.0f),
                    () -> isShaderBox() && shaderFill.get());
    public final ModeValue shaderFillColorMode =
            visibleWhen(mode("espShaderFillColorMode", "shader_fill_color_mode", SHADER_COLOR_ENTITY,
                            SHADER_COLOR_ENTITY, SHADER_COLOR_THEME, SHADER_COLOR_CUSTOM),
                    () -> isShaderBox() && shaderFill.get());
    public final RGBColorValue shaderFillColor =
            visibleWhen(colorNoAlpha("espShaderFillColor", "shader_fill_color", "#78C8FF"),
                    () -> isShaderBox() && shaderFill.get() && isCustomShaderColor(shaderFillColorMode));
    public final NumberValue<Float> shaderDarkMultiplier =
            visibleWhen(num("espShaderDarkMultiplier", "shader_dark_multiplier", 0.55f, 0.0f, 1.5f), this::isShaderBox);
    private final PostProcessPass shaderEspPass = new ShaderEspPass();
    public final SubmitNodeStorage shaderEspCommandQueue = new SubmitNodeStorage();
    public TextureTarget shaderMask;
    public TextureTarget shaderBlurBuffer;
    public TextureTarget shaderEffectBuffer;
    public MsaaFramebuffer shaderMaskMsaa;
    public FeatureRenderDispatcher shaderEspRenderDispatcher;
    public RenderBuffers shaderEspRenderBuffers;
    public int shaderBufferW = -1;
    public int shaderBufferH = -1;
    public int shaderMaskSamples;

    {
        PostProcessManager.register(shaderEspPass);
    }

    @Override
    public HudPhase getHudPhase() {
        return HudPhase.BEFORE_MISC_OVERLAYS;
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.END_MAIN;
    }

    @Override
    public void onRenderHudEngine(Renderer2D renderer, TextRenderer textRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null) return;

        Vec3 camPos = mc.gameRenderer.mainCamera().position();

        if (!is3DBox() && !isShaderBox()) {
            List<EspModel.RenderEntry> playerEntries = EspEntityCollector.collectPlayerEntries(this, camPos);
            Esp2DRenderer.renderPlayerEntries(this, renderer, tickDelta, playerEntries);
        }

        EspSearchOverlay.render(this, renderer, textRenderer, tickDelta, camPos);
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Esp3DRenderer.render(this, renderer, tickDelta);
    }

    @Override
    public void onDisable() {
        if (shaderMaskMsaa != null) {
            shaderMaskMsaa.destroyBuffers();
            shaderMaskMsaa = null;
        }
        EspShaderRenderer.closeDispatcher(this);
        shaderMaskSamples = 0;
    }

    public boolean shouldLiftNameTagsSection(Player player) {
        return isEnabled() && isNameTagsLiftBoxMode() && EspEntityCollector.shouldRenderPlayer(this, player);
    }

    private boolean isNameTagsLiftBoxMode() {
        return MODE_FULL.equalsIgnoreCase(boxMode.get())
                || MODE_CORNERS.equalsIgnoreCase(boxMode.get())
                || MODE_CHAMFERED.equalsIgnoreCase(boxMode.get());
    }

    public boolean isShaderSmokeFill() {
        return SHADER_FILL_SMOKE.equalsIgnoreCase(shaderFillStyle.get());
    }

    private boolean isCustomShaderColor(ModeValue mode) {
        return mode != null && SHADER_COLOR_CUSTOM.equalsIgnoreCase(mode.get());
    }

    public boolean isCornerBox() {
        return MODE_CORNERS.equalsIgnoreCase(boxMode.get());
    }

    public boolean isChamferedBox() {
        return MODE_CHAMFERED.equalsIgnoreCase(boxMode.get());
    }

    public boolean is3DBox() {
        return MODE_3D.equalsIgnoreCase(boxMode.get());
    }

    public boolean isShaderBox() {
        return MODE_SHADER.equalsIgnoreCase(boxMode.get());
    }

    private final class ShaderEspPass implements PostProcessPass {
        @Override
        public boolean isActive() {
            Minecraft mc = Minecraft.getInstance();
            return ESP.this.isEnabled()
                    && ESP.this.isShaderBox()
                    && mc.player != null
                    && mc.level != null;
        }

        @Override
        public Phase getPhase() {
            return Phase.PRE_HAND;
        }

        @Override
        public boolean render(GpuTextureView src, GpuTextureView dst, float tickDelta) {
            return EspShaderRenderer.render(ESP.this, src, dst, tickDelta);
        }
    }
}
