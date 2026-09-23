/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland;

import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.features.gui.clickgui.screen.ClickGuiScreen;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.hud.base.AbstractHudElement;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.features.gui.hud.anchor.HudRenderSpace;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland.IslandBoundsPatches;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland.IslandRuntimePatches;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland.controls.IslandUiUtil;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland.controls.IslandControls;
import dev.loki.lovisual.features.gui.hud.script.HudScriptLayouts;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.core.frame.RenderPhase;
import dev.loki.lovisual.render.engine.core.frame.RenderPhaseScope;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.FullScreenRenderer;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.runtime.UiRuntime;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiProjectionMode;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiRenderContext;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime.CachedUiScriptRuntime;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModule;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModuleHandle;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.render.UiScriptProps;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.Lifecycle;
import dev.loki.lovisual.util.media.session.MediaSessionService;
import dev.loki.lovisual.util.pvp.state.PvpState;

import java.time.LocalTime;
import java.util.List;

import static dev.loki.lovisual.features.theme.Theme.theme;

/**
 * Dynamic Island: оркестрация режимов (time/pvp/music/clickgui) и анимаций.
 * Патчинг UI-скрипта, bounds-патчи, утилиты и геометрия медиа-контролов
 * делегированы в {@code impl/dynamicisland/}.
 */
@HudElementRegister(order = 60)
public final class DynamicIsland extends AbstractHudElement {
    private static final String LAYOUT_ID = "lovisual:api/hud/static/dynamic_island";
    public static final DynamicIsland INSTANCE = new DynamicIsland();

    private static final float TOP_Y = 18f;
    private static final float CLOSED_HEIGHT = 35f;
    private static final float EXPANDED_HEIGHT = 98f;
    private static final float CLOSED_RADIUS = 17f;
    private static final float EXPANDED_RADIUS = 20f;

    private static final float TIME_WIDTH = 74f;
    private static final float PVP_MIN_WIDTH = 68f;
    private static final float PVP_MAX_WIDTH = 96f;
    private static final float MUSIC_EXPANDED_WIDTH = 338f;

    private static final float TIME_CONTEXT_WIDTH = 57f;

    private static final float COMPACT_PAD_X = 11f;
    private static final float COMPACT_GAP = 9f;
    private static final float COMPACT_ART = 22f;
    private static final float COMPACT_WAVE_W = 15f;
    private static final float COMPACT_FADE = 15f;
    private static final float EXPANDED_PAD_Y = 11f;
    private static final float PROGRESS_H = 5f;
    private static final float CONTROL_TIME_GAP = 10f;

    private static final int ICON_PAUSE = 0xEA02;
    private static final int ICON_PLAY = 0xEA03;
    private static final int ICON_PREV = 0xEA04;
    private static final int ICON_NEXT = 0xEA05;
    private static final int ICON_SHUFFLE = 0xEA06;

    private static final int STATUS_PVP = 0xFFFF6368;
    private static final int PROGRESS_BG = 0x2AFFFFFF;

    private final MediaSessionService mediaService = MediaSessionService.get();
    private final BooleanValue syncTheme = new BooleanValue("dynamic_island_sync_theme", true);
    private final NumberValue<Integer> themeAlpha = new NumberValue<>("dynamic_island_theme_alpha", 200, 0, 255);
    private final BooleanValue blur = new BooleanValue("dynamic_island_blur", false);
    private final NumberValue<Integer> blurAlpha = new NumberValue<>("dynamic_island_blur_alpha", 120, 0, 255);
    private final RGBAColorValue bgColor = new RGBAColorValue("dynamic_island_bg", "#E614171D");
    private final RGBColorValue accentColor = new RGBColorValue("dynamic_island_accent", "#6E8DFF");
    private final RGBColorValue textColor = new RGBColorValue("dynamic_island_text", "#F5F7FA");
    private final RGBColorValue mutedColor = new RGBColorValue("dynamic_island_muted", "#A9B1BC");

