package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module;

import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRenderSurface;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;

import java.util.ArrayList;
import java.util.List;

final class SettingsPanelState {
    final List<Setting> settings = new ArrayList<>();
    final List<SettingRow> rows = new ArrayList<>();
    final List<SettingHit> hits = new ArrayList<>();
    final SettingRenderSurface renderSurface;
    String targetId = "";
    String title = "Settings";
    boolean hudContext = false;
    boolean editorContext = false;
    boolean openTarget = false;
    float openAnim = 0f;
    float panelHeight = 120f;
    float scroll = 0f;
    float smoothedScroll = 0f;
    float closeHoverAnim = 0f;
    boolean dragging = false;
    float dragOffsetX = 0f;
    float dragOffsetY = 0f;
    boolean manualPos = false;
    float manualX = 0f;
    float manualY = 0f;
    boolean limitedHeight = false;
    float hudExtraTop = 0f;
    float panelX;
    float panelY;
    float panelW;
    float panelH;
    float closeX;
    float closeY;
    float closeW;
    float closeH;
    float contentX;
    float contentY;
    float contentW;
    float contentH;
    float maxScroll;
    float contentPadLeft;
    float contentPadRight;
    float pillX;
    float pillY;
    float pillW;
    float pillH;
    float pillActiveAnim = 1f;
    HudPreviewMode hudPreviewMode = HudPreviewMode.ONLY_CURRENT;
    float lastSettingScale = 1.0f;

    SettingsPanelState(SettingRenderSurface surface) {
        this.renderSurface = surface == null ? SettingRenderSurface.SETTINGS : surface;
    }

    void open(String id, String title, List<Setting> source, boolean hudCtx) {
        if (id == null) return;
        resetHudPreview();
        this.targetId = id;
        this.title = title == null || title.isBlank() ? id : title;
        this.settings.clear();
        if (source != null) this.settings.addAll(source);
        this.hudContext = hudCtx;
        this.editorContext = false;
        this.openTarget = true;
        this.scroll = 0f;
        this.smoothedScroll = 0f;
        this.closeHoverAnim = 0f;
        this.dragging = false;
        this.rows.clear();
        this.hits.clear();
        if (hudCtx) {
            this.manualPos = false;
            this.hudPreviewMode = HudPreviewMode.ONLY_CURRENT;
            this.pillActiveAnim = 1f;
            applyHudPreviewMode();
        }
    }

    void openEditor(String id, String title, List<Setting> source) {
        open(id, title, source, false);
        this.editorContext = true;
        this.manualPos = false;
    }

    void close() {
        openTarget = false;
        dragging = false;
    }

    boolean isActive() {
        return openTarget || openAnim > 0.001f;
    }

    boolean shouldMaskMenu() {
        return (hudContext || editorContext) && isActive();
    }

    boolean blocksMenuInput() {
        return (hudContext || editorContext) && (openTarget || openAnim > 0.35f);
    }

    void applyHudPreviewMode() {
        if (!hudContext) return;
        DraggableHudElement target = DraggableHudElementRegistry.getById(targetId);
        if (hudPreviewMode == HudPreviewMode.ONLY_CURRENT) {
            if (target != null) target.setPreviewEnabled(true);
            DraggableHudElementRegistry.setEditorWidgetId(targetId);
            DraggableHudElementRegistry.setForceVisible(true);
        } else {
            if (target != null) target.setPreviewEnabled(false);
            DraggableHudElementRegistry.setEditorWidgetId(null);
            DraggableHudElementRegistry.setForceVisible(true);
        }
    }

    void resetHudPreview() {
        DraggableHudElement target = DraggableHudElementRegistry.getById(targetId);
        if (target != null) target.setPreviewEnabled(false);
        DraggableHudElementRegistry.setEditorWidgetId(null);
        DraggableHudElementRegistry.setForceVisible(false);
    }
}
