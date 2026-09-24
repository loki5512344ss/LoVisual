package dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland;

import dev.loki.lovisual.Lifecycle;
import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.screen.ClickGuiScreen;
import dev.loki.lovisual.features.gui.hud.anchor.HudRenderSpace;
import dev.loki.lovisual.features.gui.hud.base.AbstractHudElement;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland.controls.IslandControls;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland.controls.IslandUiUtil;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.features.gui.hud.script.HudScriptLayouts;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.core.frame.RenderPhase;
import dev.loki.lovisual.render.engine.core.frame.RenderPhaseScope;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.FullScreenRenderer;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModuleHandle;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime.CachedUiScriptRuntime;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.media.session.MediaSessionService;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import java.util.List;

/**
 * Dynamic Island: фасад оркестрации острова.
 * Логика вынесена в {@code island/}: State, Animation, Layout, Renderer.
 */
@HudElementRegister(order = 60)
public final class DynamicIsland extends AbstractHudElement {
    private static final String LAYOUT_ID = "lovisual:api/hud/static/dynamic_island";
    public static final DynamicIsland INSTANCE = new DynamicIsland();
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
    private final IslandState state = new IslandState();
    private final IslandAnimation anim = new IslandAnimation();
    private final IslandLayout layout = new IslandLayout();
    private final IslandRenderer rendererHelper = new IslandRenderer();

    private DynamicIsland() { super("dynamic_island", "Dynamic Island", true); }

    public static boolean shouldRenderInScreenOverlay(Screen screen) { return INSTANCE != null && INSTANCE.isScreenOverlayAllowed(screen); }
    public static void renderScreenOverlay(GuiGraphicsExtractor ctx, float tickDelta) { INSTANCE.renderScreenOverlayInternal(ctx, tickDelta); }
    public static boolean shouldOwnClickGuiTabShell() { return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.state.clickGuiShellVisible; }

    @Override protected void defineSettings(List<SettingDef> defs) {
        defs.add(SettingDef.bool(syncTheme)); defs.add(SettingDef.number(themeAlpha).visibleWhen(syncTheme::get));
        defs.add(SettingDef.bool(blur)); defs.add(SettingDef.number(blurAlpha).visibleWhen(blur::get));
        defs.add(SettingDef.color(bgColor).visibleWhen(() -> !syncTheme.get()));
        defs.add(SettingDef.colorNoAlpha(accentColor).visibleWhen(() -> !syncTheme.get()));
        defs.add(SettingDef.colorNoAlpha(textColor).visibleWhen(() -> !syncTheme.get()));
        defs.add(SettingDef.colorNoAlpha(mutedColor).visibleWhen(() -> !syncTheme.get()));
    }
    @Override public boolean usesEngineRenderer() { return true; }
    @Override public HudRenderSpace getRenderSpace() { return HudRenderSpace.UNSCALED_LOGICAL; }
    @Override public HudPhase getHudPhase() { return HudPhase.AFTER_SUBTITLES; }
    @Override public int getRenderOrder() { return 40; }
    @Override public boolean isMouseOverInteractive(float mx, float my) { Minecraft mc = Minecraft.getInstance(); return mc != null && isInteractiveScreen(ClientScreen.current()) && contains(mx, my); }

