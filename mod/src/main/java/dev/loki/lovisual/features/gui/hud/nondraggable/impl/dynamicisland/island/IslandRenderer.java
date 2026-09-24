package dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland;

import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland.controls.IslandControls;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland.controls.IslandUiUtil;
import dev.loki.lovisual.features.gui.hud.script.HudScriptLayouts;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.runtime.UiRuntime;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiProjectionMode;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiRenderContext;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModule;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModuleHandle;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.render.UiScriptProps;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime.CachedUiScriptRuntime;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.pvp.state.PvpState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import java.time.LocalTime;
import static dev.loki.lovisual.features.theme.Theme.theme;

final class IslandRenderer {
    static final int STATUS_PVP = 0xFFFF6368;
    static final int PROGRESS_BG = 0x2AFFFFFF;
    static final int ICON_PAUSE = 0xEA02;
    static final int ICON_PLAY = 0xEA03;
    static final int ICON_PREV = 0xEA04;
    static final int ICON_NEXT = 0xEA05;
    static final int ICON_SHUFFLE = 0xEA06;
    int uiFillTop; int uiFillBottom; int uiTextPrimary; int uiTextSecondary; int uiTextMuted; int uiAccent; int uiAccentSoft;
    void updatePalette(BooleanValue syncTheme, NumberValue<Integer> themeAlpha, RGBAColorValue bgColor, RGBColorValue accentColor, RGBColorValue textColor, RGBColorValue mutedColor) {
        if (syncTheme.get()) {
            int bg = HudRenderUtil.setAlpha(theme().windowBg(), themeAlpha.get());
            uiFillTop = HudRenderUtil.mixRgb(bg, 0xFFFFFFFF, 0.04f);
            uiFillBottom = HudRenderUtil.mixRgb(bg, 0xFF000000, 0.08f);
            uiTextPrimary = theme().textPrimary(); uiTextSecondary = theme().textMuted();
            uiTextMuted = HudRenderUtil.mixColor(theme().textMuted(), bg, 0.18f);
            uiAccent = theme().accent(); uiAccentSoft = theme().accentSoft(); return;
        }
        int baseBg = bgColor.getArgb();
        uiFillTop = HudRenderUtil.mixRgb(baseBg, 0xFFFFFFFF, 0.04f);
        uiFillBottom = HudRenderUtil.mixRgb(baseBg, 0xFF000000, 0.08f);
        uiTextPrimary = textColor.getArgb() | 0xFF000000; uiTextSecondary = mutedColor.getArgb() | 0xFF000000;
        uiTextMuted = HudRenderUtil.mixColor(uiTextSecondary, baseBg, 0.16f);
        uiAccent = accentColor.getArgb() | 0xFF000000; uiAccentSoft = HudRenderUtil.mixColor(uiAccent, baseBg, 0.40f);
    }
    int accentFor(IslandState.IslandMode mode, BooleanValue syncTheme, RGBColorValue accentColor) {
        int synced = syncTheme.get() ? theme().accent() : (accentColor.getArgb() | 0xFF000000);
        return mode == IslandState.IslandMode.PVP ? STATUS_PVP : synced;
    }
    boolean renderScripted(Renderer2D renderer, TextRenderer textRenderer, TextRenderer titleRenderer, TextRenderer metaRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH, float width, float height, float mainWidth, float mainX, float leftContextWidth, float rightContextWidth, float alpha, float compactAlpha, float expandedAlpha, float radius, int accent, float x, float y, IslandState state, IslandAnimation anim, IslandControls controls, BooleanValue syncTheme, BooleanValue blur, NumberValue<Integer> blurAlpha, UiScriptModuleHandle moduleHandle, CachedUiScriptRuntime scriptRuntime) {
        if (moduleHandle.isRuntimeBlocked()) return false;
        UiScriptModule loaded = ensureModule(moduleHandle); if (loaded == null) return false;
        UiScriptProps props = buildProps(screenW, screenH, width, height, mainWidth, mainX, leftContextWidth, rightContextWidth, alpha, compactAlpha, expandedAlpha, radius, accent, x, y, titleRenderer, metaRenderer, state, anim, controls, syncTheme, blur, blurAlpha);
        long treeSig = IslandRuntimePatches.islandStructuralSignature(props);
        long dataSig = CachedUiScriptRuntime.signature(props.asMap());
        UiRuntime baked = scriptRuntime.bake(moduleHandle, loaded, "dynamic_island", treeSig, dataSig, treeSig, width, height, textRenderer, x, y, width, height, props::asMap, () -> IslandRuntimePatches.islandRuntimePatches(props), () -> IslandBoundsPatches.islandBoundsPatches(props, x, y, controls));
        if (baked == null) return false;
        baked.render(new UiRenderContext(renderer, textRenderer, ctx, tickDelta, UiProjectionMode.UNSCALED_LOGICAL));
        return true;
    }
    UiScriptProps buildProps(int screenW, int screenH, float width, float height, float mainWidth, float mainX, float leftContextWidth, float rightContextWidth, float alpha, float compactAlpha, float expandedAlpha, float radius, int accent, float originX, float originY, TextRenderer titleRenderer, TextRenderer metaRenderer, IslandState state, IslandAnimation anim, IslandControls controls, BooleanValue syncTheme, BooleanValue blur, NumberValue<Integer> blurAlpha) {
        UiScriptProps p = UiScriptProps.create(80);
        String title = state.currentSnapshot != null ? state.currentSnapshot.title() : "";
        String artist = state.currentSnapshot != null ? state.currentSnapshot.artist() : "";
        long duration = state.currentSnapshot != null ? state.currentSnapshot.durationSeconds() : 0L;
        p.put("screenW", screenW); p.put("screenH", screenH); p.put("width", width); p.put("height", height);
        p.put("mainWidth", mainWidth); p.put("mainX", mainX); p.put("leftContextWidth", leftContextWidth); p.put("rightContextWidth", rightContextWidth);
        p.put("contextGap", IslandBoundsPatches.CONTEXT_GAP);
        boolean pvpVis = (state.currentMode == IslandState.IslandMode.MUSIC || state.currentMode == IslandState.IslandMode.CLICKGUI) && PvpState.isActive() && rightContextWidth > 0.5f;
        p.put("pvpContextVisible", pvpVis);
        p.put("contextAlpha", state.currentMode == IslandState.IslandMode.MUSIC ? compactAlpha : (state.currentMode == IslandState.IslandMode.CLICKGUI ? alpha : 0.0f));
        p.put("mode", state.currentMode.name().toLowerCase()); p.put("alpha", alpha); p.put("compactAlpha", compactAlpha); p.put("expandedAlpha", expandedAlpha);
        p.put("expand", anim.expandAnim); p.put("expanded", state.expanded); p.put("blur", blur.get()); p.put("blurAlpha", blurAlpha.get() / 255.0f);
        p.put("radius", radius); p.put("time", IslandUiUtil.formatClock(LocalTime.now()));
        p.put("pvpTimer", pvpVis || state.currentMode == IslandState.IslandMode.PVP ? IslandUiUtil.formatSecondsOnly(state.predictedPvpSeconds()) : "");
        ClickGuiRenderer.ClickGuiIslandState cs = ClickGuiRenderer.islandState();
        p.put("clickGuiLabel", cs.activeLabel()); p.put("clickGuiTabCount", cs.tabCount()); p.put("clickGuiPicker", cs.pickerActive());
        p.put("title", title); p.put("artist", artist);
        p.put("titleWidthCompact", IslandUiUtil.measureWidth(titleRenderer, title, 0.82f));
        p.put("titleWidthExpanded", IslandUiUtil.measureWidth(titleRenderer, title, 0.86f));
        p.put("artistWidthExpanded", IslandUiUtil.measureWidth(metaRenderer, artist, 0.72f));
        p.put("titleScrollTime", state.titleScrollSeconds(title, artist, duration));
        p.put("elapsed", IslandUiUtil.formatShortTime(state.currentSnapshot != null ? state.currentSnapshot.predictedPositionSeconds() : 0L));
        p.put("total", IslandUiUtil.formatShortTime(state.currentSnapshot != null ? state.currentSnapshot.durationSeconds() : 0L));
        p.put("progress", state.currentSnapshot != null && state.currentSnapshot.durationSeconds() > 0 ? AnimationUtility.clamp((float) state.currentSnapshot.predictedPositionSeconds() / (float) state.currentSnapshot.durationSeconds(), 0f, 1f) : 0f);
        p.put("playing", state.currentSnapshot != null && state.currentSnapshot.isPlaying());
        Identifier art = state.currentSnapshot != null ? state.currentSnapshot.artworkTexture() : null;
        p.put("artworkTexture", art != null ? art.toString() : "");
        p.put("fillTop", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(uiFillTop, 0.88f * alpha)));
        p.put("fillBottom", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(uiFillBottom, 0.88f * alpha)));
        p.put("tintTop", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(accent, 0xFFFFFFFF, 0.10f), 0.16f * alpha)));
        p.put("tintBottom", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(accent, 0xFF000000, 0.25f), 0.112f * alpha)));
        p.put("shadow", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(IslandUiUtil.BASE_SHADOW, 0.28f * alpha)));
        p.put("stroke", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(syncTheme.get() ? theme().windowStroke() : IslandUiUtil.BASE_STROKE, accent, 0.18f), 0.18f * alpha)));
        p.put("highlight", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(0xFFFFFFFF, 0.08f * alpha)));
        p.put("contextFill", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(0xFF000000, 0.36f * alpha)));
        p.put("contextStroke", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(0xFFFFFFFF, 0.10f * alpha)));
        p.put("timeFill", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(0xFF000000, 0.48f * alpha)));
        p.put("timeStroke", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(0xFFFFFFFF, 0.10f * alpha)));
        p.put("pvpFill", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(STATUS_PVP, 0.86f * alpha)));
        p.put("pvpStroke", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(STATUS_PVP, 0xFFFFFFFF, 0.32f), 0.34f * alpha)));
        p.put("accent", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(accent, alpha)));
        p.put("accentSoft", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(uiAccentSoft, alpha)));
        p.put("textPrimary", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(uiTextPrimary, alpha)));
        p.put("textSecondary", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(uiTextSecondary, alpha)));
        p.put("textMuted", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(uiTextMuted, alpha)));
        p.put("progressBg", IslandUiUtil.hex(HudRenderUtil.scaleAlpha(PROGRESS_BG, expandedAlpha)));
        p.put("shuffleX", controls.shuffleX - originX); p.put("shuffleY", controls.shuffleY - originY);
        p.put("prevX", controls.prevX - originX); p.put("prevY", controls.prevY - originY);
        p.put("playX", controls.playX - originX); p.put("playY", controls.playY - originY);
        p.put("nextX", controls.nextX - originX); p.put("nextY", controls.nextY - originY);
        p.put("repeatX", controls.repeatX - originX); p.put("repeatY", controls.repeatY - originY);
        p.put("controlSize", controls.controlSize);
        p.put("showShuffle", state.currentSnapshot != null && state.currentSnapshot.supportsShuffle());
        p.put("showRepeat", state.currentSnapshot != null && state.currentSnapshot.supportsRepeat());
        p.put("shuffleHover", controls.shuffleHover); p.put("prevHover", controls.prevHover); p.put("playHover", controls.playHover); p.put("nextHover", controls.nextHover); p.put("repeatHover", controls.repeatHover);
        p.put("iconShuffle", IslandUiUtil.iconString(ICON_SHUFFLE)); p.put("iconPrev", IslandUiUtil.iconString(ICON_PREV));
        p.put("iconPlay", IslandUiUtil.iconString(state.currentSnapshot != null && state.currentSnapshot.isPlaying() ? ICON_PAUSE : ICON_PLAY));
        p.put("iconNext", IslandUiUtil.iconString(ICON_NEXT));
        p.put("shuffleColor", IslandUiUtil.hex(IslandUiUtil.shuffleColor(state.currentSnapshot)));
        p.put("repeatColor", IslandUiUtil.hex(IslandUiUtil.repeatColor(state.currentSnapshot)));
        p.put("repeatAsset", IslandUiUtil.repeatSvg(state.currentSnapshot));
        return p;
    }
    private UiScriptModule ensureModule(UiScriptModuleHandle h) {
        Minecraft mc = Minecraft.getInstance(); if (mc == null || mc.getResourceManager() == null) return null;
        if (!h.ensureLoaded(mc.getResourceManager())) { HudScriptLayouts.reportLoadError(h); return null; }
        h.consumeChanged(); return h.module();
    }
}
