package dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.match;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Pure scoring of a search hit against the track that is playing (0 = unrelated, 1 = certain). */
public final class LyricMatch {
    /** Lowest score still trusted as "the same song". */
    public static final double ACCEPT = 0.6;
    /** A hit this good ends the search early. */
    public static final double CERTAIN = 0.92;

    private static final double TITLE_FLOOR = 0.5;
    private static final long CLOSE_SECONDS = 2;
    private static final long FAR_SECONDS = 15;

    private LyricMatch() {
    }

    public static double score(String hitTitle, String hitArtist, long hitDurationSeconds, TrackQuery query) {
        double title = similarity(hitTitle, query.cleanTitle());
        if (title < TITLE_FLOOR) return 0.0;
        double artist = artistScore(hitArtist, query);
        double duration = durationScore(hitDurationSeconds, query.durationSeconds());
        return 0.55 * title + 0.30 * artist + 0.15 * duration;
    }

    /** Token similarity: 1 for equal text, high when one contains the other, Jaccard otherwise. */
    public static double similarity(String a, String b) {
        Set<String> left = tokens(a);
        Set<String> right = tokens(b);
        if (left.isEmpty() || right.isEmpty()) return 0.0;
        if (left.equals(right)) return 1.0;
        if (left.containsAll(right) || right.containsAll(left)) return 0.85;
        Set<String> common = new HashSet<>(left);
        common.retainAll(right);
        Set<String> all = new HashSet<>(left);
        all.addAll(right);
        return (double) common.size() / all.size();
    }

    public static double artistScore(String hitArtist, TrackQuery query) {
        if (query.artist().isEmpty()) return 0.5;
        return Math.max(similarity(hitArtist, query.cleanArtist()), similarity(hitArtist, query.primaryArtist()));
    }

    public static double durationScore(long hitSeconds, long wantedSeconds) {
        if (hitSeconds <= 0 || wantedSeconds <= 0) return 0.5;
        long diff = Math.abs(hitSeconds - wantedSeconds);
        if (diff <= CLOSE_SECONDS) return 1.0;
        if (diff >= FAR_SECONDS) return 0.0;
        return 1.0 - (double) (diff - CLOSE_SECONDS) / (FAR_SECONDS - CLOSE_SECONDS);
    }

    private static Set<String> tokens(String text) {
        if (text == null) return Set.of();
        String flat = text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
        if (flat.isEmpty()) return Set.of();
        return new HashSet<>(Arrays.asList(flat.split(" ")));
    }
}
