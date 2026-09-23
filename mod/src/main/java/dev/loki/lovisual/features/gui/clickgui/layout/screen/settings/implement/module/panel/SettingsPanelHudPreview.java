package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module.panel.SettingsPanelHudPreviewImpl;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;

/**
 * Режим превью HUD в панели настроек: ALL_ENABLED / ONLY_CURRENT,
 * анимация активного сегмента пилюли и применение режима к реестру
 * перетаскиваемых элементов.
 */
final class SettingsPanelHudPreview {

    enum Mode {
        ALL_ENABLED,
        ONLY_CURRENT
    }

    private Mode mode = Mode.ONLY_CURRENT;
    private float activeAnim = 1f;

    Mode mode() {
        return mode;
    }

    boolean isOnlyCurrent() {
        return mode == Mode.ONLY_CURRENT;
    }

    float activeAnim() {
        return activeAnim;
    }

    void tick(float dt) {
        float pillTarget = mode == Mode.ALL_ENABLED ? 0f : 1f;
        activeAnim = AnimationUtility.approach(activeAnim, pillTarget, dt, 14f);
        activeAnim = AnimationUtility.snap(activeAnim, pillTarget, 0.002f);
    }

    void setModeByPointer(String targetId, float pillX, float pillW, float mx) {
        float half = pillW * 0.5f;
        mode = (mx < pillX + half) ? Mode.ALL_ENABLED : Mode.ONLY_CURRENT;
        apply(targetId);
    }

    void resetToOnlyCurrent(String targetId) {
        mode = Mode.ONLY_CURRENT;
        activeAnim = 1f;
        apply(targetId);
    }

    void apply(String targetId) {
        DraggableHudElement target = DraggableHudElementRegistry.getById(targetId);
        if (mode == Mode.ONLY_CURRENT) {
            if (target != null) {
                target.setPreviewEnabled(true);
            }
            DraggableHudElementRegistry.setEditorWidgetId(targetId);
            DraggableHudElementRegistry.setForceVisible(true);
        } else {
            if (target != null) {
                target.setPreviewEnabled(false);
            }
            DraggableHudElementRegistry.setEditorWidgetId(null);
            DraggableHudElementRegistry.setForceVisible(true);
        }
    }

    static void reset(String targetId) {
        DraggableHudElement target = DraggableHudElementRegistry.getById(targetId);
        if (target != null) {
            target.setPreviewEnabled(false);
        }
        DraggableHudElementRegistry.setEditorWidgetId(null);
        DraggableHudElementRegistry.setForceVisible(false);
    }
}
