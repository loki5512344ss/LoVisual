package dev.loki.lovisual.features.module.modules.visuals.lyrics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LrcParser;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LyricLine;

public final class LrcParserTest {

    @Test
    void parsesStandardLrcFormat() {
        String lrc = "[00:12.00]Hello world\n[00:15.00]Second line";
        List<LyricLine> lines = LrcParser.parse(lrc);

        assertEquals(2, lines.size());
        assertEquals(12000L, lines.get(0).timestampMs());
        assertEquals("Hello world", lines.get(0).text());
        assertEquals(15000L, lines.get(1).timestampMs());
        assertEquals("Second line", lines.get(1).text());
    }

    @Test
    void parsesSingleDigitMinutes() {
        String lrc = "[1:05.20]Test";
        List<LyricLine> lines = LrcParser.parse(lrc);

        assertEquals(1, lines.size());
        assertEquals(65200L, lines.get(0).timestampMs());
    }

    @Test
    void parsesWithoutFractionalSeconds() {
        String lrc = "[01:30]No fraction";
        List<LyricLine> lines = LrcParser.parse(lrc);

        assertEquals(1, lines.size());
        assertEquals(90000L, lines.get(0).timestampMs());
        assertEquals("No fraction", lines.get(0).text());
    }

    @Test
    void parsesOneDigitCentisecond() {
        String lrc = "[00:05.1]Test";
        List<LyricLine> lines = LrcParser.parse(lrc);

        assertEquals(1, lines.size());
        assertEquals(5100L, lines.get(0).timestampMs());
    }

    @Test
    void parsesTwoDigitCentisecond() {
        String lrc = "[00:05.12]Test";
        List<LyricLine> lines = LrcParser.parse(lrc);

        assertEquals(1, lines.size());
        assertEquals(5120L, lines.get(0).timestampMs());
    }

    @Test
    void parsesThreeDigitCentisecond() {
        String lrc = "[00:05.123]Test";
        List<LyricLine> lines = LrcParser.parse(lrc);

        assertEquals(1, lines.size());
        assertEquals(5123L, lines.get(0).timestampMs());
    }

    @Test
    void ignoresLinesWithoutTimestamp() {
        String lrc = "Just a plain line\n[00:01.00]Has timestamp";
        List<LyricLine> lines = LrcParser.parse(lrc);

        assertEquals(1, lines.size());
        assertEquals("Has timestamp", lines.get(0).text());
    }

    @Test
    void ignoresEmptyLines() {
        String lrc = "\n\n[00:01.00]Line\n\n";
        List<LyricLine> lines = LrcParser.parse(lrc);

        assertEquals(1, lines.size());
    }

    @Test
    void returnsEmptyForNullInput() {
        assertTrue(LrcParser.parse(null).isEmpty());
    }

    @Test
    void returnsEmptyForBlankInput() {
        assertTrue(LrcParser.parse("").isEmpty());
        assertTrue(LrcParser.parse("   ").isEmpty());
    }

    @Test
    void returnsEmptyWhenNoSyncedLyrics() {
        String lrc = "Some text without timestamp format";
        List<LyricLine> lines = LrcParser.parse(lrc);

        assertTrue(lines.isEmpty());
    }

    @Test
    void sortsLinesByTimestamp() {
        String lrc = "[00:30.00]Third\n[00:05.00]First\n[00:15.00]Second";
        List<LyricLine> lines = LrcParser.parse(lrc);

        assertEquals(3, lines.size());
        assertEquals("First", lines.get(0).text());
        assertEquals("Second", lines.get(1).text());
        assertEquals("Third", lines.get(2).text());
    }

    @Test
    void stripsWhitespaceFromText() {
        String lrc = "[00:01.00]   Trim me   ";
        List<LyricLine> lines = LrcParser.parse(lrc);

        assertEquals(1, lines.size());
        assertEquals("Trim me", lines.get(0).text());
    }

    @Test
    void activeLineIndexReturnsMinusOneBeforeFirstLine() {
        List<LyricLine> lines = LrcParser.parse("[00:10.00]First\n[00:20.00]Second");
        assertEquals(-1, LrcParser.activeLineIndex(lines, 5_000L));
    }

    @Test
    void activeLineIndexReturnsMinusOneForEmptyOrNullList() {
        assertEquals(-1, LrcParser.activeLineIndex(List.of(), 1_000L));
        assertEquals(-1, LrcParser.activeLineIndex(null, 1_000L));
    }

    @Test
    void activeLineIndexTracksCurrentLineAsTimeAdvances() {
        List<LyricLine> lines = LrcParser.parse("[00:10.00]First\n[00:20.00]Second\n[00:30.00]Third");

        assertEquals(0, LrcParser.activeLineIndex(lines, 10_000L));
        assertEquals(0, LrcParser.activeLineIndex(lines, 19_999L));
        assertEquals(1, LrcParser.activeLineIndex(lines, 20_000L));
        assertEquals(2, LrcParser.activeLineIndex(lines, 999_999L));
    }

    @Test
    void handlesMultipleLinesWithMixedFormats() {
        String lrc = "[0:15.5]Short\n[01:30]No fraction\n[00:05.12]Two digits\n[00:05.123]Three digits";
        List<LyricLine> lines = LrcParser.parse(lrc);

        assertEquals(4, lines.size());
        assertEquals(5120L, lines.get(0).timestampMs());
        assertEquals(5123L, lines.get(1).timestampMs());
        assertEquals(15500L, lines.get(2).timestampMs());
        assertEquals(90000L, lines.get(3).timestampMs());
    }
}
