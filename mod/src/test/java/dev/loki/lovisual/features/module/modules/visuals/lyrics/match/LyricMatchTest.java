package dev.loki.lovisual.features.module.modules.visuals.lyrics.match;

import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.match.LyricMatch;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.match.TrackQuery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class LyricMatchTest {
    private static final TrackQuery QUERY = new TrackQuery("Insomnia", "Nürnberg", 190);

    @Test
    void similarityIgnoresCaseAndPunctuation() {
        assertEquals(1.0, LyricMatch.similarity("Hello, World", "hello world"), 1e-9);
        assertEquals(0.85, LyricMatch.similarity("Insomnia", "Insomnia (live)"), 1e-9);
        assertEquals(0.0, LyricMatch.similarity("abc", "xyz"), 1e-9);
        assertEquals(0.0, LyricMatch.similarity("", "xyz"), 1e-9);
    }

    @Test
    void exactHitScoresAboveAcceptanceAndNearCertain() {
        double score = LyricMatch.score("Insomnia", "Nürnberg", 190, QUERY);
        assertTrue(score >= LyricMatch.CERTAIN, "score was " + score);
    }

    @Test
    void differentTitleIsRejectedOutright() {
        assertEquals(0.0, LyricMatch.score("Completely Other", "Nürnberg", 190, QUERY), 1e-9);
    }

    @Test
    void sameTitleByAnotherArtistAndLengthFallsBelowAcceptance() {
        double score = LyricMatch.score("Insomnia", "Faithless", 420, QUERY);
        assertTrue(score < LyricMatch.ACCEPT, "score was " + score);
    }

    @Test
    void durationScoreFallsOffBetweenCloseAndFar() {
        assertEquals(1.0, LyricMatch.durationScore(191, 190), 1e-9);
        assertEquals(0.0, LyricMatch.durationScore(220, 190), 1e-9);
        assertEquals(0.5, LyricMatch.durationScore(0, 190), 1e-9);
        double mid = LyricMatch.durationScore(198, 190);
        assertTrue(mid > 0.0 && mid < 1.0);
    }

    @Test
    void unknownArtistIsNeutral() {
        assertEquals(0.5, LyricMatch.artistScore("Anyone", new TrackQuery("x", "", 0)), 1e-9);
    }
}
