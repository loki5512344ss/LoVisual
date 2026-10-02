package dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.source;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LrcParser;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LyricLine;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.match.LyricMatch;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.match.TrackQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.source.LyricHttp.encode;

/** NetEase Cloud Music: the fallback for songs lrclib does not have (it covers a lot of non-Western music). */
final class NeteaseSource {
    private static final String SEARCH = "https://music.163.com/api/cloudsearch/pc?type=1&limit=10&offset=0&s=";
    private static final String LYRIC = "https://music.163.com/api/song/lyric?os=pc&lv=-1&kv=-1&tv=-1&id=";
    private static final Pattern CREDIT = Pattern.compile("^[^:：]{1,24}\\s*[:：]\\s*.{0,60}$");
    private static final long CREDIT_WINDOW_MS = 2000;

    private NeteaseSource() {
    }

    static List<LyricLine> find(TrackQuery query, LyricHttp http) {
        String body = http.get(SEARCH + encode(query.searchText()));
        long id = body == null ? 0 : bestSongId(body, query);
        if (id == 0) return List.of();
        String lyricBody = http.get(LYRIC + id);
        return lyricBody == null ? List.of() : stripCredits(LrcParser.parse(lyric(lyricBody)));
    }

    /** Id of the best matching song in a cloudsearch response, or 0 when nothing is close enough. */
    static long bestSongId(String json, TrackQuery query) {
        try {
            JsonElement songs = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("result").get("songs");
            if (songs == null || !songs.isJsonArray()) return 0;
            long bestId = 0;
            double bestScore = 0.0;
            for (JsonElement element : songs.getAsJsonArray()) {
                JsonObject song = element.getAsJsonObject();
                double score = LyricMatch.score(song.get("name").getAsString(), artists(song.getAsJsonArray("ar")),
                        song.has("dt") ? Math.round(song.get("dt").getAsLong() / 1000.0) : 0, query);
                if (score > bestScore) {
                    bestScore = score;
                    bestId = song.get("id").getAsLong();
                }
            }
            return bestScore >= LyricMatch.ACCEPT ? bestId : 0;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** Drops the "Lyrics by / Composer :" lines NetEase puts at the very start of a song. */
    static List<LyricLine> stripCredits(List<LyricLine> lines) {
        List<LyricLine> out = new ArrayList<>(lines);
        while (!out.isEmpty() && out.get(0).timestampMs() <= CREDIT_WINDOW_MS && CREDIT.matcher(out.get(0).text()).matches()) {
            out.remove(0);
        }
        return out;
    }

    private static String lyric(String json) {
        try {
            JsonObject lrc = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("lrc");
            return lrc == null || !lrc.has("lyric") ? "" : lrc.get("lyric").getAsString();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static String artists(JsonArray array) {
        if (array == null) return "";
        StringBuilder out = new StringBuilder();
        for (JsonElement element : array) {
            JsonElement name = element.getAsJsonObject().get("name");
            if (name != null && !name.isJsonNull()) out.append(name.getAsString()).append(' ');
        }
        return out.toString().trim();
    }
}
