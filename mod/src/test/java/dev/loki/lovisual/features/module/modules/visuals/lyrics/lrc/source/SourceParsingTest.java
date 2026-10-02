package dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.source;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LyricLine;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.match.TrackQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SourceParsingTest {
    private static final TrackQuery QUERY = new TrackQuery("Insomnia", "Nürnberg", 190);

    private static JsonObject hit(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    @Test
    void syncedHitOutranksAnIdenticalPlainOne() {
        double synced = LrcLibSource.scoreOf(hit("{\"trackName\":\"Insomnia\",\"artistName\":\"Nürnberg\",\"duration\":190,\"syncedLyrics\":\"[00:01.00] a\"}"), QUERY);
        double plain = LrcLibSource.scoreOf(hit("{\"trackName\":\"Insomnia\",\"artistName\":\"Nürnberg\",\"duration\":190,\"plainLyrics\":\"a\"}"), QUERY);
        assertTrue(synced > plain);
        assertTrue(plain > 0.9);
    }

    @Test
    void instrumentalOrLyriclessHitsScoreZero() {
        assertEquals(0.0, LrcLibSource.scoreOf(hit("{\"trackName\":\"Insomnia\",\"artistName\":\"Nürnberg\",\"instrumental\":true,\"plainLyrics\":\"a\"}"), QUERY));
        assertEquals(0.0, LrcLibSource.scoreOf(hit("{\"trackName\":\"Insomnia\",\"artistName\":\"Nürnberg\",\"duration\":190}"), QUERY));
        assertEquals(0.0, LrcLibSource.scoreOf(hit("{\"trackName\":\"Insomnia\",\"artistName\":\"Nürnberg\",\"syncedLyrics\":null}"), QUERY));
    }

    @Test
    void searchUrlsStartWithTheFieldSearchAndEndWithTitleOnly() {
        List<String> urls = List.copyOf(LrcLibSource.searchUrls(QUERY));
        assertTrue(urls.get(0).contains("track_name=Insomnia") && urls.get(0).contains("artist_name=N%C3%BCrnberg"));
        assertTrue(urls.get(urls.size() - 1).endsWith("search?track_name=Insomnia"));
        assertEquals(3, urls.size(), "primary artist equals the full artist, so that variant collapses");
    }

    @Test
    void neteaseSearchPicksTheClosestSong() {
        String json = "{\"result\":{\"songs\":["
                + "{\"id\":1,\"name\":\"Insomnia\",\"dt\":420000,\"ar\":[{\"name\":\"Faithless\"}]},"
                + "{\"id\":2,\"name\":\"Insomnia\",\"dt\":190500,\"ar\":[{\"name\":\"Nürnberg\"}]}]}}";
        assertEquals(2L, NeteaseSource.bestSongId(json, QUERY));
    }

    @Test
    void neteaseSearchReturnsZeroForNoMatchOrGarbage() {
        assertEquals(0L, NeteaseSource.bestSongId("{\"result\":{\"songs\":[{\"id\":9,\"name\":\"Other\",\"dt\":1,\"ar\":[]}]}}", QUERY));
        assertEquals(0L, NeteaseSource.bestSongId("not json", QUERY));
        assertEquals(0L, NeteaseSource.bestSongId("{\"result\":{}}", QUERY));
    }

    @Test
    void neteaseCreditLinesAtTheStartAreStripped() {
        List<LyricLine> lines = List.of(
                new LyricLine(0, "作词 : Someone"),
                new LyricLine(0, "作曲 : Other"),
                new LyricLine(15_000, "first real line: with a colon"),
                new LyricLine(20_000, "second"));
        List<LyricLine> out = NeteaseSource.stripCredits(lines);
        assertEquals(2, out.size());
        assertEquals("first real line: with a colon", out.get(0).text());
    }
}
