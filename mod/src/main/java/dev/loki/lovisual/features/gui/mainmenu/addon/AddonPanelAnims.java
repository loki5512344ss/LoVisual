package dev.loki.lovisual.features.gui.mainmenu;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;

import static dev.loki.lovisual.features.gui.mainmenu.AddonUiMetrics.BUTTON_H;
import static dev.loki.lovisual.features.gui.mainmenu.AddonUiMetrics.GAP;
import static dev.loki.lovisual.features.gui.mainmenu.AddonUiMetrics.LEFT_W;
import static dev.loki.lovisual.features.gui.mainmenu.AddonUiMetrics.SCALE;

final class AddonPanelAnims {
    private float toggleHover;
    private float togglePress;
    private float scanHover;
    private float scanPress;
    private float modsHover;
    private float modsPress;
    private float configHover;
    private float configPress;
    private float backHover;
    private float backPress;

    float toggleHover() { return toggleHover; }
    float togglePress() { return togglePress; }
    float scanHover() { return scanHover; }
    float scanPress() { return scanPress; }
    float modsHover() { return modsHover; }
    float modsPress() { return modsPress; }
    float configHover() { return configHover; }
    float configPress() { return configPress; }
    float backHover() { return backHover; }
    float backPress() { return backPress; }

    void pressToggle() { togglePress = 1f; }
    void pressScan() { scanPress = 1f; }
    void pressMods() { modsPress = 1f; }
    void pressConfig() { configPress = 1f; }
    void pressBack() { backPress = 1f; }

    void updateHover(float mouseX, float mouseY, AddonUiMetrics.Layout layout) {
        float dt = AnimationUtility.deltaTime();
        float x = layout.leftX() + 5f * SCALE;
        float y = layout.bottomY() + 6f * SCALE;
        float w = LEFT_W - 10f * SCALE;
        float half = (w - GAP) * 0.5f;
        toggleHover = AddonUiMetrics.animate(toggleHover, AddonUiMetrics.inside(mouseX, mouseY, x, y, half, BUTTON_H) ? 1f : 0f, dt, 11f);
        scanHover = AddonUiMetrics.animate(scanHover, AddonUiMetrics.inside(mouseX, mouseY, x + half + GAP, y, half, BUTTON_H) ? 1f : 0f, dt, 11f);
        modsHover = AddonUiMetrics.animate(modsHover, AddonUiMetrics.inside(mouseX, mouseY, x, y + 17f * SCALE, half, BUTTON_H) ? 1f : 0f, dt, 11f);
        configHover = AddonUiMetrics.animate(configHover, AddonUiMetrics.inside(mouseX, mouseY, x + half + GAP, y + 17f * SCALE, half, BUTTON_H) ? 1f : 0f, dt, 11f);
        backHover = AddonUiMetrics.animate(backHover, AddonUiMetrics.inside(mouseX, mouseY, x, y + 34f * SCALE, w, BUTTON_H) ? 1f : 0f, dt, 11f);
        togglePress = AddonUiMetrics.animate(togglePress, 0f, dt, 8f);
        scanPress = AddonUiMetrics.animate(scanPress, 0f, dt, 8f);
        modsPress = AddonUiMetrics.animate(modsPress, 0f, dt, 8f);
        configPress = AddonUiMetrics.animate(configPress, 0f, dt, 8f);
        backPress = AddonUiMetrics.animate(backPress, 0f, dt, 8f);
    }
}
