package dev.loki.lovisual.features.module.modules.visuals.lyrics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LrcLibClient;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LyricLine;

public final class LrcLibClientTest {

    @Test
    void cleanTrackNameRemovesParenthetical() {
        assertEquals("Song Title", LrcLibClient.cleanTrackName("Song Title (feat. Guest)"));
        assertEquals("Song Title", LrcLibClient.cleanTrackName("Song Title (Remix)"));
        assertEquals("", LrcLibClient.cleanTrackName("(Song)"));
    }

    @Test
    void cleanTrackNameRemovesBrackets() {
        assertEquals("Song Title", LrcLibClient.cleanTrackName("Song Title [Deluxe]"));
        assertEquals("", LrcLibClient.cleanTrackName("[Song]"));
    }

    @Test
    void cleanTrackNameHandlesNull() {
        assertEquals("", LrcLibClient.cleanTrackName(null));
    }

    @Test
    void cleanTrackNameHandlesEmpty() {
        assertEquals("", LrcLibClient.cleanTrackName(""));
    }

    @Test
    void cleanTrackNameHandlesWhitespaceOnly() {
        assertEquals("", LrcLibClient.cleanTrackName("   "));
    }

    @Test
    void cleanTrackNameTrims() {
        assertEquals("Song Title", LrcLibClient.cleanTrackName("  Song Title  "));
    }

    @Test
    void fetchLyricsAsyncReturnsEmptyForBlankTrack() {
        var future = LrcLibClient.fetchLyricsAsync("", "Artist", false);
        List<LyricLine> result = future.join();
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchLyricsAsyncReturnsEmptyForNullTrack() {
        var future = LrcLibClient.fetchLyricsAsync(null, "Artist", false);
        List<LyricLine> result = future.join();
        assertTrue(result.isEmpty());
    }
}
