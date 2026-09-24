package dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland;

import dev.loki.lovisual.util.media.session.MediaSessionService;
import dev.loki.lovisual.util.pvp.state.PvpState;
import net.minecraft.util.Util;

/**
 * Состояние Dynamic Island: режим, снапшот медиа, расширенность, скролл заголовка.
 */
final class IslandState {

    IslandMode currentMode = IslandMode.TIME;
    MediaSessionService.Snapshot currentSnapshot =
            MediaSessionService.Snapshot.empty(MediaSessionService.isMediaAvailable());
    boolean expanded;
    boolean clickGuiShellVisible;
    String titleScrollKey = "";
    long titleScrollStartMs;

    IslandMode resolveMode(MediaSessionService.Snapshot snapshot) {
        if (snapshot != null && snapshot.hasSession()) {
            return IslandMode.MUSIC;
        }
        if (PvpState.isActive()) {
            return IslandMode.PVP;
        }
        return IslandMode.TIME;
    }

    float predictedPvpSeconds() {
        Float secondsLeft = PvpState.getSecondsLeft();
        if (!PvpState.isActive() || secondsLeft == null) {
            return 0f;
        }
        long sinceUpdate = Math.max(0L, Util.getMillis() - PvpState.getLastUpdateMs());
        return Math.max(0f, secondsLeft - sinceUpdate / 1000f);
    }

    float titleScrollSeconds(String title, String artist, long durationSeconds) {
        String key = (title != null ? title : "") + "\u0000" + (artist != null ? artist : "");
        long now = Util.getMillis();
        if (!key.equals(titleScrollKey)) {
            titleScrollKey = key;
            titleScrollStartMs = now;
            return 0.0f;
        }
        return Math.max(0.0f, (now - titleScrollStartMs) / 1000.0f);
    }

    void resetScroll() {
        titleScrollKey = "";
        titleScrollStartMs = Util.getMillis();
    }

    enum IslandMode {
        TIME,
        MUSIC,
        PVP,
        CLICKGUI
    }

    record IslandMetrics(float pvpWidth, float musicCompactWidth) {
    }
}
