package dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.match;

import java.util.regex.Pattern;

/** The track being looked up: raw player metadata plus the cleaned forms the lyric services want. */
public record TrackQuery(String title, String artist, long durationSeconds) {
    private static final Pattern BRACKETS = Pattern.compile("\\(.*?\\)|\\[.*?]");
    private static final Pattern ARTIST_SPLIT =
            Pattern.compile("\\s*(?:,|;|&|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bwith\\b|\\bx\\b| / )\\s*", Pattern.CASE_INSENSITIVE);

    public TrackQuery {
        title = title == null ? "" : title.trim();
        artist = artist == null ? "" : artist.trim();
    }

    /** Title without "(feat. X)", "[Remix]" and similar decoration. */
    public static String clean(String text) {
        if (text == null) return "";
        return BRACKETS.matcher(text).replaceAll("").trim();
    }

    public String cleanTitle() {
        String cleaned = clean(title);
        return cleaned.isEmpty() ? title : cleaned;
    }

    public String cleanArtist() {
        return clean(artist);
    }

    /** First credited artist ("A, B & C" gives "A"). */
    public String primaryArtist() {
        String cleaned = cleanArtist();
        String[] parts = ARTIST_SPLIT.split(cleaned, 2);
        return parts.length == 0 ? cleaned : parts[0].trim();
    }

    /** Free-text query for services that search by one string. */
    public String searchText() {
        return (cleanTitle() + " " + cleanArtist()).trim();
    }
}
