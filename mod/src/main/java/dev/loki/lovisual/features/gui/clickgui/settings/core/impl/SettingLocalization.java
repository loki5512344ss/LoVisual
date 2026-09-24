package dev.loki.lovisual.features.gui.clickgui.settings.core;

import dev.loki.lovisual.config.i18n.ConfigLang;
import dev.loki.lovisual.config.definition.SettingOwner;
import dev.loki.lovisual.config.values.collection.ConfigValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-setting localization state: translation-key derivation, resolved display-name /
 * option-name / description caches, common-i18n-key registration and the i18n on/off flags.
 * Owned by a single {@link Setting} and backed by the static helpers in {@link SettingI18n}.
 * Extracted so the setting base class stays under the file-size cap.
 */
final class SettingLocalization {

    private final Setting owner;
    private final Map<String, String> cachedOptionDisplayNames = new HashMap<>();
    private final List<String> commonI18nKeys = new ArrayList<>();
    private SettingOwner parent;
    private boolean nameEnabled = true;
    private boolean optionsEnabled = true;
    private String cachedDisplayName;
    private String cachedTranslationKey;
    private String cachedDescription;
    private String cachedDescriptionBase;
    private SettingOwner cachedTranslationOwner;

    SettingLocalization(Setting owner) {
        this.owner = owner;
    }

    SettingOwner getParent() { return parent; }

    void setParent(SettingOwner ownerRef) {
        if (parent != ownerRef) {
            parent = ownerRef;
            invalidateNameCaches();
            return;
        }
        parent = ownerRef;
    }

    /** {@code <parentPrefix>.<translationId>} or null when either part is missing. */
    String getTranslationKey() {
        if (parent == null) return null;
        if (cachedTranslationKey != null && cachedTranslationOwner == parent) return cachedTranslationKey;
        String prefix = parent.getTranslationKeyPrefix();
        String id = owner.getTranslationId();
        if (prefix == null || id == null || prefix.isBlank() || id.isBlank()) return null;
        cachedTranslationOwner = parent;
        cachedTranslationKey = prefix + "." + id;
        return cachedTranslationKey;
    }

    /** Ordered candidate keys: local translation key, then registered common keys, then id-based. */
    List<String> getTranslationKeys() {
        List<String> keys = new ArrayList<>(3);
        String local = getTranslationKey();
        if (local != null && !local.isBlank()) keys.add(local);
        for (String commonI18nKey : commonI18nKeys) {
            if (commonI18nKey != null && !commonI18nKey.isBlank()) keys.add("setting.common." + commonI18nKey);
        }
        String id = owner.getTranslationId();
        if (id != null && !id.isBlank()) keys.add("setting.common." + id);
        return keys;
    }

    String getDisplayName() {
        boolean ready = SettingI18n.langReady();
        if (ready && cachedDisplayName != null) return cachedDisplayName;
        if (nameEnabled) {
            String translated = SettingI18n.resolveFirstTranslation(getTranslationKeys(), true);
            if (translated != null) {
                if (ready) cachedDisplayName = translated;
                return translated;
            }
        }
        ConfigValue<?> value = owner.getConfigValue();
        if (value != null) {
            String fallback = ConfigLang.getName(value);
            if (fallback != null && !fallback.isBlank() && !fallback.equals(value.getName())) {
                if (ready) cachedDisplayName = fallback;
                return fallback;
            }
        }
        if (ready) cachedDisplayName = owner.getName();
        return owner.getName();
    }

    String getOptionDisplayName(String optionId) {
        if (optionId == null || optionId.isBlank()) return optionId;
        if (!optionsEnabled) return optionId;
        boolean ready = SettingI18n.langReady();
        if (ready) {
            String cached = cachedOptionDisplayNames.get(optionId);
            if (cached != null) return cached;
        }
        String translated = SettingI18n.resolveFirstTranslation(SettingI18n.optionKeys(getTranslationKeys(), optionId), true);
        String resolved = translated != null ? translated : optionId;
        if (ready) cachedOptionDisplayNames.put(optionId, resolved);
        return resolved;
    }

    /** Hover description from {@code <translationKey>.desc}; null when no key exists. */
    String getDescription() {
        boolean ready = SettingI18n.langReady();
        String local = getTranslationKey();
        if (local == null || local.isBlank()) return null;
        if (ready && cachedDescription != null && local.equals(cachedDescriptionBase)) return cachedDescription;
        String translated = SettingI18n.resolveFirstTranslation(List.of(local + ".desc"), false);
        if (ready && translated != null) {
            cachedDescription = translated;
            cachedDescriptionBase = local;
        }
        return translated;
    }

    void setCommonI18nKey(String key) {
        ArrayList<String> keys = new ArrayList<>();
        SettingI18n.addCommonI18nKey(keys, key);
        setCommonI18nKeys(keys);
    }

    void setCommonI18nKeys(Iterable<String> keys) {
        ArrayList<String> normalized = new ArrayList<>();
        if (keys != null) for (String key : keys) SettingI18n.addCommonI18nKey(normalized, key);
        if (!commonI18nKeys.equals(normalized)) {
            commonI18nKeys.clear();
            commonI18nKeys.addAll(normalized);
            invalidateNameCaches();
        }
    }

    void setI18nEnabled(boolean name, boolean options) {
        if (nameEnabled != name || optionsEnabled != options) {
            nameEnabled = name;
            optionsEnabled = options;
            invalidateNameCaches();
        }
    }

    void preflight() {
        if (nameEnabled) SettingI18n.resolveFirstTranslation(getTranslationKeys(), true);
    }

    void preflightOption(String optionId) {
        if (optionsEnabled && optionId != null && !optionId.isBlank()) {
            SettingI18n.resolveFirstTranslation(SettingI18n.optionKeys(getTranslationKeys(), optionId), true);
        }
    }

    private void invalidateNameCaches() {
        cachedDisplayName = null;
        cachedTranslationKey = null;
        cachedDescription = null;
        cachedDescriptionBase = null;
        cachedTranslationOwner = null;
        cachedOptionDisplayNames.clear();
    }
}
