package dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland.controls;

import dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland.controls.IslandUiUtil;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.media.session.MediaSessionService;
import net.minecraft.client.Minecraft;

/**
 * Геометрия и hover-состояния ряда медиа-контролов Dynamic Island.
 */
public final class IslandControls {

    public static final float EXPANDED_PAD_X = 13f;
    public static final float CONTROL_SIZE = 27f;
    public static final float CONTROL_GAP = 7f;
    public static final float CONTROL_ROW_GAP_Y = -5f;

    public float shuffleX;
    public float shuffleY;
    public float prevX;
    public float prevY;
    public float playX;
    public float playY;
    public float nextX;
    public float nextY;
    public float repeatX;
    public float repeatY;
    public float controlSize;
    public float shuffleHover;
    public float prevHover;
    public float playHover;
    public float nextHover;
    public float repeatHover;

    public void updateLayout(float drawX, float drawY, float width, TextRenderer valueRenderer,
                             MediaSessionService.Snapshot snapshot) {
        float progressX = drawX + EXPANDED_PAD_X;
        float progressW = width - EXPANDED_PAD_X * 2f;
        float progressY = drawY + 52f;
        float timeH = IslandUiUtil.measureHeight(valueRenderer, 0.80f);

        controlSize = CONTROL_SIZE;
        float controlGap = CONTROL_GAP;
        boolean showShuffle = snapshot != null && snapshot.supportsShuffle();
        boolean showRepeat = snapshot != null && snapshot.supportsRepeat();
        int activeControls = 3 + (showShuffle ? 1 : 0) + (showRepeat ? 1 : 0);
        float totalControlsW = controlSize * activeControls + controlGap * Math.max(0, activeControls - 1);
        float controlsX = drawX + (width - totalControlsW) * 0.5f;
        float controlsY = progressY + 5f + timeH + CONTROL_ROW_GAP_Y;
        float cursorX = controlsX;

        if (showShuffle) {
            shuffleX = cursorX;
            shuffleY = controlsY;
            cursorX += controlSize + controlGap;
        } else {
            shuffleX = shuffleY = 0f;
        }
        prevX = cursorX;
        prevY = controlsY;
        cursorX += controlSize + controlGap;
        playX = cursorX;
        playY = controlsY;
        cursorX += controlSize + controlGap;
        nextX = cursorX;
        nextY = controlsY;
        cursorX += controlSize + controlGap;
        if (showRepeat) {
            repeatX = cursorX;
            repeatY = controlsY;
        } else {
            repeatX = repeatY = 0f;
        }
    }

    public void updateHover(boolean chatOpen, Minecraft mc, MediaSessionService.Snapshot snapshot) {
        if (!chatOpen || controlSize <= 0f || mc == null || mc.getWindow() == null) {
            shuffleHover = AnimationUtility.approach(shuffleHover, 0f, 0.25f);
            prevHover = AnimationUtility.approach(prevHover, 0f, 0.25f);
            playHover = AnimationUtility.approach(playHover, 0f, 0.25f);
            nextHover = AnimationUtility.approach(nextHover, 0f, 0.25f);
            repeatHover = AnimationUtility.approach(repeatHover, 0f, 0.25f);
            return;
        }

        int fbw = mc.getWindow().getWidth();
        int fbh = mc.getWindow().getHeight();
        float uiScale = HudScale.scale(fbw, fbh);
        float mx = HudScale.toVirtual((float) mc.mouseHandler.xpos(), uiScale);
        float my = HudScale.toVirtual((float) mc.mouseHandler.ypos(), uiScale);
        boolean showShuffle = snapshot != null && snapshot.supportsShuffle();
        boolean showRepeat = snapshot != null && snapshot.supportsRepeat();

        shuffleHover = AnimationUtility.approach(shuffleHover, showShuffle && IslandUiUtil.hit(mx, my, shuffleX, shuffleY, controlSize, controlSize) ? 1f : 0f, 0.25f);
        prevHover = AnimationUtility.approach(prevHover, IslandUiUtil.hit(mx, my, prevX, prevY, controlSize, controlSize) ? 1f : 0f, 0.25f);
        playHover = AnimationUtility.approach(playHover, IslandUiUtil.hit(mx, my, playX, playY, controlSize, controlSize) ? 1f : 0f, 0.25f);
        nextHover = AnimationUtility.approach(nextHover, IslandUiUtil.hit(mx, my, nextX, nextY, controlSize, controlSize) ? 1f : 0f, 0.25f);
        repeatHover = AnimationUtility.approach(repeatHover, showRepeat && IslandUiUtil.hit(mx, my, repeatX, repeatY, controlSize, controlSize) ? 1f : 0f, 0.25f);
    }

    public void reset() {
        shuffleX = shuffleY = 0f;
        prevX = prevY = 0f;
        playX = playY = 0f;
        nextX = nextY = 0f;
        repeatX = repeatY = 0f;
        controlSize = 0f;
        shuffleHover = 0f;
        prevHover = 0f;
        playHover = 0f;
        nextHover = 0f;
        repeatHover = 0f;
    }
}
