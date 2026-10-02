package dev.loki.lovisual.features.module.modules.visuals.lyrics.match;

import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.match.TrackQuery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class TrackQueryTest {

    @Test
    void cleanRemovesBracketsAndTrims() {
        assertEquals("Song Title", TrackQuery.clean("Song Title (feat. Guest)"));
        assertEquals("Song Title", TrackQuery.clean("Song Title [Deluxe]"));
        assertEquals("Song Title", TrackQuery.clean("  Song Title  "));
        assertEquals("", TrackQuery.clean("(Song)"));
        assertEquals("", TrackQuery.clean(null));
    }

    @Test
    void cleanTitleFallsBackToRawWhenEverythingWasDecoration() {
        assertEquals("(Intro)", new TrackQuery("(Intro)", "A", 0).cleanTitle());
    }

    @Test
    void primaryArtistIsTheFirstCredit() {
        assertEquals("Nürnberg", new TrackQuery("x", "Nürnberg", 0).primaryArtist());
        assertEquals("A", new TrackQuery("x", "A, B & C", 0).primaryArtist());
        assertEquals("A", new TrackQuery("x", "A feat. B", 0).primaryArtist());
        assertEquals("", new TrackQuery("x", null, 0).primaryArtist());
    }

    @Test
    void searchTextJoinsCleanedTitleAndArtist() {
        assertEquals("Insomnia Nürnberg", new TrackQuery("Insomnia (Remix)", "Nürnberg", 190).searchText());
        assertEquals("Insomnia", new TrackQuery("Insomnia", "", 190).searchText());
    }
}
