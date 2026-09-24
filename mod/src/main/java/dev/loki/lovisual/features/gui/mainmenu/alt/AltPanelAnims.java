package dev.loki.lovisual.features.gui.mainmenu.alt;

import dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;

import static dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.*;

/**
 * Hover/press animation state for the left control panel of the alt manager.
 */
public final class AltPanelAnims {
    private float fieldHoverAnim;
    private float fieldFocusAnim;
    private float addHoverAnim;
    private float addPressAnim;
    private float offlineModeHoverAnim;
    private float microsoftModeHoverAnim;
    private float offlineModePressAnim;
    private float microsoftModePressAnim;
    private float randomHoverAnim;
    private float randomPressAnim;
    private float openSiteHoverAnim;
    private float openSitePressAnim;
    private float clearHoverAnim;
    private float clearPressAnim;

    public float fieldHover() { return fieldHoverAnim; }

    public float fieldFocus() { return fieldFocusAnim; }

    public float addHover() { return addHoverAnim; }

    public float addPress() { return addPressAnim; }

    public float offlineModeHover() { return offlineModeHoverAnim; }

    public float microsoftModeHover() { return microsoftModeHoverAnim; }

    public float offlineModePress() { return offlineModePressAnim; }

    public float microsoftModePress() { return microsoftModePressAnim; }

    public float randomHover() { return randomHoverAnim; }

    public float randomPress() { return randomPressAnim; }

    public float openSiteHover() { return openSiteHoverAnim; }

    public float openSitePress() { return openSitePressAnim; }

    public float clearHover() { return clearHoverAnim; }

    public float clearPress() { return clearPressAnim; }

    public void pressOfflineMode() { offlineModePressAnim = 1f; }

    public void pressMicrosoftMode() { microsoftModePressAnim = 1f; }

    public void pressAdd() { addPressAnim = 1f; }

    public void pressRandom() { randomPressAnim = 1f; }

    public void pressOpenSite() { openSitePressAnim = 1f; }

    public void pressClear() { clearPressAnim = 1f; }

    public void updateHover(float mouseX, float mouseY, AltUiMetrics.Layout l, AltManagerController controller) {
        float dt = AnimationUtility.deltaTime();
        boolean microsoftMode = controller.isMicrosoftMode();

        float fieldX = l.leftX() + 5f * SCALE;
        float fieldY = l.topY() + 57f * SCALE;
        float fieldW = LEFT_W - 10f * SCALE - ADD_SIZE - 3f * SCALE;
        float addX = fieldX + fieldW + 3f * SCALE;
        float buttonX = l.leftX() + 5f * SCALE;
        float buttonW = LEFT_W - 10f * SCALE;
        float randomY = fieldY + FIELD_H + 5f * SCALE;
        float openSiteY = randomY + TOP_BUTTON_H + 5f * SCALE;
        float clearY = (microsoftMode ? openSiteY : randomY) + TOP_BUTTON_H + 5f * SCALE;
        float modeX = l.leftX() + 5f * SCALE;
        float modeY = l.topY() + 27f * SCALE;
        float modeW = LEFT_W - 10f * SCALE;
        float modeGap = 2f * SCALE;
        float modeItemW = (modeW - modeGap) * 0.5f;

        fieldHoverAnim = animate(fieldHoverAnim, inside(mouseX, mouseY, fieldX, fieldY, fieldW, FIELD_H) ? 1f : 0f, dt, 11f);
        fieldFocusAnim = animate(fieldFocusAnim, !microsoftMode && controller.isNicknameFocused() ? 1f : 0f, dt, 10f);
        addHoverAnim = animate(addHoverAnim, inside(mouseX, mouseY, addX, fieldY, ADD_SIZE, ADD_SIZE) ? 1f : 0f, dt, 12f);
        offlineModeHoverAnim = animate(offlineModeHoverAnim, inside(mouseX, mouseY, modeX, modeY, modeItemW, 13f * SCALE) ? 1f : 0f, dt, 11f);
        microsoftModeHoverAnim = animate(microsoftModeHoverAnim, inside(mouseX, mouseY, modeX + modeItemW + modeGap, modeY, modeItemW, 13f * SCALE) ? 1f : 0f, dt, 11f);
        offlineModePressAnim = animate(offlineModePressAnim, 0f, dt, 8f);
        microsoftModePressAnim = animate(microsoftModePressAnim, 0f, dt, 8f);
        randomHoverAnim = animate(randomHoverAnim, inside(mouseX, mouseY, buttonX, randomY, buttonW, TOP_BUTTON_H) ? 1f : 0f, dt, 11f);
        openSiteHoverAnim = animate(openSiteHoverAnim, microsoftMode && inside(mouseX, mouseY, buttonX, openSiteY, buttonW, TOP_BUTTON_H) ? 1f : 0f, dt, 11f);
        clearHoverAnim = animate(clearHoverAnim, inside(mouseX, mouseY, buttonX, clearY, buttonW, TOP_BUTTON_H) ? 1f : 0f, dt, 11f);
        addPressAnim = animate(addPressAnim, 0f, dt, 8f);
        randomPressAnim = animate(randomPressAnim, 0f, dt, 7f);
        openSitePressAnim = animate(openSitePressAnim, 0f, dt, 7f);
        clearPressAnim = animate(clearPressAnim, 0f, dt, 7f);
    }
}
