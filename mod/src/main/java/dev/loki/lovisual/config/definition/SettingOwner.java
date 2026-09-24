package dev.loki.lovisual.config.definition;
import dev.loki.lovisual.features.module.core.Module;

/**
 * Owner of GUI settings. Allows settings to save config and build i18n keys
 * without tying them to Module.
 */
public interface SettingOwner {
    String name();

    void saveConfig();

    default String getTranslationKeyPrefix() {
        String name = name();
        if (name == null || name.isBlank()) return null;
        return "setting." + name;
    }
}
