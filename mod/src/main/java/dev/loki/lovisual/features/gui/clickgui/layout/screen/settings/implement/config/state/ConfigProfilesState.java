package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.config;

import dev.loki.lovisual.config.profile.codec.ConfigProfileType;
import dev.loki.lovisual.config.profile.diff.ConfigProfileDiff;
import dev.loki.lovisual.config.profile.snapshot.ConfigProfileMeta;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.config.ConfigProfileCardComponent.CardHit;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;

import java.util.ArrayList;
import java.util.List;

final class ConfigProfilesState {
    private static final long STATUS_DURATION_MS = 2400L;

    private ConfigProfileType type = ConfigProfileType.MODULES;
    private ConfigProfileMeta selected;
    private ConfigProfileDiff selectedDiff;
    private String diffError;
    private String statusMessage;
    private long statusUntilMs;

    private ConfigProfileType editingType;
    private String editingId;
    private String editingText = "";
    private Rect editingTitleRect = Rect.ZERO;

    private float scroll;
    private float smoothedScroll;
    private boolean scrollbarVisible;
    private boolean draggingScrollbar;
    private float scrollbarX;
    private float scrollbarY;
    private float scrollbarW;
    private float scrollbarH;
    private float scrollbarThumbY;
    private float scrollbarThumbH;
    private float scrollbarMaxScroll;

    private float scrollbarDragOffset;
    private final List<CardEntryHit> hits = new ArrayList<>();

    ConfigProfileType type() { return type; }
    void type(ConfigProfileType v) { type = v; }

    ConfigProfileMeta selected() { return selected; }
    void selected(ConfigProfileMeta v) { selected = v; }

    ConfigProfileDiff selectedDiff() { return selectedDiff; }
    void selectedDiff(ConfigProfileDiff v) { selectedDiff = v; }

    String diffError() { return diffError; }
    void diffError(String v) { diffError = v; }

    String statusMessage() { return statusMessage; }
    long statusUntilMs() { return statusUntilMs; }

    ConfigProfileType editingType() { return editingType; }
    String editingId() { return editingId; }
    String editingText() { return editingText; }
    void editingText(String v) { editingText = v; }
    Rect editingTitleRect() { return editingTitleRect; }
    void editingTitleRect(Rect v) { editingTitleRect = v; }

    float scroll() { return scroll; }
    void scroll(float v) { scroll = v; }
    float smoothedScroll() { return smoothedScroll; }
    void smoothedScroll(float v) { smoothedScroll = v; }

    boolean scrollbarVisible() { return scrollbarVisible; }
    void scrollbarVisible(boolean v) { scrollbarVisible = v; }
    boolean draggingScrollbar() { return draggingScrollbar; }
    void draggingScrollbar(boolean v) { draggingScrollbar = v; }
    float scrollbarX() { return scrollbarX; }
    void scrollbarX(float v) { scrollbarX = v; }
    float scrollbarY() { return scrollbarY; }
    void scrollbarY(float v) { scrollbarY = v; }
    float scrollbarW() { return scrollbarW; }
    float scrollbarW(float v) { scrollbarW = v; return v; }
    float scrollbarH() { return scrollbarH; }
    void scrollbarH(float v) { scrollbarH = v; }
    float scrollbarThumbY() { return scrollbarThumbY; }
    void scrollbarThumbY(float v) { scrollbarThumbY = v; }
    float scrollbarThumbH() { return scrollbarThumbH; }
    void scrollbarThumbH(float v) { scrollbarThumbH = v; }
    float scrollbarMaxScroll() { return scrollbarMaxScroll; }
    void scrollbarMaxScroll(float v) { scrollbarMaxScroll = v; }
    float scrollbarDragOffset() { return scrollbarDragOffset; }
    void scrollbarDragOffset(float v) { scrollbarDragOffset = v; }

    List<CardEntryHit> hits() { return hits; }

    void resetScroll() {
        scroll = 0f;
        smoothedScroll = 0f;
        draggingScrollbar = false;
    }

    void setStatus(String message) {
        statusMessage = message;
        statusUntilMs = System.currentTimeMillis() + STATUS_DURATION_MS;
    }

    void clearStatus() {
        statusMessage = null;
        statusUntilMs = 0L;
    }

    boolean isStatusVisible() {
        if (statusMessage == null || statusMessage.isBlank()) return false;
        if (System.currentTimeMillis() > statusUntilMs) {
            clearStatus();
            return false;
        }
        return true;
    }

    boolean isRenaming() {
        return editingType != null && editingId != null;
    }

    boolean isEditing(ConfigProfileMeta meta) {
        return meta != null && editingType == meta.type() && editingId != null && editingId.equals(meta.getId());
    }

    void beginRename(ConfigProfileMeta meta, float x, float y, float w, float h) {
        if (meta == null) return;
        editingType = meta.type();
        editingId = meta.getId();
        editingText = meta.name();
        editingTitleRect = new Rect(x, y, w, h);
        clearStatus();
    }

    void cancelRename() {
        editingType = null;
        editingId = null;
        editingText = "";
        editingTitleRect = Rect.ZERO;
    }

    record CardEntryHit(ConfigProfileMeta meta, CardHit hit) {}

    record Rect(float x, float y, float w, float h) {
        static final Rect ZERO = new Rect(0f, 0f, 0f, 0f);
        boolean contains(float mx, float my) {
            return ClickGuiMath.insideRect(mx, my, x, y, w, h);
        }
    }
}