    private final UiScriptModuleHandle moduleHandle = HudScriptLayouts.handle(LAYOUT_ID);
    private final CachedUiScriptRuntime scriptRuntime = new CachedUiScriptRuntime(HudScriptLayouts.runtimeReporter());
    private final IslandControls controls = new IslandControls();

    private float visibilityAnim;
    private float expandAnim;
    private float widthAnim = -1f;
    private float mainWidthAnim = -1f;
    private float leftContextWidthAnim;
    private float rightContextWidthAnim;
    private float heightAnim = -1f;
    private boolean expanded;
    private boolean clickGuiShellVisible;
    private String titleScrollKey = "";
    private long titleScrollStartMs;

    private IslandMode currentMode = IslandMode.TIME;
    private MediaSessionService.Snapshot currentSnapshot = MediaSessionService.Snapshot.empty(MediaSessionService.isMediaAvailable());

    private int uiFillTop;
    private int uiFillBottom;
    private int uiTextPrimary;
    private int uiTextSecondary;
    private int uiTextMuted;
    private int uiAccent;
    private int uiAccentSoft;

    private DynamicIsland() {
        super("dynamic_island", "Dynamic Island", true);
    }

    public static boolean shouldRenderInScreenOverlay(Screen screen) {
        return INSTANCE != null && INSTANCE.isScreenOverlayAllowed(screen);
    }

    public static void renderScreenOverlay(GuiGraphicsExtractor ctx, float tickDelta) {
        INSTANCE.renderScreenOverlayInternal(ctx, tickDelta);
    }

    public static boolean shouldOwnClickGuiTabShell() {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return false;
        return INSTANCE.clickGuiShellVisible;
    }

    @Override
    protected void defineSettings(List<SettingDef> defs) {
        defs.add(SettingDef.bool(syncTheme));
        defs.add(SettingDef.number(themeAlpha).visibleWhen(syncTheme::get));
        defs.add(SettingDef.bool(blur));
        defs.add(SettingDef.number(blurAlpha).visibleWhen(blur::get));
        defs.add(SettingDef.color(bgColor).visibleWhen(() -> !syncTheme.get()));
        defs.add(SettingDef.colorNoAlpha(accentColor).visibleWhen(() -> !syncTheme.get()));
        defs.add(SettingDef.colorNoAlpha(textColor).visibleWhen(() -> !syncTheme.get()));
        defs.add(SettingDef.colorNoAlpha(mutedColor).visibleWhen(() -> !syncTheme.get()));
    }

    @Override
    public boolean usesEngineRenderer() {
        return true;
    }

    @Override
    public HudRenderSpace getRenderSpace() {
        return HudRenderSpace.UNSCALED_LOGICAL;
    }

    @Override
    public HudPhase getHudPhase() {
        return HudPhase.AFTER_SUBTITLES;
    }

    @Override
    public int getRenderOrder() {
        return 40;
    }

    @Override
    public boolean isMouseOverInteractive(float mx, float my) {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && isInteractiveScreen(ClientScreen.current()) && contains(mx, my);
    }

