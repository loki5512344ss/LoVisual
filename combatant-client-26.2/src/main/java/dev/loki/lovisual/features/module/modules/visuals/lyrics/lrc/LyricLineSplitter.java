package dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LyricLine;

public final class LyricLineSplitter {
    private LyricLineSplitter() {
    }

    /**
     * Splits long lyric lines (more than 4 words) into shorter rhythmic chunks with micro-timings.
     */
    public static List<LyricLine> splitLongLines(List<LyricLine> originalLines) {
        if (originalLines == null || originalLines.isEmpty()) {
            return Collections.emptyList();
        }

        List<LyricLine> result = new ArrayList<>();

        for (int i = 0; i < originalLines.size(); i++) {
            LyricLine current = originalLines.get(i);
            long nextTimeMs = (i + 1 < originalLines.size()) ? originalLines.get(i + 1).timestampMs() : current.timestampMs() + 4000L;
            long lineDuration = Math.max(1500L, nextTimeMs - current.timestampMs());

            String text = current.text().trim();
            String[] words = text.split("\\s+");

            if (words.length <= 4) {
                result.add(current);
            } else {
                int wordsPerChunk = words.length > 7 ? 3 : 2;
                List<String> chunks = new ArrayList<>();
                StringBuilder sb = new StringBuilder();
                int count = 0;

                for (String word : words) {
                    if (!sb.isEmpty()) sb.append(" ");
                    sb.append(word);
                    count++;
                    if (count >= wordsPerChunk) {
                        chunks.add(sb.toString());
                        sb.setLength(0);
                        count = 0;
                    }
                }
                if (!sb.isEmpty()) {
                    chunks.add(sb.toString());
                }

                int numChunks = chunks.size();
                if (numChunks == 0) {
                    result.add(current);
                    continue;
                }
                long chunkDuration = lineDuration / numChunks;

                for (int c = 0; c < numChunks; c++) {
                    long chunkStartMs = current.timestampMs() + (c * chunkDuration);
                    result.add(new LyricLine(chunkStartMs, chunks.get(c)));
                }
            }
        }

        Collections.sort(result);
        return result;
    }
}
