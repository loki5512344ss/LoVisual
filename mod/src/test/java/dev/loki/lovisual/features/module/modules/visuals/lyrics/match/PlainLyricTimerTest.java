package dev.loki.lovisual.features.module.modules.visuals.lyrics.match;

import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LyricLine;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.match.PlainLyricTimer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PlainLyricTimerTest {

    @Test
    void emptyWithoutTextOrDuration() {
        assertTrue(PlainLyricTimer.estimate(null, 100).isEmpty());
        assertTrue(PlainLyricTimer.estimate("a\nb", 0).isEmpty());
        assertTrue(PlainLyricTimer.estimate("\n  \n", 100).isEmpty());
    }

    @Test
    void sectionMarkersAndBlankLinesAreDropped() {
        List<LyricLine> lines = PlainLyricTimer.estimate("[Chorus]\nfirst line here\n\nsecond line here", 200);
        assertEquals(2, lines.size());
        assertEquals("first line here", lines.get(0).text());
    }

    @Test
    void timelineStartsAfterTheIntroAndStaysInsideTheSong() {
        List<LyricLine> lines = PlainLyricTimer.estimate("one two three\nfour five six\nseven eight nine", 200);
        assertEquals(3, lines.size());
        assertEquals(12_000, lines.get(0).timestampMs());
        for (int i = 1; i < lines.size(); i++) {
            assertTrue(lines.get(i).timestampMs() > lines.get(i - 1).timestampMs());
        }
        assertTrue(lines.get(2).timestampMs() < 200_000);
    }

    @Test
    void longerLinesGetMoreTime() {
        List<LyricLine> lines = PlainLyricTimer.estimate("short line!\nthis one is a much much longer line of lyrics\nend line!!", 300);
        long first = lines.get(1).timestampMs() - lines.get(0).timestampMs();
        long second = lines.get(2).timestampMs() - lines.get(1).timestampMs();
        assertTrue(second > first);
    }
}
