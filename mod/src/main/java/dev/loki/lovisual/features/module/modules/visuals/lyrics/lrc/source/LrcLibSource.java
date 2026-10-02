package dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.source;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LrcParser;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LyricLine;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.match.LyricMatch;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.match.PlainLyricTimer;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.match.TrackQuery;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.source.LyricHttp.encode;

/** lrclib.net: exact lookup by duration first, then several searches ranked by how well they match. */
final class LrcLibSource {
    private static final String API = "https://lrclib.net/api/";
    private static final double SYNCED_BONUS = 0.08;

    private LrcLibSource() {
    }

    static List<LyricLine> find(TrackQuery query, LyricHttp http) {
        String title = query.cleanTitle();
        if (!query.artist().isEmpty() && query.durationSeconds() > 0) {
            String body = http.get(API + "get?artist_name=" + encode(query.cleanArtist())
                    + "&track_name=" + encode(title) + "&duration=" + query.durationSeconds());
            JsonObject exact = body == null ? null : object(body);
            if (exact != null) {
                List<LyricLine> synced = timed(exact);
                if (!synced.isEmpty()) return synced;
            }
        }

        JsonObject best = null;
        double bestScore = 0.0;
        for (String url : searchUrls(query)) {
            String body = http.get(url);
            JsonArray hits = body == null ? null : array(body);
            if (hits == null) continue;
            for (JsonElement element : hits) {
                if (!element.isJsonObject()) continue;
                JsonObject hit = element.getAsJsonObject();
                double score = scoreOf(hit, query);
                if (score > bestScore) {
                    best = hit;
                    bestScore = score;
                }
            }
            if (bestScore >= LyricMatch.CERTAIN) break;
        }
        if (best == null || bestScore < LyricMatch.ACCEPT) return List.of();
        return lines(best, query);
    }

    static Set<String> searchUrls(TrackQuery query) {
        String title = encode(query.cleanTitle());
        Set<String> urls = new LinkedHashSet<>();
        if (!query.artist().isEmpty()) {
            urls.add(API + "search?track_name=" + title + "&artist_name=" + encode(query.cleanArtist()));
            urls.add(API + "search?track_name=" + title + "&artist_name=" + encode(query.primaryArtist()));
        }
        urls.add(API + "search?q=" + encode(query.searchText()));
        urls.add(API + "search?track_name=" + title);
        return urls;
    }

    /** Hit score; instrumentals and hits without any lyrics score zero, synced ones get a small bonus. */
    static double scoreOf(JsonObject hit, TrackQuery query) {
        if (flag(hit, "instrumental")) return 0.0;
        boolean synced = text(hit, "syncedLyrics") != null;
        if (!synced && text(hit, "plainLyrics") == null) return 0.0;
        double base = LyricMatch.score(text(hit, "trackName"), text(hit, "artistName"),
                Math.round(number(hit, "duration")), query);
        return base > 0.0 ? base + (synced ? SYNCED_BONUS : 0.0) : 0.0;
    }

    private static List<LyricLine> lines(JsonObject hit, TrackQuery query) {
        List<LyricLine> synced = timed(hit);
        if (!synced.isEmpty()) return synced;
        long duration = Math.round(number(hit, "duration"));
        return PlainLyricTimer.estimate(text(hit, "plainLyrics"), duration > 0 ? duration : query.durationSeconds());
    }

    private static List<LyricLine> timed(JsonObject hit) {
        String synced = text(hit, "syncedLyrics");
        return synced == null ? List.of() : LrcParser.parse(synced);
    }

    private static JsonObject object(String body) {
        try {
            JsonElement parsed = JsonParser.parseString(body);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static JsonArray array(String body) {
        try {
            JsonElement parsed = JsonParser.parseString(body);
            return parsed.isJsonArray() ? parsed.getAsJsonArray() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String text(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) return null;
        String value = element.getAsString();
        return value.isBlank() ? null : value;
    }

    private static double number(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() ? element.getAsDouble() : 0.0;
    }

    private static boolean flag(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean() && element.getAsBoolean();
    }
}
