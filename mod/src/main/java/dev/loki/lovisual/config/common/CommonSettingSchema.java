package dev.loki.lovisual.config.common;

import java.util.List;

public interface CommonSettingSchema {
    String commonI18nKey();

    default List<String> commonI18nKeys() {
        String key = commonI18nKey();
        return key == null || key.isBlank() ? List.of() : List.of(key);
    }
}
