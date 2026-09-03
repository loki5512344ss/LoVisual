package dev.loki.lovisual.features.module.modules.visuals.lyrics;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class LyricLineSplitterTest {

    @Test
    void returnsEmptyForNullInput() {
        assertTrue(LyricLineSplitter.splitLongLines(null).isEmpty());
    }

    @Test
    void returnsEmptyForEmptyList() {
        assertTrue(LyricLineSplitter.splitLongLines(Collections.emptyList()).isEmpty());
    }

    @Test
    void keepsShortLinesUnsplit() {
        LyricLine line = new LyricLine(1000L, "Short text");
        List<LyricLine> input = Collections.singletonList(line);
        List<LyricLine> result = LyricLineSplitter.splitLongLines(input);

        assertEquals(1, result.size());
        assertEquals(1000L, result.get(0).timestampMs());
        assertEquals("Short text", result.get(0).text());
    }

    @Test
    void splitsLongLineIntoChunks() {
        LyricLine line = new LyricLine(2000L, "This is a long line with many words in it");
        List<LyricLine> input = Collections.singletonList(line);
        List<LyricLine> result = LyricLineSplitter.splitLongLines(input);

        assertTrue(result.size() > 1, "Long line should be split into multiple chunks");
        assertEquals("This is a", result.get(0).text());
    }

    @Test
    void maintainsTimestampOrder() {
        LyricLine line = new LyricLine(3000L, "One two three four five six seven eight nine ten");
        List<LyricLine> input = Collections.singletonList(line);
        List<LyricLine> result = LyricLineSplitter.splitLongLines(input);

        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i).timestampMs() >= result.get(i - 1).timestampMs(),
                    "Timestamps should be in order");
        }
    }

    @Test
    void sortsResultByTimestamp() {
        LyricLine line1 = new LyricLine(3000L, "Word word word word word word");
        LyricLine line2 = new LyricLine(1000L, "Another line with several words here");
        List<LyricLine> input = Arrays.asList(line1, line2);
        List<LyricLine> result = LyricLineSplitter.splitLongLines(input);

        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i).timestampMs() >= result.get(i - 1).timestampMs(),
                    "Result should be sorted by timestamp");
        }
    }

    @Test
    void splitsLineIntoMultipleChunksWithDecreasingDurations() {
        LyricLine line = new LyricLine(5000L, "This line has too many words in it to be short");
        List<LyricLine> input = Collections.singletonList(line);
        List<LyricLine> result = LyricLineSplitter.splitLongLines(input);

        assertTrue(result.size() > 1, "Long line should be split");
        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i).timestampMs() > result.get(i - 1).timestampMs(),
                    "Timestamps should be strictly increasing");
        }
    }

    @Test
    void handlesExactFourWordLine() {
        LyricLine line = new LyricLine(1000L, "One two three four");
        List<LyricLine> input = Collections.singletonList(line);
        List<LyricLine> result = LyricLineSplitter.splitLongLines(input);

        assertEquals(1, result.size(), "Exactly 4 words should not be split");
    }

    @Test
    void splitsFiveWordLine() {
        LyricLine line = new LyricLine(1000L, "One two three four five");
        List<LyricLine> input = Collections.singletonList(line);
        List<LyricLine> result = LyricLineSplitter.splitLongLines(input);

        assertTrue(result.size() > 1, "5 words should be split");
    }
}