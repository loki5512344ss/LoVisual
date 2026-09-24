package dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland;

import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland.controls.IslandUiUtil;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.media.session.MediaSessionService;
import net.minecraft.client.Minecraft;

/**
 * Геометрия Dynamic Island: метрики и границы отрисовки.
 */
final class IslandLayout {

    static final float TOP_Y = 18f;
    static final float CLOSED_HEIGHT = 35f;
    static final float EXPANDED_HEIGHT = 98f;
    static final float CLOSED_RADIUS = 17f;
    static final float EXPANDED_RADIUS = 20f;
    static final float TIME_WIDTH = 74f;
    static final float PVP_MIN_WIDTH = 68f;
    static final float PVP_MAX_WIDTH = 96f;
    static final float MUSIC_EXPANDED_WIDTH = 338f;
    static final float TIME_CONTEXT_WIDTH = 57f;
    static final float COMPACT_PAD_X = 11f;
    static final float COMPACT_GAP = 9f;
    static final float COMPACT_ART = 22f;
    static final float COMPACT_WAVE_W = 15f;

    IslandState.IslandMetrics measureMetrics(TextRenderer titleRenderer,
                                             TextRenderer valueRenderer,
                                             float predictedPvpSeconds,
                                             MediaSessionService.Snapshot snapshot) {
        float pvpTimerW = IslandUiUtil.reservedTimeWidth(
                valueRenderer, IslandUiUtil.formatSecondsOnly(predictedPvpSeconds), 0.84f);
        float pvpWidth = 24f + pvpTimerW;
        String title = snapshot != null ? snapshot.title() : "";
        float titleW = IslandUiUtil.measureWidth(titleRenderer, title, 0.82f);
        String compactElapsed = IslandUiUtil.formatShortTime(
                snapshot != null ? snapshot.predictedPositionSeconds() : 0L);
        float elapsedW = IslandUiUtil.reservedTimeWidth(valueRenderer, compactElapsed, 0.72f);
        float musicWidth = 10f + elapsedW + 18f + COMPACT_ART + COMPACT_GAP + titleW + COMPACT_GAP + COMPACT_WAVE_W + 10f;
        return new IslandState.IslandMetrics(pvpWidth, IslandUiUtil.clamp(musicWidth, 226f, 308f));
    }

    LayoutResult computeLayout(IslandState state,
                               IslandAnimation anim,
                               IslandState.IslandMetrics metrics,
                               ClickGuiRenderer.ClickGuiIslandState clickGuiState,
                               int screenW,
                               int screenH,
                               Minecraft mc) {
        float targetMainWidth = switch (state.currentMode) {
            case TIME -> TIME_WIDTH;
            case PVP -> IslandUiUtil.clamp(metrics.pvpWidth(), PVP_MIN_WIDTH, PVP_MAX_WIDTH);
            case MUSIC -> AnimationUtility.lerp(metrics.musicCompactWidth(), MUSIC_EXPANDED_WIDTH, anim.expandAnim);
            case CLICKGUI -> clickGuiState.tabBarW();
        };
        float targetHeight = state.currentMode == IslandState.IslandMode.CLICKGUI
                ? clickGuiState.tabBarH()
                : AnimationUtility.lerp(CLOSED_HEIGHT,
                        state.currentMode == IslandState.IslandMode.MUSIC ? EXPANDED_HEIGHT : CLOSED_HEIGHT,
                        anim.expandAnim);

        boolean musicContext = state.currentMode == IslandState.IslandMode.MUSIC;
        boolean clickGuiContext = state.currentMode == IslandState.IslandMode.CLICKGUI;
        float contextPresence = musicContext
                ? 1.0f - AnimationUtility.easeOutCubic(anim.expandAnim)
                : (clickGuiContext ? 1.0f : 0.0f);
        float targetLeftContextWidth = TIME_CONTEXT_WIDTH * contextPresence;
        float targetRightContextWidth = dev.loki.lovisual.util.pvp.state.PvpState.isActive()
                ? IslandBoundsPatches.PVP_CONTEXT_WIDTH * contextPresence : 0.0f;

        float drawMainWidth = anim.mainWidthAnim < 0f ? targetMainWidth : anim.mainWidthAnim;
        float drawHeight = anim.heightAnim < 0f ? targetHeight : anim.heightAnim;
        float leftOccupied = anim.leftContextWidthAnim > 0.5f
                ? anim.leftContextWidthAnim + IslandBoundsPatches.CONTEXT_GAP : 0.0f;
        float rightOccupied = anim.rightContextWidthAnim > 0.5f
                ? IslandBoundsPatches.CONTEXT_GAP + anim.rightContextWidthAnim : 0.0f;
        float drawWidth = leftOccupied + drawMainWidth + rightOccupied;
        float drawX = state.currentMode == IslandState.IslandMode.CLICKGUI
                ? clickGuiState.tabBarX() - leftOccupied
                : (screenW - drawMainWidth) * 0.5f - leftOccupied;
        float drawY = state.currentMode == IslandState.IslandMode.CLICKGUI
                ? clickGuiState.tabBarY() - (1.0f - AnimationUtility.easeOutCubic(clickGuiState.lifecycle())) * 18.0f
                : TOP_Y + IslandUiUtil.bossBarOffset(mc, screenH, TOP_Y);
        float mainX = leftOccupied;
        float alpha = AnimationUtility.easeOutCubic(anim.visibilityAnim);
        float radius = state.currentMode == IslandState.IslandMode.CLICKGUI
                ? drawHeight * 0.5f
                : AnimationUtility.lerp(CLOSED_RADIUS, EXPANDED_RADIUS, anim.expandAnim);

        return new LayoutResult(targetMainWidth, targetHeight, targetLeftContextWidth, targetRightContextWidth,
                drawMainWidth, drawHeight, drawWidth, drawX, drawY, mainX, alpha, radius);
    }

    record LayoutResult(float targetMainWidth,
                        float targetHeight,
                        float targetLeftContextWidth,
                        float targetRightContextWidth,
                        float drawMainWidth,
                        float drawHeight,
                        float drawWidth,
                        float drawX,
                        float drawY,
                        float mainX,
                        float alpha,
                        float radius) {
    }
}
