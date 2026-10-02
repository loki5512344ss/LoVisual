package dev.loki.lovisual.features.module.modules.visuals.lyrics.source;

import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LyricLine;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.match.TrackQuery;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.source.LyricsFinder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class LyricSourcesTest {

    @Test
    void blankTitleFindsNothingWithoutRetry() {
        LyricsFinder.Result result = LyricsFinder.findAsync("", "Artist", 100, false).join();
        assertTrue(result.lines().isEmpty());
        assertTrue(!result.retryLater());
    }

    @Test
    void nullTitleFindsNothing() {
        List<LyricLine> lines = LyricsFinder.findAsync(null, "Artist", 100, false).join().lines();
        assertTrue(lines.isEmpty());
    }

    @Test
    void queryKeepsRawFieldsTrimmed() {
        TrackQuery query = new TrackQuery("  T ", " A ", 5);
        assertTrue(query.title().equals("T") && query.artist().equals("A"));
    }
}
