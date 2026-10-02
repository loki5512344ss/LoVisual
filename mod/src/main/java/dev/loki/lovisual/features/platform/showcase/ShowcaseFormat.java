package dev.loki.lovisual.features.platform.showcase;

import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.UUID;

/** Pure helpers for the showcase commands (no client or network), unit-tested. */
public final class ShowcaseFormat {

    private ShowcaseFormat() {
    }

    /** {@code 3. Title by nick (12 copies)}; {@code index} is 1-based. */
    public static String listingLine(int index, JsonObject listing) {
        String title = string(listing, "title", "?");
        String nick = "?";
        if (listing.has("author") && listing.get("author").isJsonObject()) {
            nick = string(listing.getAsJsonObject("author"), "nick", "?");
        }
        int copies = listing.has("copies_count") ? listing.get("copies_count").getAsInt() : 0;
        return index + ". " + title + " by " + nick + " (" + copies + (copies == 1 ? " copy)" : " copies)");
    }

    /** Normalizes the sort argument; {@code null} when it is neither of the two supported values. */
    public static String parseSort(String raw) {
        String sort = raw == null ? "new" : raw.toLowerCase(Locale.ROOT);
        return sort.equals("new") || sort.equals("popular") ? sort : null;
    }

    /**
     * Resolves a user reference to a listing id: either a 1-based number into
     * the last browsed page, or a full UUID. {@code null} when it is neither.
     */
    public static String resolveId(String ref, java.util.List<String> lastPageIds) {
        if (ref == null) {
            return null;
        }
        if (ref.matches("\\d{1,3}")) {
            int index = Integer.parseInt(ref);
            return index >= 1 && index <= lastPageIds.size() ? lastPageIds.get(index - 1) : null;
        }
        try {
            return UUID.fromString(ref).toString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
    }
}