    @Override public boolean onMouseClicked(float mx, float my, int button) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || !isInteractiveScreen(ClientScreen.current()) || !contains(mx, my)) return false;
        if (state.currentMode == IslandState.IslandMode.MUSIC && anim.isExpanded(0.01f) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (state.currentSnapshot != null && state.currentSnapshot.supportsShuffle() && IslandUiUtil.hit(mx, my, controls.shuffleX, controls.shuffleY, controls.controlSize, controls.controlSize)) { mediaService.toggleShuffle(); return true; }
            if (IslandUiUtil.hit(mx, my, controls.prevX, controls.prevY, controls.controlSize, controls.controlSize)) { mediaService.previous(); return true; }
            if (IslandUiUtil.hit(mx, my, controls.playX, controls.playY, controls.controlSize, controls.controlSize)) { mediaService.playPause(); return true; }
            if (IslandUiUtil.hit(mx, my, controls.nextX, controls.nextY, controls.controlSize, controls.controlSize)) { mediaService.next(); return true; }
            if (state.currentSnapshot != null && state.currentSnapshot.supportsRepeat() && IslandUiUtil.hit(mx, my, controls.repeatX, controls.repeatY, controls.controlSize, controls.controlSize)) { mediaService.cycleRepeatMode(); return true; }
        }
        if (state.currentMode == IslandState.IslandMode.MUSIC && (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) { state.expanded = !state.expanded; return true; }
        return false;
    }

    @Override public void renderEngine(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) { renderEngineInternal(renderer, textRenderer, ctx, tickDelta, screenW, screenH, false); }

    private void renderScreenOverlayInternal(GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!Lifecycle.canRunHud() || mc == null || mc.getWindow() == null || ctx == null) return;
        Screen cur = ClientScreen.current(); boolean clickGuiOverlay = isEnabled() && IslandUiUtil.clickGuiEnabled();
        if (!clickGuiOverlay && !isScreenOverlayAllowed(cur)) return;
        FullScreenRenderer.ensureInit();
        int screenW = Math.max(1, Math.round(HudScale.virtualWidth(mc.getWindow().getWidth(), mc.getWindow().getHeight())));
        int screenH = Math.max(1, Math.round(HudScale.virtualHeight(mc.getWindow().getWidth(), mc.getWindow().getHeight())));
        LoVisualRenderSystem.updateFrameTiming(tickDelta, tickDelta, tickDelta);
        ViewportContext.beginUnscaledLogical(ctx);
        try (RenderPhaseScope ignored = LoVisualRenderSystem.phase(RenderPhase.SCREEN_TOP, "2d:screen:dynamic_island")) {
            Renderer2D.COLOR.begin(); renderEngineInternal(Renderer2D.COLOR, TextRenderer.get(), ctx, tickDelta, screenW, screenH, true); Renderer2D.COLOR.render();
        } finally { ViewportContext.end(ctx); }
    }

    private void renderEngineInternal(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH, boolean screenOverlayPass) {
        Minecraft mc = Minecraft.getInstance(); if (mc == null) return;
        HudScriptLayouts.pollReloadCombo(mc); if (moduleHandle.consumeChanged()) scriptRuntime.reset();
        Screen currentScreen = ClientScreen.current(); boolean chatOpen = currentScreen instanceof ChatScreen;
        boolean clickGuiVisible = IslandUiUtil.clickGuiEnabled(); boolean clickGuiTabShellScreen = currentScreen instanceof ClickGuiScreen;
        ClickGuiRenderer.ClickGuiIslandState clickGuiState = ClickGuiRenderer.islandState();
        boolean clickGuiBridge = isEnabled() && screenOverlayPass && clickGuiVisible && clickGuiTabShellScreen && clickGuiState.lifecycle() > 0.001f && clickGuiState.tabBarW() > 1.0f;
        if (clickGuiVisible && clickGuiTabShellScreen && !clickGuiBridge) { state.clickGuiShellVisible = false; setBounds(0f, 0f, 0f, 0f); controls.reset(); return; }
        boolean shouldShow = clickGuiBridge || (isEnabled() && (screenOverlayPass ? isScreenOverlayAllowed(currentScreen) : currentScreen == null || chatOpen));
        state.currentSnapshot = mediaService.snapshot(); state.currentMode = clickGuiBridge ? IslandState.IslandMode.CLICKGUI : state.resolveMode(state.currentSnapshot);
        if (!isInteractiveScreen(currentScreen) || state.currentMode != IslandState.IslandMode.MUSIC) state.expanded = false;
        float dt = AnimationUtility.deltaTime(); anim.updateVisibility(dt, shouldShow); anim.updateExpand(dt, state.expanded);
        state.clickGuiShellVisible = clickGuiBridge && anim.isVisible(0.01f);
        if (!shouldShow && !anim.isVisible(0.01f)) { state.clickGuiShellVisible = false; setBounds(0f, 0f, 0f, 0f); controls.reset(); return; }
        rendererHelper.updatePalette(syncTheme, themeAlpha, bgColor, accentColor, textColor, mutedColor);
        TextRenderer fallback = textRenderer != null ? textRenderer : TextRenderer.get();
        TextRenderer titleRenderer = Fonts.renderer("InterMedium", FontInfo.Type.Regular, fallback);
        TextRenderer valueRenderer = Fonts.renderer("Inter", FontInfo.Type.Bold, fallback);
        TextRenderer metaRenderer = Fonts.renderer("Inter", FontInfo.Type.Regular, fallback);
        IslandState.IslandMetrics metrics = layout.measureMetrics(titleRenderer, valueRenderer, state.predictedPvpSeconds(), state.currentSnapshot);
        IslandLayout.LayoutResult lr = layout.computeLayout(state, anim, metrics, clickGuiState, screenW, screenH, mc);
        anim.updateLayout(dt, lr.targetMainWidth(), lr.targetHeight(), lr.targetLeftContextWidth(), lr.targetRightContextWidth());
        float drawMainWidth = anim.mainWidthAnim < 0f ? lr.targetMainWidth() : anim.mainWidthAnim;
        float drawHeight = anim.heightAnim < 0f ? lr.targetHeight() : anim.heightAnim;
        float leftOccupied = anim.leftContextWidthAnim > 0.5f ? anim.leftContextWidthAnim + IslandBoundsPatches.CONTEXT_GAP : 0f;
        float rightOccupied = anim.rightContextWidthAnim > 0.5f ? IslandBoundsPatches.CONTEXT_GAP + anim.rightContextWidthAnim : 0f;
        float drawWidth = leftOccupied + drawMainWidth + rightOccupied;
        float drawX = state.currentMode == IslandState.IslandMode.CLICKGUI ? clickGuiState.tabBarX() - leftOccupied : (screenW - drawMainWidth) * 0.5f - leftOccupied;
        float drawY = state.currentMode == IslandState.IslandMode.CLICKGUI ? clickGuiState.tabBarY() - (1f - AnimationUtility.easeOutCubic(clickGuiState.lifecycle())) * 18f : IslandLayout.TOP_Y + IslandUiUtil.bossBarOffset(mc, screenH, IslandLayout.TOP_Y);
        float mainX = leftOccupied; float alpha = anim.visibilityAlpha();
        float radius = state.currentMode == IslandState.IslandMode.CLICKGUI ? drawHeight * 0.5f : AnimationUtility.lerp(IslandLayout.CLOSED_RADIUS, IslandLayout.EXPANDED_RADIUS, anim.expandAnim);
        int accent = rendererHelper.accentFor(state.currentMode, syncTheme, accentColor);
        setBounds(drawX, drawY, drawWidth, drawHeight);
        float expandedAlpha = state.currentMode == IslandState.IslandMode.MUSIC ? anim.expandedAlpha(alpha) : 0f;
        float compactAlpha = state.currentMode == IslandState.IslandMode.MUSIC ? anim.compactAlpha(alpha) : 0f;
        if (expandedAlpha > 0.01f) { controls.updateLayout(drawX + mainX, drawY, drawMainWidth, valueRenderer, state.currentSnapshot); controls.updateHover(isInteractiveScreen(currentScreen), mc, state.currentSnapshot); } else controls.reset();
        boolean rendered = rendererHelper.renderScripted(renderer, fallback, titleRenderer, metaRenderer, ctx, tickDelta, screenW, screenH, drawWidth, drawHeight, drawMainWidth, mainX, anim.leftContextWidthAnim, anim.rightContextWidthAnim, alpha, compactAlpha, expandedAlpha, radius, accent, x, y, state, anim, controls, syncTheme, blur, blurAlpha, moduleHandle, scriptRuntime);
        if (!rendered) IslandUiUtil.drawFallback(renderer, drawX + mainX, drawY, drawMainWidth, drawHeight, radius, alpha, accent, rendererHelper.uiFillTop);
    }

    private boolean isInteractiveScreen(Screen screen) { return screen instanceof ChatScreen || isScreenOverlayAllowed(screen); }
    private boolean isScreenOverlayAllowed(Screen screen) { if (screen == null || screen instanceof ChatScreen) return false; if (screen instanceof ClickGuiScreen) return false; return !IslandUiUtil.isSettingsLikeScreen(screen); }
}
