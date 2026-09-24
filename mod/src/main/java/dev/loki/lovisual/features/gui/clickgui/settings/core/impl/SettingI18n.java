package dev.loki.lovisual.features.gui.clickgui.settings.core;

import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Static i18n resolution and translation-key generation for {@link Setting}: language-readiness
 * gate, first-hit translation lookup, missing-key capture and the option-id variant expansion
 * used to build candidate keys. Holds only process-wide state; per-instance caches live in
 * {@link SettingLocalization}. Extracted so the setting base class stays under the file-size cap.
 */
final class SettingI18n {

    private static final ThreadLocal<Setting.MissingI18nReporter> MISSING_REPORTER = new ThreadLocal<>();
    private static boolean langReady = false;

    private SettingI18n() { }

    static boolean langReady() { return langReady; }

    /** Reset the language-readiness gate and re-run the probe; used before a preflight pass. */
    static boolean preparePreflight() {
        langReady = false;
        return ensureLangReady();
    }

    /** Install a missing-key reporter for the current thread; the returned handle restores the prior one. */
    static AutoCloseable captureMissing(Setting.MissingI18nReporter reporter) {
        Setting.MissingI18nReporter previous = MISSING_REPORTER.get();
        if (reporter == null) MISSING_REPORTER.remove();
        else MISSING_REPORTER.set(reporter);
        return () -> {
            if (previous == null) MISSING_REPORTER.remove();
            else MISSING_REPORTER.set(previous);
        };
    }

    /** Return the first key that resolves to a translation (distinct from the key), else report it as missing. */
    static String resolveFirstTranslation(List<String> keys, boolean logIfMissing) {
        if (keys == null || keys.isEmpty()) return null;
        if (!ensureLangReady()) return null;
        String firstLoggable = null;
        for (String key : keys) {
            if (key == null || key.isBlank()) continue;
            if (key.startsWith("setting.") && firstLoggable == null) firstLoggable = key;
            String translated = I18n.get(key);
            if (!translated.equals(key)) return translated;
        }
        if (logIfMissing && firstLoggable != null) {
            Setting.MissingI18nReporter reporter = MISSING_REPORTER.get();
            if (reporter != null) reporter.missing(keys);
        }
        return null;
    }

    /** Candidate option keys: {@code <base>.option.<variant>} for every base key and option-id variant. */
    static List<String> optionKeys(List<String> baseKeys, String optionId) {
        ArrayList<String> keys = new ArrayList<>();
        if (optionId == null || optionId.isBlank()) return keys;
        ArrayList<String> variants = optionIdVariants(optionId);
        for (String base : baseKeys) {
            if (base == null || base.isBlank()) continue;
            for (String variant : variants) addOptionKey(keys, base, variant);
        }
        return keys;
    }

    static void addCommonI18nKey(ArrayList<String> keys, String key) {
        if (key == null) return;
        String normalized = key.trim();
        if (!normalized.isBlank() && !keys.contains(normalized)) keys.add(normalized);
    }

    private static boolean ensureLangReady() {
        if (langReady) return true;
        if (!I18n.get("setting.module.bind").equals("setting.module.bind")) langReady = true;
        return langReady;
    }

    private static ArrayList<String> optionIdVariants(String optionId) {
        ArrayList<String> variants = new ArrayList<>();
        addVariant(variants, optionId);
        String lower = optionId.toLowerCase(Locale.ROOT);
        addVariant(variants, lower);
        addNormalizedOptionVariants(variants, lower);
        addVariant(variants, optionId.toUpperCase(Locale.ROOT));
        if (!lower.isEmpty()) addVariant(variants, Character.toUpperCase(lower.charAt(0)) + lower.substring(1));
        return variants;
    }

    private static void addNormalizedOptionVariants(ArrayList<String> variants, String lower) {
        if (lower == null || lower.isBlank()) return;
        String normalized = lower.trim().replace('-', '_').replace(' ', '_');
        while (normalized.contains("__")) normalized = normalized.replace("__", "_");
        normalized = stripEdgeUnderscores(normalized);
        addVariant(variants, normalized);
        addVariant(variants, normalized.replace("_", ""));
    }

    private static String stripEdgeUnderscores(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '_') start++;
        while (end > start && value.charAt(end - 1) == '_') end--;
        return start == 0 && end == value.length() ? value : value.substring(start, end);
    }

    private static void addVariant(ArrayList<String> variants, String value) {
        if (value != null && !value.isBlank() && !variants.contains(value)) variants.add(value);
    }

    private static void addOptionKey(ArrayList<String> keys, String base, String optionId) {
        String key = base + ".option." + optionId;
        if (!keys.contains(key)) keys.add(key);
    }
}
