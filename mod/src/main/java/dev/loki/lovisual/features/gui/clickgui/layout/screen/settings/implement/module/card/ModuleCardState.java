package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.other.StatusRender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ModuleCardState {
    final List<CardHit> hits = new ArrayList<>();
    final Map<String, Float> stateAnimById = new HashMap<>();
    final Map<String, Float> hoverAnimById = new HashMap<>();
    final Map<String, StatusRender> statusRenderById = new HashMap<>();

    float scroll;
    float smoothedScroll;
    boolean scrollbarVisible;
    boolean draggingScrollbar;
    float scrollbarX;
    float scrollbarY;
    float scrollbarW;
    float scrollbarH;
    float scrollbarThumbY;
    float scrollbarThumbH;
    float scrollbarMaxScroll;
    float scrollbarDragOffset;

    void resetScroll() {
        scroll = 0f;
        smoothedScroll = 0f;
        draggingScrollbar = false;
    }
}

record CardHit(String id,
               float x,
               float y,
               float w,
               float h,
               float bindX,
               float bindY,
               float bindW,
               float bindH,
               float statusX,
               float statusY,
               float statusW,
               float statusH,
               boolean hasSettings,
               boolean toggleable) {
    String getId() {
        return id;
    }

    boolean statusHit(float mx, float my) {
        if (!toggleable) return false;
        return mx >= statusX && mx <= statusX + statusW
                && my >= statusY && my <= statusY + statusH;
    }

    boolean bindHit(float mx, float my) {
        return mx >= bindX && mx <= bindX + bindW
                && my >= bindY && my <= bindY + bindH;
    }
}

record Parallax(float yawDeg, float pitchDeg, float scale, float shiftX, float shiftY, boolean active) {
    static final Parallax NONE = new Parallax(0f, 0f, 1f, 0f, 0f, false);
}

record BindRect(float x, float y, float w, float h, float hitX, float hitY, float hitW, float hitH) {
}
