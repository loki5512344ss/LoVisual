package dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LyricLine;

public final class LrcParser {
    private static final Pattern LRC_PATTERN = Pattern.compile("\\[(\\d+):(\\d{2})(?:\\.(\\d{1,3}))?\\](.*)");

    private LrcParser() {
    }

    public static List<LyricLine> parse(String lrcContent) {
        if (lrcContent == null || lrcContent.isBlank()) {
            return Collections.emptyList();
        }

        List<LyricLine> lines = new ArrayList<>();
        String[] rawLines = lrcContent.split("\\r?\\n");

        for (String line : rawLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            Matcher matcher = LRC_PATTERN.matcher(trimmed);
            if (matcher.matches()) {
                try {
                    long minutes = Long.parseLong(matcher.group(1));
                    long seconds = Long.parseLong(matcher.group(2));
                    String fractionStr = matcher.group(3);

                    long millis = 0;
                    if (fractionStr != null && !fractionStr.isEmpty()) {
                        millis = Long.parseLong(fractionStr);
                        if (fractionStr.length() == 1) {
                            millis *= 100;
                        } else if (fractionStr.length() == 2) {
                            millis *= 10;
                        }
                    }

                    long totalTimeMs = (minutes * 60_000L) + (seconds * 1_000L) + millis;
                    String text = matcher.group(4).trim();

                    if (!text.isEmpty()) {
                        lines.add(new LyricLine(totalTimeMs, text));
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        Collections.sort(lines);
        return lines;
    }

    /**
     * Index of the lyric line active at {@code timeMs} within a list already sorted by
     * {@link LyricLine#timestampMs()} (as returned by {@link #parse}), or {@code -1} before the
     * first line / for an empty list. Plain data lookup with no playback-state dependency, so any
     * consumer (the 3D lyrics module, or in future a compact HUD display) can reuse it without
     * depending on {@code LyricPlayback}'s module-coupled state machine.
     */
    public static int activeLineIndex(List<LyricLine> lines, long timeMs) {
        if (lines == null || lines.isEmpty()) return -1;
        int lo = 0;
        int hi = lines.size() - 1;
        int result = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (lines.get(mid).timestampMs() <= timeMs) {
                result = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return result;
    }
}