    @Override
    public boolean onMouseClicked(float mx, float my, int button) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || !isInteractiveScreen(ClientScreen.current())) return false;
        if (!contains(mx, my)) return false;

        if (currentMode == IslandMode.MUSIC && expandAnim > 0.01f && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (currentSnapshot != null && currentSnapshot.supportsShuffle()
                    && IslandUiUtil.hit(mx, my, controls.shuffleX, controls.shuffleY, controls.controlSize, controls.controlSize)) {
                mediaService.toggleShuffle();
                return true;
            }
            if (IslandUiUtil.hit(mx, my, controls.prevX, controls.prevY, controls.controlSize, controls.controlSize)) {
                mediaService.previous();
                return true;
            }
            if (IslandUiUtil.hit(mx, my, controls.playX, controls.playY, controls.controlSize, controls.controlSize)) {
                mediaService.playPause();
                return true;
            }
            if (IslandUiUtil.hit(mx, my, controls.nextX, controls.nextY, controls.controlSize, controls.controlSize)) {
                mediaService.next();
                return true;
            }
            if (currentSnapshot != null && currentSnapshot.supportsRepeat()
                    && IslandUiUtil.hit(mx, my, controls.repeatX, controls.repeatY, controls.controlSize, controls.controlSize)) {
                mediaService.cycleRepeatMode();
                return true;
            }
        }

        if (currentMode == IslandMode.MUSIC
                && (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            expanded = !expanded;
            return true;
        }
        return false;
    }

    @Override
    public void renderEngine(Renderer2D renderer,
                             TextRenderer textRenderer,
                             GuiGraphicsExtractor ctx,
                             float tickDelta,
                             int screenW,
                             int screenH) {
        renderEngineInternal(renderer, textRenderer, ctx, tickDelta, screenW, screenH, false);
    }

    private void renderScreenOverlayInternal(GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!Lifecycle.canRunHud()) return;
        if (mc == null || mc.getWindow() == null || ctx == null) return;
        Screen currentScreen = ClientScreen.current();
        boolean clickGuiOverlay = isEnabled() && IslandUiUtil.clickGuiEnabled();
        if (!clickGuiOverlay && !isScreenOverlayAllowed(currentScreen)) return;

        FullScreenRenderer.ensureInit();
        int screenW = Math.max(1, Math.round(HudScale.virtualWidth(
                mc.getWindow().getWidth(),
                mc.getWindow().getHeight()
        )));
        int screenH = Math.max(1, Math.round(HudScale.virtualHeight(
                mc.getWindow().getWidth(),
                mc.getWindow().getHeight()
        )));

        LoVisualRenderSystem.updateFrameTiming(tickDelta, tickDelta, tickDelta);
        ViewportContext.beginUnscaledLogical(ctx);
        try (RenderPhaseScope ignored = LoVisualRenderSystem.phase(RenderPhase.SCREEN_TOP, "2d:screen:dynamic_island")) {
            Renderer2D.COLOR.begin();
            renderEngineInternal(Renderer2D.COLOR, TextRenderer.get(), ctx, tickDelta, screenW, screenH, true);
            Renderer2D.COLOR.render();
        } finally {
            ViewportContext.end(ctx);
        }
    }

    private void renderEngineInternal(Renderer2D renderer,
                                      TextRenderer textRenderer,
                                      GuiGraphicsExtractor ctx,
                                      float tickDelta,
                                      int screenW,
                                      int screenH,
                                      boolean screenOverlayPass) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        HudScriptLayouts.pollReloadCombo(mc);
        if (moduleHandle.consumeChanged()) {
            resetScriptRuntime();
        }

        Screen currentScreen = ClientScreen.current();
        boolean chatOpen = currentScreen instanceof ChatScreen;
        boolean clickGuiVisible = IslandUiUtil.clickGuiEnabled();
        boolean clickGuiTabShellScreen = currentScreen instanceof ClickGuiScreen;
        ClickGuiRenderer.ClickGuiIslandState clickGuiState = ClickGuiRenderer.islandState();
        boolean clickGuiBridge = isEnabled()
                && screenOverlayPass
                && clickGuiVisible
                && clickGuiTabShellScreen
                && clickGuiState.lifecycle() > 0.001f
                && clickGuiState.tabBarW() > 1.0f;
        if (clickGuiVisible && clickGuiTabShellScreen && !clickGuiBridge) {
            clickGuiShellVisible = false;
            setBounds(0f, 0f, 0f, 0f);
            controls.reset();
            return;
        }
        boolean shouldShow = clickGuiBridge
                || (isEnabled() && (screenOverlayPass ? isScreenOverlayAllowed(currentScreen) : currentScreen == null || chatOpen));
        currentSnapshot = mediaService.snapshot();
        currentMode = clickGuiBridge ? IslandMode.CLICKGUI : resolveMode(currentSnapshot);

        if (!isInteractiveScreen(currentScreen) || currentMode != IslandMode.MUSIC) {
            expanded = false;
        }

        float dt = AnimationUtility.deltaTime();
        visibilityAnim = AnimationUtility.approach(visibilityAnim, shouldShow ? 1f : 0f, dt, 10f);
        expandAnim = AnimationUtility.approach(expandAnim, expanded ? 1f : 0f, dt, 10f);
        visibilityAnim = AnimationUtility.snap(visibilityAnim, shouldShow ? 1f : 0f, 0.002f);
        expandAnim = AnimationUtility.snap(expandAnim, expanded ? 1f : 0f, 0.002f);
        clickGuiShellVisible = clickGuiBridge && visibilityAnim > 0.01f;

        if (!shouldShow && visibilityAnim <= 0.01f) {
            clickGuiShellVisible = false;
            setBounds(0f, 0f, 0f, 0f);
            controls.reset();
            return;
        }

        updatePalette();
        TextRenderer fallback = textRenderer != null ? textRenderer : TextRenderer.get();
        TextRenderer titleRenderer = Fonts.renderer("InterMedium", FontInfo.Type.Regular, fallback);
        TextRenderer valueRenderer = Fonts.renderer("Inter", FontInfo.Type.Bold, fallback);
        TextRenderer metaRenderer = Fonts.renderer("Inter", FontInfo.Type.Regular, fallback);

        IslandMetrics metrics = measureMetrics(titleRenderer, valueRenderer, metaRenderer);
        float targetMainWidth = switch (currentMode) {
            case TIME -> TIME_WIDTH;
            case PVP -> IslandUiUtil.clamp(metrics.pvpWidth, PVP_MIN_WIDTH, PVP_MAX_WIDTH);
            case MUSIC -> AnimationUtility.lerp(metrics.musicCompactWidth, MUSIC_EXPANDED_WIDTH, expandAnim);
            case CLICKGUI -> clickGuiState.tabBarW();
        };
        float targetHeight = currentMode == IslandMode.CLICKGUI
                ? clickGuiState.tabBarH()
                : AnimationUtility.lerp(CLOSED_HEIGHT, currentMode == IslandMode.MUSIC ? EXPANDED_HEIGHT : CLOSED_HEIGHT, expandAnim);

        boolean musicContext = currentMode == IslandMode.MUSIC;
        boolean clickGuiContext = currentMode == IslandMode.CLICKGUI;
        float contextPresence = musicContext ? 1.0f - AnimationUtility.easeOutCubic(expandAnim) : (clickGuiContext ? 1.0f : 0.0f);
        float targetLeftContextWidth = TIME_CONTEXT_WIDTH * contextPresence;
        float targetRightContextWidth = PvpState.isActive() ? IslandBoundsPatches.PVP_CONTEXT_WIDTH * contextPresence : 0.0f;

        mainWidthAnim = mainWidthAnim < 0f ? targetMainWidth : AnimationUtility.approach(mainWidthAnim, targetMainWidth, dt, 12f);
        heightAnim = heightAnim < 0f ? targetHeight : AnimationUtility.approach(heightAnim, targetHeight, dt, 12f);
        leftContextWidthAnim = AnimationUtility.approach(leftContextWidthAnim, targetLeftContextWidth, dt, 13f);
        rightContextWidthAnim = AnimationUtility.approach(rightContextWidthAnim, targetRightContextWidth, dt, 13f);
        mainWidthAnim = AnimationUtility.snap(mainWidthAnim, targetMainWidth, 0.05f);
        heightAnim = AnimationUtility.snap(heightAnim, targetHeight, 0.05f);
        leftContextWidthAnim = AnimationUtility.snap(leftContextWidthAnim, targetLeftContextWidth, 0.05f);
        rightContextWidthAnim = AnimationUtility.snap(rightContextWidthAnim, targetRightContextWidth, 0.05f);

        float drawMainWidth = mainWidthAnim;
        float drawHeight = heightAnim;
        float leftOccupied = leftContextWidthAnim > 0.5f ? leftContextWidthAnim + IslandBoundsPatches.CONTEXT_GAP : 0.0f;
        float rightOccupied = rightContextWidthAnim > 0.5f ? IslandBoundsPatches.CONTEXT_GAP + rightContextWidthAnim : 0.0f;
        float drawWidth = leftOccupied + drawMainWidth + rightOccupied;
        widthAnim = drawWidth;
        float drawX = currentMode == IslandMode.CLICKGUI
                ? clickGuiState.tabBarX() - leftOccupied
                : (screenW - drawMainWidth) * 0.5f - leftOccupied;
        float drawY = currentMode == IslandMode.CLICKGUI
                ? clickGuiState.tabBarY() - (1.0f - AnimationUtility.easeOutCubic(clickGuiState.lifecycle())) * 18.0f
                : TOP_Y + IslandUiUtil.bossBarOffset(mc, screenH, TOP_Y);
        float mainX = leftOccupied;
        float alpha = AnimationUtility.easeOutCubic(visibilityAnim);
        float radius = currentMode == IslandMode.CLICKGUI ? drawHeight * 0.5f : AnimationUtility.lerp(CLOSED_RADIUS, EXPANDED_RADIUS, expandAnim);
        int accent = accentFor(currentMode);

        setBounds(drawX, drawY, drawWidth, drawHeight);

        float expandedContentAlpha = currentMode == IslandMode.MUSIC ? AnimationUtility.easeOutCubic(expandAnim) * alpha : 0f;
        float compactContentAlpha = currentMode == IslandMode.MUSIC ? (1f - AnimationUtility.easeOutCubic(expandAnim)) * alpha : 0f;
        if (expandedContentAlpha > 0.01f) {
            controls.updateLayout(drawX + mainX, drawY, drawMainWidth, valueRenderer, currentSnapshot);
            controls.updateHover(isInteractiveScreen(currentScreen), mc, currentSnapshot);
        } else {
            controls.reset();
        }

        if (!renderScripted(renderer, fallback, titleRenderer, metaRenderer, ctx, tickDelta, screenW, screenH, drawWidth, drawHeight,
                drawMainWidth, mainX, leftContextWidthAnim, rightContextWidthAnim,
                alpha, compactContentAlpha, expandedContentAlpha, radius, accent)) {
            IslandUiUtil.drawFallback(renderer, drawX + mainX, drawY, drawMainWidth, drawHeight, radius, alpha, accent, uiFillTop);
        }
    }

    private boolean renderScripted(Renderer2D renderer,
                                   TextRenderer textRenderer,
                                   TextRenderer titleRenderer,
                                   TextRenderer metaRenderer,
                                   GuiGraphicsExtractor ctx,
                                   float tickDelta,
                                   int screenW,
                                   int screenH,
                                   float width,
                                   float height,
                                   float mainWidth,
                                   float mainX,
                                   float leftContextWidth,
                                   float rightContextWidth,
                                   float alpha,
                                   float compactAlpha,
                                   float expandedAlpha,
                                   float radius,
                                   int accent) {
        if (moduleHandle.isRuntimeBlocked()) return false;

        UiScriptModule loaded = ensureModule();
        if (loaded == null) return false;

        UiScriptProps props = buildProps(
                screenW, screenH, width, height, mainWidth, mainX, leftContextWidth, rightContextWidth,
                alpha, compactAlpha, expandedAlpha, radius, accent,
                titleRenderer, metaRenderer
        );
        long treeSignature = IslandRuntimePatches.islandStructuralSignature(props);
        long dataSignature = CachedUiScriptRuntime.signature(props.asMap());
        long layoutSignature = treeSignature;
        UiRuntime baked = scriptRuntime.bake(
                moduleHandle,
                loaded,
                "dynamic_island",
                treeSignature,
                dataSignature,
                layoutSignature,
                width,
                height,
                textRenderer,
                x,
                y,
                width,
                height,
                props::asMap,
                () -> IslandRuntimePatches.islandRuntimePatches(props),
                () -> IslandBoundsPatches.islandBoundsPatches(props, x, y, controls)
        );
        if (baked == null) return false;
        baked.render(new UiRenderContext(renderer, textRenderer, ctx, tickDelta, UiProjectionMode.UNSCALED_LOGICAL));
        return true;
    }

    private UiScriptProps buildProps(int screenW,
                                                                int screenH,
                                                                float width,
                                                                float height,
                                                                float mainWidth,
                                                                float mainX,
                                                                float leftContextWidth,
                                                                float rightContextWidth,
                                                                float alpha,
                                                                float compactAlpha,
                                                                float expandedAlpha,
                                                                float radius,
                                                                int accent,
                                                                TextRenderer titleRenderer,
                                                                TextRenderer metaRenderer) {
        UiScriptProps props = UiScriptProps.create(80);
        String title = currentSnapshot != null ? currentSnapshot.title() : "";
        String artist = currentSnapshot != null ? currentSnapshot.artist() : "";
        long duration = currentSnapshot != null ? currentSnapshot.durationSeconds() : 0L;
        props.put("screenW", screenW);
        props.put("screenH", screenH);
        props.put("width", width);
        props.put("height", height);
        props.put("mainWidth", mainWidth);
        props.put("mainX", mainX);
        props.put("leftContextWidth", leftContextWidth);
        props.put("rightContextWidth", rightContextWidth);
        props.put("contextGap", IslandBoundsPatches.CONTEXT_GAP);
        boolean pvpContextVisible = (currentMode == IslandMode.MUSIC || currentMode == IslandMode.CLICKGUI) && PvpState.isActive() && rightContextWidth > 0.5f;
        props.put("pvpContextVisible", pvpContextVisible);
        props.put("contextAlpha", currentMode == IslandMode.MUSIC ? compactAlpha : (currentMode == IslandMode.CLICKGUI ? alpha : 0.0f));
        props.put("mode", currentMode.name().toLowerCase());
        props.put("alpha", alpha);
        props.put("compactAlpha", compactAlpha);
        props.put("expandedAlpha", expandedAlpha);
        props.put("expand", expandAnim);
        props.put("expanded", expanded);
        props.put("blur", blur.get());
        props.put("blurAlpha", blurAlpha.get() / 255.0f);
        props.put("radius", radius);
        props.put("time", IslandUiUtil.formatClock(LocalTime.now()));
        props.put("pvpTimer", pvpContextVisible || currentMode == IslandMode.PVP ? IslandUiUtil.formatSecondsOnly(predictedPvpSeconds()) : "");
        ClickGuiRenderer.ClickGuiIslandState clickGuiState = ClickGuiRenderer.islandState();
        props.put("clickGuiLabel", clickGuiState.activeLabel());
        props.put("clickGuiTabCount", clickGuiState.tabCount());
        props.put("clickGuiPicker", clickGuiState.pickerActive());
        props.put("title", title);
        props.put("artist", artist);
        props.put("titleWidthCompact", IslandUiUtil.measureWidth(titleRenderer, title, 0.82f));
        props.put("titleWidthExpanded", IslandUiUtil.measureWidth(titleRenderer, title, 0.86f));
        props.put("artistWidthExpanded", IslandUiUtil.measureWidth(metaRenderer, artist, 0.72f));
        props.put("titleScrollTime", titleScrollSeconds(title, artist, duration));
        props.put("elapsed", IslandUiUtil.formatShortTime(currentSnapshot != null ? currentSnapshot.predictedPositionSeconds() : 0L));
        props.put("total", IslandUiUtil.formatShortTime(currentSnapshot != null ? currentSnapshot.durationSeconds() : 0L));
        props.put("progress", currentSnapshot != null && currentSnapshot.durationSeconds() > 0
                ? AnimationUtility.clamp((float) currentSnapshot.predictedPositionSeconds() / (float) currentSnapshot.durationSeconds(), 0f, 1f)
                : 0f);
        props.put("playing", currentSnapshot != null && currentSnapshot.isPlaying());
        Identifier artwork = currentSnapshot != null ? currentSnapshot.artworkTexture() : null;
        props.put("artworkTexture", artwork != null ? artwork.toString() : "");
        props.put("fillTop", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(uiFillTop, 0.88f * alpha)));
        props.put("fillBottom", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(uiFillBottom, 0.88f * alpha)));
        props.put("tintTop", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(accent, 0xFFFFFFFF, 0.10f), 0.16f * alpha)));
        props.put("tintBottom", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(accent, 0xFF000000, 0.25f), 0.112f * alpha)));
        props.put("shadow", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(IslandUiUtil.BASE_SHADOW, 0.28f * alpha)));
        props.put("stroke", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(syncTheme.get() ? theme().windowStroke() : IslandUiUtil.BASE_STROKE, accent, 0.18f), 0.18f * alpha)));
        props.put("highlight", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(0xFFFFFFFF, 0.08f * alpha)));
        props.put("contextFill", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(0xFF000000, 0.36f * alpha)));
        props.put("contextStroke", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(0xFFFFFFFF, 0.10f * alpha)));
        props.put("timeFill", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(0xFF000000, 0.48f * alpha)));
        props.put("timeStroke", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(0xFFFFFFFF, 0.10f * alpha)));
        props.put("pvpFill", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(STATUS_PVP, 0.86f * alpha)));
        props.put("pvpStroke", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(STATUS_PVP, 0xFFFFFFFF, 0.32f), 0.34f * alpha)));
        props.put("accent", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(accent, alpha)));
        props.put("accentSoft", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(uiAccentSoft, alpha)));
        props.put("textPrimary", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(uiTextPrimary, alpha)));
        props.put("textSecondary", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(uiTextSecondary, alpha)));
        props.put("textMuted", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(uiTextMuted, alpha)));
        props.put("progressBg", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(PROGRESS_BG, expandedAlpha)));
        props.put("shuffleX", controls.shuffleX - x);
        props.put("shuffleY", controls.shuffleY - y);
        props.put("prevX", controls.prevX - x);
        props.put("prevY", controls.prevY - y);
        props.put("playX", controls.playX - x);
        props.put("playY", controls.playY - y);
        props.put("nextX", controls.nextX - x);
        props.put("nextY", controls.nextY - y);
        props.put("repeatX", controls.repeatX - x);
        props.put("repeatY", controls.repeatY - y);
        props.put("controlSize", controls.controlSize);
        props.put("showShuffle", currentSnapshot != null && currentSnapshot.supportsShuffle());
        props.put("showRepeat", currentSnapshot != null && currentSnapshot.supportsRepeat());
        props.put("shuffleHover", controls.shuffleHover);
        props.put("prevHover", controls.prevHover);
        props.put("playHover", controls.playHover);
        props.put("nextHover", controls.nextHover);
        props.put("repeatHover", controls.repeatHover);
        props.put("iconShuffle", IslandUiUtil.iconString(ICON_SHUFFLE));
        props.put("iconPrev", IslandUiUtil.iconString(ICON_PREV));
        props.put("iconPlay", IslandUiUtil.iconString(currentSnapshot != null && currentSnapshot.isPlaying() ? ICON_PAUSE : ICON_PLAY));
        props.put("iconNext", IslandUiUtil.iconString(ICON_NEXT));
        props.put("shuffleColor", IslandUiUtil.hex(IslandUiUtil.shuffleColor(currentSnapshot)));
        props.put("repeatColor", IslandUiUtil.hex(IslandUiUtil.repeatColor(currentSnapshot)));
        props.put("repeatAsset", IslandUiUtil.repeatSvg(currentSnapshot));
        return props;
    }

    private float titleScrollSeconds(String title, String artist, long durationSeconds) {
        String key = (title != null ? title : "") + "\u0000" + (artist != null ? artist : "");
        long now = Util.getMillis();
        if (!key.equals(titleScrollKey)) {
            titleScrollKey = key;
            titleScrollStartMs = now;
            return 0.0f;
        }
        return Math.max(0.0f, (now - titleScrollStartMs) / 1000.0f);
    }

    private UiScriptModule ensureModule() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getResourceManager() == null) return null;
        if (!moduleHandle.ensureLoaded(mc.getResourceManager())) {
            HudScriptLayouts.reportLoadError(moduleHandle);
            return null;
        }
        moduleHandle.consumeChanged();
        return moduleHandle.module();
    }

    private void resetScriptRuntime() {
        scriptRuntime.reset();
    }

    private IslandMode resolveMode(MediaSessionService.Snapshot snapshot) {
        if (snapshot != null && snapshot.hasSession()) return IslandMode.MUSIC;
        if (PvpState.isActive()) return IslandMode.PVP;
        return IslandMode.TIME;
    }

    private IslandMetrics measureMetrics(TextRenderer titleRenderer,
                                         TextRenderer valueRenderer,
                                         TextRenderer metaRenderer) {
        float pvpTimerW = IslandUiUtil.reservedTimeWidth(valueRenderer, IslandUiUtil.formatSecondsOnly(predictedPvpSeconds()), 0.84f);
        float pvpWidth = 24f + pvpTimerW;

        String title = currentSnapshot != null ? currentSnapshot.title() : "";
        float titleW = IslandUiUtil.measureWidth(titleRenderer, title, 0.82f);
        String compactElapsed = IslandUiUtil.formatShortTime(currentSnapshot != null ? currentSnapshot.predictedPositionSeconds() : 0L);
        float elapsedW = IslandUiUtil.reservedTimeWidth(valueRenderer, compactElapsed, 0.72f);
        float musicWidth = 10f + elapsedW + 18f + COMPACT_ART + COMPACT_GAP + titleW + COMPACT_GAP + COMPACT_WAVE_W + 10f;
        return new IslandMetrics(pvpWidth, IslandUiUtil.clamp(musicWidth, 226f, 308f));
    }

    private boolean isInteractiveScreen(Screen screen) {
        return screen instanceof ChatScreen || isScreenOverlayAllowed(screen);
    }

    private boolean isScreenOverlayAllowed(Screen screen) {
        if (screen == null || screen instanceof ChatScreen) return false;
        if (screen instanceof ClickGuiScreen) return false;
        return !IslandUiUtil.isSettingsLikeScreen(screen);
    }

    private int accentFor(IslandMode mode) {
        int syncedAccent = syncTheme.get() ? theme().accent() : (accentColor.getArgb() | 0xFF000000);
        return switch (mode) {
            case TIME -> syncedAccent;
            case MUSIC -> syncedAccent;
            case PVP -> STATUS_PVP;
            case CLICKGUI -> syncedAccent;
        };
    }

    private void updatePalette() {
        if (syncTheme.get()) {
            int windowBg = HudRenderUtil.setAlpha(theme().windowBg(), themeAlpha.get());
            uiFillTop = HudRenderUtil.mixRgb(windowBg, 0xFFFFFFFF, 0.04f);
            uiFillBottom = HudRenderUtil.mixRgb(windowBg, 0xFF000000, 0.08f);
            uiTextPrimary = theme().textPrimary();
            uiTextSecondary = theme().textMuted();
            uiTextMuted = HudRenderUtil.mixColor(theme().textMuted(), windowBg, 0.18f);
            uiAccent = theme().accent();
            uiAccentSoft = theme().accentSoft();
            return;
        }

        int baseBg = bgColor.getArgb();
        uiFillTop = HudRenderUtil.mixRgb(baseBg, 0xFFFFFFFF, 0.04f);
        uiFillBottom = HudRenderUtil.mixRgb(baseBg, 0xFF000000, 0.08f);
        uiTextPrimary = textColor.getArgb() | 0xFF000000;
        uiTextSecondary = mutedColor.getArgb() | 0xFF000000;
        uiTextMuted = HudRenderUtil.mixColor(uiTextSecondary, baseBg, 0.16f);
        uiAccent = accentColor.getArgb() | 0xFF000000;
        uiAccentSoft = HudRenderUtil.mixColor(uiAccent, baseBg, 0.40f);
    }

    private float predictedPvpSeconds() {
        Float secondsLeft = PvpState.getSecondsLeft();
        if (!PvpState.isActive() || secondsLeft == null) return 0f;
        long sinceUpdate = Math.max(0L, Util.getMillis() - PvpState.getLastUpdateMs());
        return Math.max(0f, secondsLeft - sinceUpdate / 1000f);
    }

    private enum IslandMode {
        TIME,
        MUSIC,
        PVP,
        CLICKGUI
    }

    private record IslandMetrics(float pvpWidth, float musicCompactWidth) {
    }
}
