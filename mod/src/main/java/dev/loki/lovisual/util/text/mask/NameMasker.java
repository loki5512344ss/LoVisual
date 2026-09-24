package dev.loki.lovisual.util.text.mask;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import dev.loki.lovisual.util.text.mask.NameProtector;
import dev.loki.lovisual.features.module.modules.misc.protect.StreamerMode;

/**
 * Pure name/word masking shared by StreamerMode. Unlike {@link NameProtector}
 * it masks every nickname-like token deterministically (no per-name config):
 * "Notch" -> "Player", "Herobrine_777" -> "Player2".
 */
public final class NameMasker {

    private static final int TOKEN_MAX = 16;
    private static final java.util.Set<String> COMMON_WORDS = java.util.Set.of(
            "and", "или", "the", "for", "you", "your", "has", "was", "not", "все", "для"
    );

    private final Map<String, String> assigned = new LinkedHashMap<>();
    private final String replacement;
    private final List<String> extraWords;

    public NameMasker(String replacement, List<String> extraWords) {
        this.replacement = replacement == null || replacement.isBlank() ? "Player" : replacement.trim();
        this.extraWords = extraWords == null ? List.of() : List.copyOf(extraWords);
    }

    public String mask(String text) {
        if (text == null || text.isEmpty()) return text;

        String masked = replaceWords(text);
        if (!masked.equals(text)) return masked;

        return replaceTokens(masked);
    }

    private String replaceTokens(String text) {
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            int tokenEnd = nicknameTokenEnd(text, i);
            if (tokenEnd < 0) {
                out.append(text.charAt(i));
                i++;
                continue;
            }
            String token = text.substring(i, tokenEnd);
            String alias = assigned.computeIfAbsent(token, this::registerTokenAlias);
            out.append(alias);
            i = tokenEnd;
        }
        return out.toString();
    }

    private String registerTokenAlias(String token) {
        return replacement + (assigned.size() + 1);
    }

    private String replaceWords(String text) {
        String result = text;
        for (String word : extraWords) {
            if (word == null || word.isBlank()) continue;
            result = replaceAllIgnoreCase(result, word.trim(), replacement);
        }
        return result;
    }

    static String replaceAllIgnoreCase(String text, String word, String replacement) {
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        String needle = word.toLowerCase(java.util.Locale.ROOT);
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            int idx = lower.indexOf(needle, i);
            if (idx < 0) {
                out.append(text, i, text.length());
                break;
            }
            out.append(text, i, idx).append(replacement);
            i = idx + needle.length();
        }
        return out.toString();
    }

    /**
     * Nickname-like token: latin letters/digits/underscore, 3..16 chars,
     * containing at least one letter and at least one digit-free letter run
     * (skips pure numbers and common words like "and").
     */
    static int nicknameTokenEnd(String text, int start) {
        int end = start;
        while (end < text.length() && end - start < TOKEN_MAX && isTokenChar(text.charAt(end))) end++;
        if (end - start < 3) return -1;

        String token = text.substring(start, end).toLowerCase(java.util.Locale.ROOT);
        if (COMMON_WORDS.contains(token)) return -1;

        boolean hasLetter = false;
        for (int i = start; i < end; i++) {
            if (Character.isLetter(text.charAt(i))) {
                hasLetter = true;
                break;
            }
        }
        boolean wordBounded =
                (start == 0 || !isTokenChar(text.charAt(start - 1)))
                        && (end >= text.length() || !isTokenChar(text.charAt(end)));
        return hasLetter && wordBounded ? end : -1;
    }

    private static boolean isTokenChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
