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

    void sample(UUID id, int side, double x, double y, double z, long now, long lifeMs) {
        TrackBuffer[] pair = tracks.computeIfAbsent(id, ignored -> new TrackBuffer[2]);
        if (pair[side] == null) pair[side] = new TrackBuffer();
        pair[side].add(x, y, z, now, lifeMs);
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
