package dev.loki.lovisual.features.module.modules.visuals.effects.elytratrails;

import dev.loki.lovisual.features.module.modules.visuals.effects.ribbon.TrackBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Истории концов крыльев: по ленте на каждое крыло каждого игрока; опустевшие ленты и игроки удаляются. */
final class WingTrailStore {

    static final int LEFT = 0;
    static final int RIGHT = 1;

    private final Map<UUID, TrackBuffer[]> tracks = new HashMap<>();

    void clear() {
        tracks.clear();
    }

    /** Минимальный шаг записи (блоки), скачок и пауза, после которых лента начинается заново (как у evo). */
    static final double MIN_STEP = 0.35;
    static final double MAX_STEP = 8.0;
    static final long GAP_MS = 300L;

    void sample(UUID id, int side, double x, double y, double z, long now, long lifeMs) {
        sample(id, side, x, y, z, now, lifeMs, 0.0f);
    }

    void sample(UUID id, int side, double x, double y, double z, long now, long lifeMs, float speed) {
        TrackBuffer[] pair = tracks.computeIfAbsent(id, ignored -> new TrackBuffer[2]);
        if (pair[side] == null) pair[side] = new TrackBuffer();
        pair[side].addContinuous(x, y, z, now, lifeMs, speed, MIN_STEP, MAX_STEP, GAP_MS);
    }

    /** Убирает устаревшие точки; опустевшие ленты и игроки без лент удаляются. */
    void prune(long now, long lifeMs) {
        tracks.values().removeIf(pair -> {
            boolean any = false;
            for (int i = 0; i < pair.length; i++) {
                if (pair[i] == null) continue;
                pair[i].trim(now, lifeMs);
                if (pair[i].isEmpty()) pair[i] = null;
                else any = true;
            }
            return !any;
        });
    }

    /** Живые ленты для отрисовки (до двух на игрока). */
    List<TrackBuffer> snapshot() {
        List<TrackBuffer> out = new ArrayList<>(tracks.size() * 2);
        for (TrackBuffer[] pair : tracks.values()) {
            for (TrackBuffer track : pair) {
                if (track != null && !track.isEmpty()) out.add(track);
            }
        }
        return out;
    }

    int trackCount() {
        return tracks.size();
    }
}
