package dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.match;

import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LyricLine;

import java.util.ArrayList;
import java.util.List;

/** Gives unsynced lyrics a plausible timeline by spreading the lines over the song, weighted by length. */
public final class PlainLyricTimer {
    private static final double INTRO = 0.06;
    private static final double OUTRO = 0.94;
    private static final int MIN_WEIGHT = 8;

    private PlainLyricTimer() {
    }

    public static List<LyricLine> estimate(String plain, long durationSeconds) {
        List<LyricLine> out = new ArrayList<>();
        if (plain == null || durationSeconds <= 0) return out;

        List<String> lines = new ArrayList<>();
        for (String raw : plain.split("\\r?\\n")) {
            String line = raw.trim();
            if (!line.isEmpty() && !line.matches("\\[[^\\]]*]")) lines.add(line);
        }
        if (lines.isEmpty()) return out;

        double total = 0;
        for (String line : lines) total += Math.max(MIN_WEIGHT, line.length());
        double startMs = durationSeconds * 1000.0 * INTRO;
        double span = durationSeconds * 1000.0 * (OUTRO - INTRO);
        double cursor = 0;
        for (String line : lines) {
            out.add(new LyricLine(Math.round(startMs + span * cursor / total), line));
            cursor += Math.max(MIN_WEIGHT, line.length());
        }
        return out;
    }
}
